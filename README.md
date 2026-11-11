# Coulson Assistant

Backend for a RAG-based workplace assistant. It receives meeting video recordings, transcribes audio with speaker identification, and stores transcriptions with optional vector semantic search.

---

## Overview

The system processes meeting videos in parallel and stores results in PostgreSQL with the `pgvector` extension, so an AI agent can query meeting content in natural language.

**Main pipeline:**

```
Video upload (up to 4 GB)
  → Split into chunks of up to 200 MB (FFmpeg)
  → Parallel audio extraction (FFmpeg, mono 16 kHz 32k)
  → Merge audio into a single MP3
  → Split audio into chunks of up to 400 s
  → Parallel transcription via webhook
  → Speaker name resolution via webhook
  → Persist to PostgreSQL (meetings + segments)
  → Optional video persistence to Supabase Storage
  → Optional vector embeddings (OpenAI)
```

Transcription is asynchronous: `POST /api/v1/videos/transcribe` returns `202 Accepted` with a `jobId`. Poll `GET /api/v1/videos/transcribe/{jobId}` until status is `DONE` or `FAILED`.

---

## Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Build | Maven 3.9 |
| Database | PostgreSQL 15 (Supabase) |
| Semantic search | pgvector (PostgreSQL extension) |
| Migrations | Flyway |
| Media processing | FFmpeg |
| Embeddings | Spring AI + OpenAI `text-embedding-3-small` (optional) |
| API docs | Springdoc OpenAPI (Swagger UI) |
| Containers | Docker + Docker Compose |

---

## Prerequisites

- Docker and Docker Compose installed
- External Docker network `yellowleaf-net` created:

```bash
docker network create yellowleaf-net
```

- An n8n instance on the same network, with the webhooks configured (see below)

---

## Local environment

### 1. Supabase (database)

```bash
cd infra/supabase
docker compose up -d
```

Services after startup:

| Service | URL |
|---|---|
| API Gateway (Kong) | http://localhost:8000 |
| Supabase Studio | http://localhost:8000 (HTTP Basic Auth: `DASHBOARD_USERNAME` / `DASHBOARD_PASSWORD` in `infra/supabase/.env`) |
| PostgreSQL | localhost:5432 |

Default credentials live in `infra/supabase/.env`.

### 2. Coulson Assistant

Copy and adjust environment variables:

```bash
cp .env.example .env
```

Build and start:

```bash
docker compose build
docker compose up -d
```

| Resource | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui |
| OpenAPI | http://localhost:8080/api-docs |
| Health | http://localhost:8080/actuator/health |

Full endpoint reference for agents: [`doc/COULSON_API-INFO_FOR_AGENTS.md`](doc/COULSON_API-INFO_FOR_AGENTS.md).

---

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `WEBHOOK_BASE_URL` | `http://n8n:5678` | n8n base URL |
| `WEBHOOK_TRANSCRIBE_PATH` | `/webhook/transcribe` | Transcription webhook path |
| `WEBHOOK_SPEAKERS_PATH` | `/webhook/speakers` | Speaker resolution webhook path |
| `WEBHOOK_TOKEN` | — | Webhook auth token |
| `WEBHOOK_CONNECT_TIMEOUT` | `30` | Connect timeout (seconds) |
| `WEBHOOK_READ_TIMEOUT` | `300` | Read timeout (seconds) |
| `WEBHOOK_TRANSCRIPTION_PARALLELISM` | `8` | Audio chunks processed in parallel |
| `VIDEO_MAX_CHUNK_SIZE_MB` | `200` | Max video chunk size (MB) |
| `VIDEO_MAX_AUDIO_CHUNK_SECONDS` | `400` | Max audio chunk duration (seconds) |
| `FFMPEG_AUDIO_SAMPLE_RATE` | `16000` | Extracted audio sample rate |
| `FFMPEG_AUDIO_CHANNELS` | `1` | Audio channels (mono) |
| `FFMPEG_AUDIO_BITRATE` | `32k` | Compressed audio bitrate |
| `SUPABASE_DB_URL` | — | PostgreSQL JDBC URL |
| `SUPABASE_DB_USER` | `postgres` | Database user |
| `SUPABASE_DB_PASSWORD` | — | Database password |
| `SUPABASE_URL` | — | Supabase API base URL |
| `SUPABASE_SERVICE_ROLE_KEY` | — | Supabase service role key |
| `SUPABASE_STORAGE_BUCKET` | `meetings` | Storage bucket for meeting assets |
| `SUPABASE_VOICE_SAMPLES_BUCKET` | `voice-samples` | Bucket for participant voice samples |
| `SUPABASE_PRESENTATION_AUDIO_BUCKET` | `presentation-audio` | Bucket for presentation audio |
| `VIDEO_PERSIST_ENABLED` | `false` | Enable video transcoding and storage |
| `VIDEO_PERSIST_BUCKET` | `videos` | Bucket for persisted videos |
| `OPENAI_API_KEY` | — | OpenAI API key (optional) |
| `OPENAI_EMBEDDING_ENABLED` | `false` | Enable embedding generation |
| `ELEVENLABS_API_KEY` | — | ElevenLabs API key (optional) |

---

## API (summary)

Base path: `/api/v1`

### Videos

| Method | Path | Description |
|---|---|---|
| `POST` | `/videos/transcribe` | Submit video for async transcription (`multipart/form-data`, optional `teamId`, `force`) |
| `GET` | `/videos/transcribe/{jobId}` | Poll transcription job status/result |
| `GET` | `/videos/assets` | List all persisted video assets |
| `POST` | `/videos/by-team/{teamId}/persist` | Persist/transcode a video for a team (requires `VIDEO_PERSIST_ENABLED=true`) |
| `GET` | `/videos/by-team/{teamId}` | List video assets for a team |

**Transcription job statuses:** `PROCESSING`, `DONE`, `FAILED`

### Participants

| Method | Path | Description |
|---|---|---|
| `POST` | `/participants` | Create participant (optional voice sample) |
| `GET` | `/participants` | List or get by `id` |
| `PUT` | `/participants?id={id}` | Update participant |
| `PATCH` | `/participants?id={id}` | Partial update |
| `DELETE` | `/participants?id={id}` | Delete participant |
| `GET` | `/participants/voice-sample?id={id}` | Download voice sample |
| `POST` | `/participants/{id}/presentation-voice` | Upload presentation voice |
| `GET` | `/participants/{id}/presentation-voice` | Download presentation voice |
| `DELETE` | `/participants/{id}/presentation-voice` | Delete presentation voice |

### Teams

| Method | Path | Description |
|---|---|---|
| `POST` | `/teams` | Create team |
| `GET` | `/teams` | List or get by `id` |
| `DELETE` | `/teams?id={id}` | Delete team and memberships |
| `POST` | `/teams/{id}/presentation-audio` | Upload team presentation audio |
| `GET` | `/teams/{id}/presentation-audio` | Download presentation audio |
| `DELETE` | `/teams/{id}/presentation-audio` | Delete presentation audio |

### Team members

| Method | Path | Description |
|---|---|---|
| `POST` | `/team-members?teamId={teamId}` | Add participant to team |
| `GET` | `/team-members` | List by `teamId` (optional `participantId`) |
| `DELETE` | `/team-members?teamId={teamId}&participantId={participantId}` | Remove membership |

---

## Expected webhooks (n8n)

### `POST /webhook/transcribe`

Receives an MP3 audio chunk and returns transcription with timestamps.

**Request:** `multipart/form-data` with the MP3 file and `webhook-token` header.

**Expected response:**

```json
{
  "text": "Chunk text...",
  "segments": [
    { "speaker": "speaker_0", "start": 0.12, "end": 3.45, "text": "..." }
  ]
}
```

### `POST /webhook/speakers`

Receives segment speaker codes and returns real-name mapping.

**Request body:**

```json
{
  "segments": [
    { "speaker": "speaker_0", "start": 0.12, "end": 3.45, "text": "..." }
  ]
}
```

**Expected response:**

```json
[
  { "speaker": "speaker_0", "name": "Matheus" },
  { "speaker": "speaker_1", "name": "Amanda" }
]
```

---

## Embeddings and semantic search (optional)

When `OPENAI_EMBEDDING_ENABLED=true` and `OPENAI_API_KEY` are set, the service generates embeddings for each transcribed segment using OpenAI `text-embedding-3-small` (1536 dimensions).

Vectors are stored in `transcription_segments.embedding` and can be queried by cosine similarity for the conversational assistant knowledge base.

```env
OPENAI_API_KEY=sk-...
OPENAI_EMBEDDING_ENABLED=true
```

---

## Architecture

The project follows DDD with clear layer boundaries:

```
src/main/java/com/yellowleaf/coulson/
├── domain/
│   ├── model/          # Pure domain entities
│   ├── port/           # Interfaces (contracts)
│   ├── event/          # Domain events
│   └── service/        # Domain logic without external deps
├── application/
│   ├── usecase/        # Use cases (orchestration)
│   ├── command/        # Use case input objects
│   ├── result/         # Use case output objects
│   └── event/          # Async event listeners
├── infrastructure/
│   ├── adapter/        # Port implementations
│   ├── config/         # Spring config (beans, properties)
│   └── persistence/    # JPA entities and repositories
└── interfaces/
    └── rest/           # Controllers, DTOs, exception handler
```

---

## License

This project is licensed under the [MIT License](LICENSE).
