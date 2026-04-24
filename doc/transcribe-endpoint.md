# Endpoint `/transcribe` (Coulson Assistant)

Este documento descreve o **fluxo de código** do endpoint de transcrição, o que ele faz, e quais **integrações** e **persistências** participam do processo.

## Onde o endpoint está

- **Rota HTTP**: `POST /api/v1/videos/transcribe`
- **Controller**: `src/main/java/com/yellowleaf/coulson/interfaces/rest/VideoController.java`
- **Use case (orquestração)**: `src/main/java/com/yellowleaf/coulson/application/usecase/ProcessVideoUseCase.java`

## Contrato de entrada e saída

### Request

- **Content-Type**: `multipart/form-data`
- **Campo**: `file` (`MultipartFile`)

### Response `200`

Retorna um JSON com:

- **videoId**: ID gerado para o vídeo processado
- **transcription**: texto completo concatenado
- **totalChunks**: quantidade de chunks de vídeo gerados (quando o vídeo é dividido)
- **segments**: lista com `{ speaker, start, end, text }`

Observação: no código o `videoId` é reaproveitado como `meetingId` na persistência.

## Visão geral do fluxo (alto nível)

1. Controller recebe o upload e chama o caso de uso.
2. O vídeo é salvo localmente em diretório temporário.
3. Se necessário, o vídeo é dividido em chunks de até 200 MB (FFmpeg, stream copy).
4. Para cada chunk de vídeo, o áudio é extraído em paralelo (FFmpeg).
5. Os áudios extraídos são concatenados em um único MP3 (FFmpeg concat).
6. O MP3 final é dividido em chunks por duração (ex.: 400s) para transcrição.
7. Cada chunk de áudio é transcrito via webhook HTTP (tipicamente n8n).
8. Para cada chunk transcrito, é feito um segundo webhook para resolver nomes de locutores.
9. Texto e segmentos finais (com offsets corrigidos) são agregados.
10. A reunião e seus segmentos são persistidos no PostgreSQL.
11. Um evento de domínio é publicado; se embeddings estiverem habilitados, inicia-se um processamento assíncrono para preencher `embedding` nos segmentos.
12. Arquivos temporários (vídeo/chunks/áudios/chunks de áudio) são removidos ao final.

## Fluxo detalhado por camada

## 1) Camada REST (`VideoController`)

Arquivo: `src/main/java/com/yellowleaf/coulson/interfaces/rest/VideoController.java`

- Recebe `file` via `@RequestParam("file")`.
- Valida `file.isEmpty()` e retorna `400` se vazio.
- Cria `ProcessVideoCommand` e chama `processVideoUseCase.execute(command)`.
- Converte o resultado (`TranscriptionResult`) em `TranscriptionResponse` (DTO) para o retorno.

Ponto importante:

- O controller está mapeado em `@RequestMapping("/api/v1/videos")`, então o caminho final é `/api/v1/videos/transcribe`.

## 2) Caso de uso (`ProcessVideoUseCase`)

Arquivo: `src/main/java/com/yellowleaf/coulson/application/usecase/ProcessVideoUseCase.java`

Este é o **núcleo do fluxo**. Ele orquestra processamento de mídia, integração com webhooks e persistência.

### 2.1) Armazenamento inicial do upload

- Chama `storagePort.store(command.getFile())`.
- Implementação usada no `AppConfig`: `VideoStoragePort -> LocalVideoStorageAdapter`.
- Resultado é um `Video` (domínio) com:
  - ID (gerado internamente pelo `Video`)
  - nome original
  - caminho do arquivo temporário
  - tipo / tamanho

Configuração relevante:

- `video.temp-dir` em `src/main/resources/application.yml` (default: `/tmp/coulson-videos`)

### 2.2) Divisão do vídeo (quando necessário)

Porta:

- `VideoSplitterPort`

Implementação:

- `FfmpegVideoSplitterAdapter`

Como decide dividir:

- Obtém duração com `ffprobe` (`probeDuration`).
- Cria um `SplitPlan` via `VideoSplitPlanService.computePlan(video, maxChunkSizeBytes)`.
- Se `SplitPlan` não exigir divisão: retorna um único `VideoChunk` apontando para o arquivo original.
- Se exigir: executa múltiplos comandos FFmpeg com `-ss`, `-t`, `-c copy` para gerar chunks.

Configuração relevante:

- `video.max-chunk-size-mb` (default: 200)
- `ffmpeg.path`, `ffmpeg.ffprobe-path`

### 2.3) Extração de áudio (paralelo)

Porta:

- `AudioExtractorPort`

Implementação:

- `FfmpegAudioExtractorAdapter`

Como funciona:

- Para cada `VideoChunk`, cria um `CompletableFuture` com `supplyAsync(..., executor)`.
- O executor vem de `AppConfig.processingExecutor`, com tamanho `webhook.transcription-parallelism` (default: 4).
- Executa FFmpeg com:
  - `-vn` (sem vídeo)
  - `-ar` sample rate (default: 16000)
  - `-ac` canais (default: 1)
  - `-b:a` bitrate (default: 32k)

Saída:

- Gera `AudioChunk` por chunk de vídeo, com índice (0..N-1) e arquivo `.mp3` no temp dir.

### 2.4) Merge dos áudios extraídos (concat)

Porta:

- `AudioMergerPort`

Implementação:

- `FfmpegAudioMergerAdapter`

Comportamento:

- Se houver apenas 1 chunk de áudio, não cria arquivo novo (retorna referência ao mesmo arquivo).
- Se houver vários:
  - escreve um arquivo `concat list` no temp dir (ex.: `...-concat.txt`)
  - executa FFmpeg com `-f concat -safe 0 -c copy` para criar `...-merged.mp3`

### 2.5) Verificação de tamanho do áudio final

No `ProcessVideoUseCase`, após o merge:

- Lê o tamanho do MP3 final com `Files.size(...)`.
- **Regra de limite**: se o áudio final passar de **25MB**, lança `IllegalStateException`:
  - `"Arquivo muito extenso para transcrição..."`

Isso existe mesmo com upload permitido de até 4GB; na prática é uma proteção para o tamanho do áudio enviado ao serviço de transcrição.

### 2.6) Divisão do áudio por duração (para transcrição)

Porta:

- `AudioSplitterPort`

Implementação:

- `FfmpegAudioSplitterAdapter`

Como funciona:

- Mede duração do áudio via `ffprobe`.
- Se duração <= `maxAudioChunkSeconds`, retorna o próprio `AudioChunk`.
- Se maior:
  - calcula `numChunks = ceil(totalDuration / maxAudioChunkSeconds)`
  - para cada chunk:
    - FFmpeg com `-ss`, `-t`, `-c copy` para gerar `...-audio-chunk-i.mp3`
    - cria `AudioChunk` contendo também o `startOffsetSeconds` (offset do chunk em relação ao áudio total)

Configuração relevante:

- `video.max-audio-chunk-seconds` (default: 400)

### 2.7) Transcrição via webhook (paralelo)

Porta:

- `TranscriptionWebhookPort`

Implementação:

- `HttpTranscriptionWebhookAdapter`

O que envia:

- `POST` multipart/form-data
- campo `file` com o MP3 do chunk
- header `webhook-token` com valor de `webhook.token`

URL:

- Montada por `WebhookProperties.getTranscribeUrl()` = `baseUrl + transcribePath`
- Defaults:
  - `webhook.base-url`: `http://n8n:5678`
  - `webhook.transcribe-path`: `/webhook/transcribe`

O que recebe:

- Espera um JSON com pelo menos:
  - `text`
  - `segments`: array de `{ speaker, start, end, text }`
- O adapter tenta parsear JSON; se falhar, retorna o corpo bruto como texto e segmentos vazios.

Paralelismo:

- Para cada `AudioChunk` a transcrever, cria `CompletableFuture` no mesmo `executor`.

Tratamento de falhas:

- `joinAllOrCancel(...)` faz `allOf(...).join()`.
- Se algum future falha:
  - cancela os demais (`cancel(true)`)
  - lança `IllegalStateException` agregando a causa

### 2.8) Resolução de nomes de locutores via webhook

Objetivo:

- Substituir códigos do diarization (ex.: `speaker_0`) por nomes reais (ex.: `Matheus`).

Porta:

- `SpeakerResolutionWebhookPort`

Implementação:

- `HttpSpeakerResolutionWebhookAdapter`

O que envia:

- `POST` JSON
- header `webhook-token`
- body:
  - `{ "segments": [ { speaker, start, end, text }, ... ] }`

URL:

- `WebhookProperties.getSpeakersUrl()` = `baseUrl + speakersPath`
- Default `webhook.speakers-path`: `/webhook/speakers`

O que recebe:

- Uma lista JSON de mapeamentos:
  - `[ { "speaker": "speaker_0", "name": "..." }, ... ]`

Aplicação:

- `ProcessVideoUseCase.applyMappings(...)` cria um `Map<speakerCode, name>`.
- Para cada segmento, troca `speaker` se existir mapping, senão mantém o valor original.
- Se o webhook retornar vazio, o chunk mantém os códigos originais.

### 2.9) Agregação final (texto + segmentos com offsets)

Após obter as transcrições dos chunks:

- Ordena por `startOffsetSeconds` para manter a sequência do áudio.
- Concatena os textos em um `StringBuilder` (com espaço entre chunks).
- Para cada segmento do chunk:
  - soma o offset do chunk em `start` e `end`
  - adiciona ao array final `allSegments`

O resultado retornado ao controller é:

- `new TranscriptionResult(videoId, combinedText, videoChunks.size(), allSegments)`

### 2.10) Persistência no PostgreSQL (meetings + segments)

Método: `persistMeeting(video, fullText, segments)`

- Cria um `Meeting` (domínio) usando:
  - id = `video.getId()`
  - title = `video.getOriginalName()`
  - duration = `video.getDurationSeconds()`
  - fullText + segments
- Chama `meetingRepositoryPort.save(meeting)`

Implementação:

- `MeetingRepositoryPort -> JpaMeetingRepositoryAdapter`

Como grava:

- Converte `Meeting` -> `MeetingEntity`
- Grava `MeetingEntity` e seus `TranscriptionSegmentEntity` em cascata.

Schema (Flyway):

- `src/main/resources/db/migration/V1__create_schema.sql`
- Tabelas:
  - `meetings`
  - `transcription_segments` (com coluna `embedding vector(1536)`)

Nota:

- `MeetingEntity.videoStorageKey` existe no schema, mas no fluxo atual do `/transcribe` não é preenchido (o vídeo fica em disco temporário e é deletado no final).

### 2.11) Evento de domínio e embeddings (opcional, assíncrono)

Após salvar a meeting:

- `eventPublisher.publishEvent(new MeetingTranscribedEvent(videoId))`

Listener:

- `src/main/java/com/yellowleaf/coulson/application/event/EmbeddingEventListener.java`

Condição de ativação:

- Só existe se `EmbeddingPort` estiver registrado (`@ConditionalOnBean(EmbeddingPort.class)`).
- `EmbeddingPort` é criado apenas quando:
  - `spring.ai.openai.embedding.enabled=true` (mapeado a `OPENAI_EMBEDDING_ENABLED`)

Processo:

- Executa assíncrono (`@Async("embeddingExecutor")`).
- Busca segmentos sem embedding:
  - `SELECT id, text FROM transcription_segments WHERE meeting_id = ? AND embedding IS NULL`
  - via `VectorUpdateRepository` (JdbcTemplate)
- Para cada segmento:
  - chama `EmbeddingPort.embed(text)` (OpenAI via Spring AI)
  - atualiza `transcription_segments.embedding`
- Se todos os segmentos forem embedados:
  - marca `meetings.embedded_at` com timestamp atual

### 2.12) Limpeza de arquivos temporários

O `execute()` mantém uma lista `filesToCleanup` com paths de:

- arquivo original
- chunks de vídeo (se existirem)
- áudios extraídos
- chunks de áudio para transcrição (se existirem)

No `finally`, chama `Files.deleteIfExists(...)` em cada item.

## Integrações externas envolvidas

## 1) FFmpeg/FFprobe (processamento local)

Usado para:

- medir duração do vídeo e do áudio (ffprobe)
- dividir vídeo em chunks por tamanho (ffmpeg com `-c copy`)
- extrair áudio do vídeo (ffmpeg)
- concatenar áudios (ffmpeg concat)
- dividir áudio por duração (ffmpeg com `-c copy`)

Dependência:

- binários configurados por `ffmpeg.path` e `ffmpeg.ffprobe-path`.

## 2) Webhooks HTTP (tipicamente n8n)

O backend não chama diretamente a OpenAI para transcrição. Em vez disso, envia para um webhook configurável.

- **Transcrição**: `POST {WEBHOOK_BASE_URL}{WEBHOOK_TRANSCRIBE_PATH}`
  - Multipart com `file`
  - Header `webhook-token`
- **Resolução de locutores**: `POST {WEBHOOK_BASE_URL}{WEBHOOK_SPEAKERS_PATH}`
  - JSON com segmentos
  - Header `webhook-token`

Arquivo de referência (infra):

- `infra/n8n/workflows/transcribe-service.json`
  - expõe um webhook `transcribe`
  - encaminha para `https://api.openai.com/v1/audio/transcriptions`
  - usa modelo `gpt-4o-transcribe-diarize` com `response_format=diarized_json`

## 3) OpenAI (opcional, embeddings)

Se habilitado, o backend chama OpenAI via Spring AI para embeddings de texto:

- Modelo configurado: `text-embedding-3-small`
- Persistência do vetor em `transcription_segments.embedding` (pgvector)

## Persistências envolvidas

- **PostgreSQL** (via JPA + JDBC para vetores):
  - `meetings`: metadados + `full_text` + `embedded_at`
  - `transcription_segments`: segmentos com timestamps + `embedding` (opcional)
- **Disco local (temporário)**:
  - vídeo original e arquivos gerados (chunks de vídeo, MP3s, concat list)
  - sempre deletados no `finally` do use case

O endpoint `/transcribe` não persiste o arquivo de vídeo em storage externo; apenas os dados estruturados da transcrição são persistidos no banco.

