# Coulson Assistant

Backend de um assistente de trabalho baseado em RAG (Retrieval-Augmented Generation). Recebe gravações de reuniões em vídeo, transcreve o áudio com identificação de locutores e armazena as transcrições com suporte a busca semântica por vetores.

---

## Visão geral

O sistema processa vídeos de reuniões de forma paralela e armazena os resultados em um banco PostgreSQL com extensão `pgvector`, possibilitando que um agente de IA consulte o conteúdo das reuniões por linguagem natural.

**Fluxo principal:**

```
Video upload (até 4GB)
  → Divisão em chunks de até 200MB (FFmpeg)
  → Extração de áudio em paralelo (FFmpeg, mono 16kHz 32k)
  → Merge do áudio em um único MP3
  → Divisão do áudio em chunks de até 400s
  → Transcrição em paralelo via webhook
  → Resolução de nomes dos locutores via webhook
  → Persistência no PostgreSQL (meetings + segments)
  → Geração de embeddings vetoriais (opcional, OpenAI)
```

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Build | Maven 3.9 |
| Banco de dados | PostgreSQL 15 (Supabase) |
| Busca semântica | pgvector (extensão PostgreSQL) |
| Migrations | Flyway |
| Processamento de mídia | FFmpeg |
| Embeddings | Spring AI + OpenAI `text-embedding-3-small` (opcional) |
| Documentação da API | Springdoc OpenAPI (Swagger UI) |
| Containerização | Docker + Docker Compose |

---

## Pré-requisitos

- Docker e Docker Compose instalados
- Rede Docker externa `yellowleaf-net` criada:
  ```bash
  docker network create yellowleaf-net
  ```
- Instância do n8n rodando na mesma rede, com os webhooks configurados (ver abaixo)

---

## Subindo o ambiente local

### 1. Supabase (banco de dados)

```bash
cd supabase
docker compose up -d
```

Serviços disponíveis após inicialização:

| Serviço | URL |
|---|---|
| API Gateway (Kong) | http://localhost:8000 |
| Supabase Studio | http://localhost:3001 |
| PostgreSQL direto | localhost:5432 |

> As credenciais padrão estão no arquivo `supabase/.env`.

### 2. Coulson Assistant

Copie e ajuste as variáveis de ambiente:

```bash
cp .env.example .env
# edite .env com seus valores
```

Build e inicialização:

```bash
docker compose build
docker compose up -d
```

API disponível em: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui`

---

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `WEBHOOK_BASE_URL` | `http://n8n:5678` | URL base do n8n |
| `WEBHOOK_TRANSCRIBE_PATH` | `/webhook/transcribe` | Rota de transcrição |
| `WEBHOOK_SPEAKERS_PATH` | `/webhook/speakers` | Rota de resolução de locutores |
| `WEBHOOK_TOKEN` | — | Token de autenticação dos webhooks |
| `WEBHOOK_CONNECT_TIMEOUT` | `30` | Timeout de conexão (segundos) |
| `WEBHOOK_READ_TIMEOUT` | `300` | Timeout de leitura (segundos) |
| `WEBHOOK_TRANSCRIPTION_PARALLELISM` | `4` | Chunks de áudio processados em paralelo |
| `VIDEO_MAX_CHUNK_SIZE_MB` | `200` | Tamanho máximo de chunk de vídeo (MB) |
| `VIDEO_MAX_AUDIO_CHUNK_SECONDS` | `400` | Duração máxima de chunk de áudio (segundos) |
| `FFMPEG_AUDIO_SAMPLE_RATE` | `16000` | Sample rate do áudio extraído |
| `FFMPEG_AUDIO_CHANNELS` | `1` | Canais de áudio (mono) |
| `FFMPEG_AUDIO_BITRATE` | `32k` | Bitrate do áudio comprimido |
| `SUPABASE_DB_URL` | — | JDBC URL do PostgreSQL |
| `SUPABASE_DB_USER` | `postgres` | Usuário do banco |
| `SUPABASE_DB_PASSWORD` | — | Senha do banco |
| `OPENAI_API_KEY` | — | Chave da API OpenAI (opcional) |
| `OPENAI_EMBEDDING_ENABLED` | `false` | Habilita geração de embeddings |

---

## API

### Transcrição

#### `POST /api/v1/videos/transcribe`

Recebe um arquivo de vídeo e retorna a transcrição completa com segmentos por locutor.

**Request:** `multipart/form-data`

| Campo | Tipo | Descrição |
|---|---|---|
| `file` | Binary | Arquivo de vídeo (mp4, mov, mkv, avi, webm, etc.) |

**Response `200`:**

```json
{
  "videoId": "d07c147c-a4b0-4a9e-96dc-6a7c58b65a82",
  "transcription": "Texto completo da reunião...",
  "totalChunks": 4,
  "segments": [
    {
      "speaker": "Matheus",
      "start": 0.596,
      "end": 3.45,
      "text": "Bom dia, pessoal."
    }
  ]
}
```

---

### Equipes

#### `POST /api/v1/teams`

Cria uma equipe.

```json
{ "name": "Time de Produto" }
```

#### `GET /api/v1/teams`

Lista todas as equipes.

#### `GET /api/v1/teams?id={id}`

Busca equipe pelo ID, incluindo seus membros.

#### `DELETE /api/v1/teams?id={id}`

Remove equipe e todos os seus membros.

---

### Membros de equipe

#### `POST /api/v1/team-members?teamId={teamId}`

Adiciona um membro à equipe.

```json
{ "name": "Matheus" }
```

#### `GET /api/v1/team-members?teamId={teamId}`

Lista todos os membros de uma equipe.

#### `GET /api/v1/team-members?teamId={teamId}&memberId={memberId}`

Busca um membro específico.

#### `PUT /api/v1/team-members?memberId={memberId}`

Atualiza o nome de um membro.

```json
{ "name": "Matheus Costa" }
```

#### `DELETE /api/v1/team-members?memberId={memberId}`

Remove um membro.

---

## Schema do banco de dados

```sql
-- Reuniões transcritas
CREATE TABLE meetings (
    id                UUID          PRIMARY KEY,
    title             VARCHAR(500)  NOT NULL,
    recorded_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    duration_sec      NUMERIC(10,2),
    video_storage_key VARCHAR(1000),
    full_text         TEXT          NOT NULL,
    embedded_at       TIMESTAMPTZ
);

-- Segmentos com timestamps e locutor resolvido
CREATE TABLE transcription_segments (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id  UUID          NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    speaker     VARCHAR(255)  NOT NULL,
    start_sec   NUMERIC(10,3) NOT NULL,
    end_sec     NUMERIC(10,3) NOT NULL,
    text        TEXT          NOT NULL,
    embedding   vector(1536)               -- preenchido quando OPENAI_EMBEDDING_ENABLED=true
);

-- Equipes de reunião
CREATE TABLE teams (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255)  NOT NULL UNIQUE,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Membros das equipes
CREATE TABLE team_members (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id    UUID          NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    name       VARCHAR(255)  NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_team_member_name UNIQUE (team_id, name)
);
```

O índice HNSW no campo `embedding` habilita busca por similaridade semântica (cosine):

```sql
CREATE INDEX idx_segments_embedding
    ON transcription_segments USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;
```

---

## Webhooks esperados (n8n)

O serviço depende de dois endpoints externos (tipicamente configurados no n8n):

### `POST /webhook/transcribe`

Recebe um chunk de áudio MP3 e retorna a transcrição com timestamps.

**Request:** `multipart/form-data` com o arquivo MP3 e header `webhook-token`.

**Response esperado:**

```json
{
  "text": "Texto do trecho...",
  "segments": [
    { "speaker": "speaker_0", "start": 0.12, "end": 3.45, "text": "..." }
  ]
}
```

### `POST /webhook/speakers`

Recebe os segmentos de um chunk e retorna o mapeamento de códigos para nomes reais.

**Request body:**

```json
{
  "segments": [
    { "speaker": "speaker_0", "start": 0.12, "end": 3.45, "text": "..." }
  ]
}
```

**Response esperado:**

```json
[
  { "speaker": "speaker_0", "name": "Matheus" },
  { "speaker": "speaker_1", "name": "Amanda" }
]
```

---

## Embeddings e busca semântica (opcional)

Quando `OPENAI_EMBEDDING_ENABLED=true` e `OPENAI_API_KEY` estão configurados, o serviço gera automaticamente embeddings para cada segmento transcrito usando o modelo `text-embedding-3-small` da OpenAI (1536 dimensões).

Os vetores ficam armazenados na coluna `embedding` da tabela `transcription_segments` e podem ser consultados por similaridade semântica, habilitando a base de conhecimento do assistente conversacional.

**Para habilitar no `.env`:**

```env
OPENAI_API_KEY=sk-...
OPENAI_EMBEDDING_ENABLED=true
```

---

## Arquitetura

O projeto segue DDD com separação clara de camadas:

```
src/main/java/com/yellowleaf/coulson/
├── domain/
│   ├── model/          # Entidades de domínio puras
│   ├── port/           # Interfaces (contratos)
│   ├── event/          # Eventos de domínio
│   └── service/        # Lógica de domínio sem dependências externas
├── application/
│   ├── usecase/        # Casos de uso (orquestração)
│   ├── command/        # Objetos de entrada dos use cases
│   ├── result/         # Objetos de saída dos use cases
│   └── event/          # Listeners de eventos assíncronos
├── infrastructure/
│   ├── adapter/        # Implementações das portas
│   ├── config/         # Configuração Spring (beans, properties)
│   └── persistence/    # JPA entities e repositories
└── interfaces/
    └── rest/           # Controllers, DTOs, exception handler
```
