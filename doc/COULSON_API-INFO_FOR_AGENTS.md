# Coulson Assistant Service — API Reference for AI Agents

> This document describes all REST endpoints exposed by the Coulson Assistant Service.
> It is intended as context for AI coding agents that need to integrate with or build upon this API.

---

## General Information

| Property | Value |
|---|---|
| Service name | `coulson-assistant` |
| Default port | `8080` |
| Base path | `/api/v1` |
| Max upload size | `4 GB` |
| Swagger UI | `GET /swagger-ui` |
| OpenAPI docs | `GET /api-docs` |
| External URL (Kong gateway) | `https://<host>/coulson` |

All responses use `Content-Type: application/json` unless otherwise noted.
All timestamps follow ISO-8601 with timezone offset (e.g. `2026-04-28T10:00:00-03:00`).
All IDs are UUIDs v4.

---

## Error Response

Every error from any endpoint follows the same structure.

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Participant not found: 3fa85f64-...",
  "timestamp": "2026-04-28T13:00:00.000Z"
}
```

| Field | Type | Description |
|---|---|---|
| `status` | `integer` | HTTP status code |
| `error` | `string` | Short label (e.g. "Not Found", "Bad Request") |
| `message` | `string` | Human-readable detail |
| `timestamp` | `string (ISO-8601)` | Moment the error occurred |

### Global error mapping

| Exception | HTTP Status |
|---|---|
| `NoSuchElementException` | `404 Not Found` |
| `MethodArgumentNotValidException` | `400 Bad Request` |
| `MaxUploadSizeExceededException` | `413 Payload Too Large` |
| `RestClientException` | `502 Bad Gateway` |
| `UncheckedIOException` | `500 Internal Server Error` |
| `IllegalArgumentException` | `400 Bad Request` |
| `IllegalStateException` | `409 Conflict` |
| Uncaught `Exception` | `500 Internal Server Error` |

---

## Tag: Videos

Base path: `/api/v1/videos`

---

### POST /api/v1/videos/transcribe

Submit a video file for asynchronous transcription. Returns a job ID immediately. The processing pipeline runs in the background:

1. Video is stored locally.
2. If it exceeds 200 MB, FFmpeg splits it into chunks (stream copy, no re-encoding).
3. Audio is extracted from each chunk in parallel.
4. Audio chunks are merged, compressed to mono 8 kHz for transcript submission.
5. Each audio chunk is sent to the configured transcription webhook in parallel.
6. Speaker resolution webhook is called to map speaker codes to participant names.
7. Segments are time-offset-corrected and combined into a final result.
8. A `Meeting` record is persisted in the database.
9. If `teamId` was provided, a `VideoAsset` is also created and the transcoded video is uploaded to Supabase Storage asynchronously.

**Request**

- `Content-Type: multipart/form-data`

| Parameter | Location | Type | Required | Description |
|---|---|---|---|---|
| `file` | form field | `MultipartFile` | yes | Video file. Any FFmpeg-supported format (mp4, mov, mkv, avi, webm, etc.) |
| `teamId` | query param | `UUID` | no | Team ID. When provided, triggers video persistence to Supabase Storage after transcription. |
| `force` | query param | `boolean` | no (default `false`) | When `true`, bypasses duplicate detection and reprocesses even if a transcription for this file already exists. |

**Responses**

| Status | Description | Body |
|---|---|---|
| `202 Accepted` | Job accepted, transcription running in background | `TranscriptionJobResponse` |
| `400 Bad Request` | Empty or missing file | `ErrorResponse` |
| `413 Payload Too Large` | File exceeds server upload limit | `ErrorResponse` |
| `500 Internal Server Error` | Error during file storage | `ErrorResponse` |

**Response body — `TranscriptionJobResponse`**

```json
{
  "jobId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "PROCESSING",
  "videoName": "reuniao-2026-04-28.mp4",
  "error": null,
  "result": null,
  "createdAt": "2026-04-28T10:00:00-03:00",
  "updatedAt": "2026-04-28T10:00:05-03:00"
}
```

| Field | Type | Description |
|---|---|---|
| `jobId` | `UUID` | Unique job identifier. Use this to poll for the result. |
| `status` | `string` | `PROCESSING`, `DONE`, or `FAILED` |
| `videoName` | `string` | Original filename of the uploaded video |
| `error` | `string \| null` | Error message. Present only when `status = FAILED` |
| `result` | `object \| null` | Full transcription result. Present only when `status = DONE`. See `TranscriptionResult` below. |
| `createdAt` | `string (ISO-8601)` | When the job was created |
| `updatedAt` | `string (ISO-8601)` | Last status change |

**Nested — `result` object when `status = DONE`**

```json
{
  "videoId": "a3f1c2d4-89ab-4321-bcde-0123456789ab",
  "transcription": "Hello, welcome to the meeting. Today we will...",
  "totalChunks": 2,
  "segments": [
    {
      "speaker": "Matheus",
      "start": 0.12,
      "end": 3.45,
      "text": "Hello, welcome to the meeting."
    },
    {
      "speaker": "Ana",
      "start": 3.90,
      "end": 7.10,
      "text": "Today we will discuss..."
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `videoId` | `string` | Internal ID generated for this video processing run |
| `transcription` | `string` | Full joined transcription text |
| `totalChunks` | `integer` | Number of video chunks processed. Will be `1` if file was under 200 MB. |
| `segments` | `array` | Speaker segments in chronological order |
| `segments[].speaker` | `string` | Speaker name (resolved from participant registry) or raw code like `speaker_0` if unresolved |
| `segments[].start` | `number` | Segment start time in seconds |
| `segments[].end` | `number` | Segment end time in seconds |
| `segments[].text` | `string` | Transcribed text for this segment |

---

### GET /api/v1/videos/transcribe/{jobId}

Poll the status and result of a previously submitted transcription job.

**Path Variables**

| Variable | Type | Required | Description |
|---|---|---|---|
| `jobId` | `UUID` | yes | Job ID returned by `POST /api/v1/videos/transcribe` |

**Responses**

| Status | Description | Body |
|---|---|---|
| `200 OK` | Job found | `TranscriptionJobResponse` (same structure as POST response) |
| `404 Not Found` | No job with the given ID | `ErrorResponse` |

**Usage pattern:**

```
POST /api/v1/videos/transcribe  →  202 { jobId: "abc..." }
GET  /api/v1/videos/transcribe/abc...  →  200 { status: "PROCESSING" }
GET  /api/v1/videos/transcribe/abc...  →  200 { status: "DONE", result: { ... } }
```

---

### GET /api/v1/videos/assets

List all video assets persisted in Supabase Storage (across all teams).

**Responses**

| Status | Description | Body |
|---|---|---|
| `200 OK` | Asset list | `VideoAssetResponse[]` |

---

### POST /api/v1/videos/by-team/{teamId}/persist

Manually upload a video to be transcoded and persisted to Supabase Storage for a specific team. This endpoint requires `VIDEO_PERSIST_ENABLED=true` on the server.

The job is asynchronous: the response is returned immediately with status `UPLOADED` while processing runs in the background.

**Path Variables**

| Variable | Type | Required | Description |
|---|---|---|---|
| `teamId` | `UUID` | yes | Team the video belongs to |

**Request**

- `Content-Type: multipart/form-data`

| Parameter | Location | Type | Required | Description |
|---|---|---|---|---|
| `file` | form field | `MultipartFile` | yes | Video file to be transcoded and stored |

**Responses**

| Status | Description | Body |
|---|---|---|
| `202 Accepted` | Processing queued | `VideoAssetResponse` |
| `503 Service Unavailable` | Video persistence is disabled on the server | `ErrorResponse` |
| `409 Conflict` | Asset for this team is already processing | `ErrorResponse` |

---

### GET /api/v1/videos/by-team/{teamId}

List all video assets belonging to a specific team.

**Path Variables**

| Variable | Type | Required | Description |
|---|---|---|---|
| `teamId` | `UUID` | yes | Team ID |

**Responses**

| Status | Description | Body |
|---|---|---|
| `200 OK` | Asset list | `VideoAssetResponse[]` |

---

### VideoAssetResponse structure

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "refId": "manual-a3f1c2d4-89ab-4321-bcde-0123456789ab",
  "teamId": "a3f1c2d4-89ab-4321-bcde-0123456789ab",
  "teamName": "Time de Produto",
  "status": "READY",
  "playbackUrl": "https://storage.example.com/meetings/videos/manual-.../playback_540p.mp4",
  "hlsMasterUrl": null,
  "durationMs": 3600000,
  "width": 960,
  "height": 540,
  "fps": 30.0,
  "playbackSizeBytes": 52428800,
  "errorMessage": null,
  "createdAt": "2026-04-28T10:00:00-03:00",
  "updatedAt": "2026-04-28T10:05:00-03:00"
}
```

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Asset internal ID |
| `refId` | `string` | Reference ID. For transcription-triggered assets: the transcription job's video ID. For manually uploaded: `manual-{teamId}` |
| `teamId` | `UUID` | Owner team |
| `teamName` | `string` | Team name |
| `status` | `string` | `UPLOADED`, `PROCESSING`, `READY`, or `FAILED` |
| `playbackUrl` | `string \| null` | URL for the transcoded playback video (540p MP4). Present when `status = READY`. May be a public URL or a signed URL depending on server configuration. |
| `hlsMasterUrl` | `string \| null` | HLS master playlist URL. Present only when HLS is enabled on the server. |
| `durationMs` | `long \| null` | Video duration in milliseconds |
| `width` | `integer \| null` | Playback video width in pixels |
| `height` | `integer \| null` | Playback video height in pixels (max 540 by default) |
| `fps` | `number \| null` | Frames per second of the playback file |
| `playbackSizeBytes` | `long \| null` | Size of the transcoded playback file in bytes |
| `errorMessage` | `string \| null` | Error description. Present only when `status = FAILED` |
| `createdAt` | `string (ISO-8601)` | Creation timestamp |
| `updatedAt` | `string (ISO-8601)` | Last update timestamp |

**`status` lifecycle:**

```
UPLOADED → PROCESSING → READY
                     → FAILED
```

---

## Tag: Participants

Base path: `/api/v1/participants`

Participants are the people who attend meetings. They can optionally have a voice sample attached, which is used by the speaker resolution webhook to map speaker codes to real names during transcription.

---

### POST /api/v1/participants

Register a new participant. Optionally attaches a voice sample file.

**Request**

- `Content-Type: multipart/form-data`

| Parameter | Location | Type | Required | Description |
|---|---|---|---|---|
| `name` | form field | `string` | yes | Full name of the participant |
| `role` | form field | `string` | no | Job role or title. Defaults to `"indefinido"` if omitted or blank. |
| `voiceSample` | form part | `MultipartFile` | no | Audio file of the participant's voice. Stored in Supabase Storage bucket `voice-samples`. Supported formats: `m4a`, `mp3`, `wav`, `ogg`, `flac`. |

**Responses**

| Status | Description | Body |
|---|---|---|
| `201 Created` | Participant registered | `ParticipantResponse` |
| `400 Bad Request` | Invalid request | `ErrorResponse` |

---

### GET /api/v1/participants

List all participants or retrieve a single one by ID.

**Query Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `UUID` | no | When provided, returns a single participant with their team memberships. When omitted, returns all participants. |

**Responses**

| Status | Description | Body |
|---|---|---|
| `200 OK` | Without `id`: list of all participants | `ParticipantResponse[]` |
| `200 OK` | With `id`: single participant with teams | `ParticipantResponse` |
| `404 Not Found` | `id` provided but participant not found | `ErrorResponse` |

> Note: when listing all participants, each item in the list already includes the `teams` array populated.

---

### PUT /api/v1/participants?id={id}

Update the name, role, and/or voice sample of an existing participant.
Only fields explicitly provided are updated. If `voiceSample` is provided, the existing sample is deleted and replaced.

**Query Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `UUID` | yes | Participant ID |

**Request**

- `Content-Type: multipart/form-data`

| Parameter | Location | Type | Required | Description |
|---|---|---|---|---|
| `name` | form field | `string` | no | New name |
| `role` | form field | `string` | no | New role |
| `voiceSample` | form part | `MultipartFile` | no | New voice sample. Replaces existing one if present. |

**Responses**

| Status | Description | Body |
|---|---|---|
| `200 OK` | Updated participant | `ParticipantResponse` |
| `404 Not Found` | Participant not found | `ErrorResponse` |

---

### PATCH /api/v1/participants?id={id}

Upload or replace only the voice sample of a participant.

**Query Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `UUID` | yes | Participant ID |

**Request**

- `Content-Type: multipart/form-data`

| Parameter | Location | Type | Required | Description |
|---|---|---|---|---|
| `voiceSample` | form part | `MultipartFile` | yes | Voice sample audio file. Replaces any existing sample. |

**Responses**

| Status | Description | Body |
|---|---|---|
| `200 OK` | Updated participant | `ParticipantResponse` |
| `400 Bad Request` | No file provided | `ErrorResponse` |
| `404 Not Found` | Participant not found | `ErrorResponse` |

---

### DELETE /api/v1/participants?id={id}

Delete a participant. If the participant has a voice sample stored in Supabase Storage, it is also deleted.

**Query Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `UUID` | yes | Participant ID |

**Responses**

| Status | Description | Body |
|---|---|---|
| `204 No Content` | Successfully deleted | — |
| `404 Not Found` | Participant not found | `ErrorResponse` |

---

### GET /api/v1/participants/voice-sample?id={id}

Download the voice sample audio file of a participant as a binary attachment.

**Query Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `UUID` | yes | Participant ID |

**Responses**

| Status | Description | Body |
|---|---|---|
| `200 OK` | Binary audio file. `Content-Disposition: attachment`. `Content-Type` resolved from file extension (`audio/x-m4a`, `audio/mpeg`, `audio/wav`, `audio/ogg`, `audio/flac`, or `application/octet-stream`). | `byte[]` |
| `404 Not Found` | Participant not found or no voice sample attached | `ErrorResponse` |

---

### ParticipantResponse structure

```json
{
  "id": "b2f3c1d4-89ab-4321-bcde-0123456789ab",
  "name": "Matheus",
  "role": "Engenheiro de Software",
  "voiceSampleKey": "b2f3c1d4-89ab-4321-bcde-0123456789ab.m4a",
  "createdAt": "2026-04-01T09:00:00-03:00",
  "teams": [
    {
      "id": "a3f1c2d4-89ab-4321-bcde-0123456789ab",
      "name": "Time de Produto"
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Participant ID |
| `name` | `string` | Full name |
| `role` | `string` | Job role |
| `voiceSampleKey` | `string \| null` | Storage key of the voice sample file. `null` if no sample was uploaded. |
| `createdAt` | `string (ISO-8601)` | Registration date |
| `teams` | `array` | Teams this participant belongs to. Empty array if none. |
| `teams[].id` | `UUID` | Team ID |
| `teams[].name` | `string` | Team name |

---

## Tag: Teams

Base path: `/api/v1/teams`

Teams group participants together. When a video is transcribed and associated with a team, the participants in that team provide the voice samples used for speaker identification.

---

### POST /api/v1/teams

Create a new team.

**Request**

- `Content-Type: application/json`

```json
{
  "name": "Time de Produto"
}
```

| Field | Type | Required | Validation | Description |
|---|---|---|---|---|
| `name` | `string` | yes | Not blank, max 255 chars | Team name |

**Responses**

| Status | Description | Body |
|---|---|---|
| `201 Created` | Team created | `TeamResponse` |
| `400 Bad Request` | Validation failed | `ErrorResponse` |

---

### GET /api/v1/teams

List all teams or find a single team by ID.

**Query Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `UUID` | no | When provided, returns a single team with its member list. When omitted, returns all teams. |

**Responses**

| Status | Description | Body |
|---|---|---|
| `200 OK` | Without `id`: list of all teams | `TeamResponse[]` |
| `200 OK` | With `id`: single team with members | `TeamResponse` |
| `404 Not Found` | `id` provided but team not found | `ErrorResponse` |

---

### DELETE /api/v1/teams?id={id}

Delete a team and cascade-remove all its members.

**Query Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `UUID` | yes | Team ID |

**Responses**

| Status | Description | Body |
|---|---|---|
| `204 No Content` | Team deleted | — |
| `404 Not Found` | Team not found | `ErrorResponse` |

---

### TeamResponse structure

```json
{
  "id": "a3f1c2d4-89ab-4321-bcde-0123456789ab",
  "name": "Time de Produto",
  "createdAt": "2026-03-15T08:00:00-03:00",
  "members": [
    {
      "teamId": "a3f1c2d4-89ab-4321-bcde-0123456789ab",
      "teamName": "Time de Produto",
      "participant": {
        "id": "b2f3c1d4-89ab-4321-bcde-0123456789ab",
        "name": "Matheus",
        "role": "Engenheiro de Software",
        "voiceSampleKey": "b2f3c1d4-...-0123456789ab.m4a",
        "createdAt": "2026-04-01T09:00:00-03:00",
        "teams": []
      },
      "addedAt": "2026-04-02T10:00:00-03:00"
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Team ID |
| `name` | `string` | Team name |
| `createdAt` | `string (ISO-8601)` | Creation date |
| `members` | `array` | List of team members. Populated when querying by ID; may be empty when listing all teams depending on implementation. |
| `members[].teamId` | `UUID` | Team ID (same as parent) |
| `members[].teamName` | `string` | Team name (same as parent) |
| `members[].participant` | `object` | Participant data (see `ParticipantResponse` — `teams` field is always empty inside this nested object) |
| `members[].addedAt` | `string (ISO-8601)` | When the participant was added to the team |

---

## Tag: Team Members

Base path: `/api/v1/team-members`

Manages the many-to-many relationship between participants and teams.

---

### POST /api/v1/team-members?teamId={teamId}

Add a participant to a team.

**Query Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `teamId` | `UUID` | yes | ID of the team |

**Request**

- `Content-Type: application/json`

```json
{
  "participantId": "b2f3c1d4-89ab-4321-bcde-0123456789ab"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `participantId` | `UUID` | yes | ID of the participant to add |

**Responses**

| Status | Description | Body |
|---|---|---|
| `201 Created` | Participant added | `TeamMemberResponse` |
| `400 Bad Request` | Invalid request or participant already in team | `ErrorResponse` |
| `404 Not Found` | Team or participant not found | `ErrorResponse` |

---

### GET /api/v1/team-members

List team members. Behavior varies by query parameters:

- No params: returns all members across all teams.
- `teamId` only: returns all members of that team.
- `teamId` + `participantId`: returns that specific membership entry.

**Query Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `teamId` | `UUID` | no | Filter by team |
| `participantId` | `UUID` | no | Filter by participant (only meaningful when combined with `teamId`) |

**Responses**

| Status | Description | Body |
|---|---|---|
| `200 OK` | Member list | `TeamMemberResponse[]` |
| `404 Not Found` | Specific member not found (when both params provided) | `ErrorResponse` |

---

### DELETE /api/v1/team-members?teamId={teamId}&participantId={participantId}

Remove a participant from a team.

**Query Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `teamId` | `UUID` | yes | Team ID |
| `participantId` | `UUID` | yes | Participant ID |

**Responses**

| Status | Description | Body |
|---|---|---|
| `204 No Content` | Member removed | — |
| `404 Not Found` | Membership not found | `ErrorResponse` |

---

### TeamMemberResponse structure

```json
{
  "teamId": "a3f1c2d4-89ab-4321-bcde-0123456789ab",
  "teamName": "Time de Produto",
  "participant": {
    "id": "b2f3c1d4-89ab-4321-bcde-0123456789ab",
    "name": "Matheus",
    "role": "Engenheiro de Software",
    "voiceSampleKey": "b2f3c1d4-...-0123456789ab.m4a",
    "createdAt": "2026-04-01T09:00:00-03:00",
    "teams": []
  },
  "addedAt": "2026-04-02T10:00:00-03:00"
}
```

| Field | Type | Description |
|---|---|---|
| `teamId` | `UUID` | Team ID |
| `teamName` | `string` | Team name |
| `participant` | `object` | Full participant data. The `teams` array inside this nested object is always empty. |
| `addedAt` | `string (ISO-8601)` | Date the participant was added to the team |

---

## Domain Concepts Summary

| Concept | Description |
|---|---|
| **Participant** | A person who attends meetings. Has a name, role, and optional voice sample stored in Supabase Storage. |
| **Team** | A group of participants associated with a meeting context. |
| **TeamMember** | The association between a Participant and a Team. |
| **TranscriptionJob** | An async job created when a video is submitted for transcription. Tracks status from `PROCESSING` to `DONE` or `FAILED`. The `result` field holds the full transcription JSON. |
| **VideoAsset** | A video file transcoded to 540p MP4 and stored in Supabase Storage. Created automatically when `teamId` is passed to the transcription endpoint, or manually via the `/persist` endpoint. |
| **Meeting** | Internal record persisted after a successful transcription. Contains the meeting title, duration, full text, and speaker segments. Not directly exposed via a CRUD endpoint in this version. |

---

## Typical Integration Flows

### Flow 1: Transcribe a meeting video and get the result

```
1. POST /api/v1/videos/transcribe
      form: file=<video.mp4>, teamId=<uuid>
   → 202 { jobId: "abc-..." }

2. GET /api/v1/videos/transcribe/abc-...
   → 200 { status: "PROCESSING" }

3. (poll until DONE)
   GET /api/v1/videos/transcribe/abc-...
   → 200 {
       status: "DONE",
       result: {
         transcription: "...",
         segments: [ { speaker: "Ana", start: 0.1, end: 2.5, text: "..." }, ... ]
       }
     }
```

### Flow 2: Register participants and associate with a team

```
1. POST /api/v1/teams
      body: { "name": "Time de Engenharia" }
   → 201 { id: "team-uuid", ... }

2. POST /api/v1/participants
      form: name="Ana", role="Engenheira", voiceSample=<ana.m4a>
   → 201 { id: "participant-uuid-ana", ... }

3. POST /api/v1/team-members?teamId=team-uuid
      body: { "participantId": "participant-uuid-ana" }
   → 201 { teamId: "...", participant: { name: "Ana", ... }, ... }
```

### Flow 3: Manually upload a video for storage

```
1. POST /api/v1/videos/by-team/{teamId}/persist
      form: file=<video.mp4>
   → 202 { id: "asset-uuid", status: "UPLOADED", ... }

2. GET /api/v1/videos/by-team/{teamId}
   → 200 [ { status: "PROCESSING", ... }, ... ]

3. (poll until READY)
   GET /api/v1/videos/by-team/{teamId}
   → 200 [ { status: "READY", playbackUrl: "https://...", ... } ]
```

---

## Notes for AI Agents

- **No authentication is enforced at the HTTP layer** in the current version. All endpoints are publicly accessible when the service is reachable.
- The `result` field in `TranscriptionJobResponse` is serialized as a raw JSON value (not a quoted string). Treat it as an embedded JSON object, not a string.
- The `voiceSampleKey` in `ParticipantResponse` is a Supabase Storage object key, not a direct URL. To download the audio, use `GET /api/v1/participants/voice-sample?id={participantId}`.
- `VideoAssetStatus` transitions are one-directional: `UPLOADED → PROCESSING → READY | FAILED`. A `PROCESSING` asset that exceeds the configured timeout (`VIDEO_PERSIST_PROCESS_TIMEOUT`, default 1800 s) will be retried on the next manual trigger.
- When `VIDEO_PERSIST_ENABLED=false` (server-side env var), the `/persist` endpoint returns `503` and video assets are not created during transcription either.
- Speaker names in segments are resolved by a separate webhook. If resolution fails or no mappings are returned, raw codes like `speaker_0`, `speaker_1` are preserved in the segment output.
- All multipart endpoints that accept a `voiceSample` or video `file` use Spring's `MultipartFile`. Send them as `multipart/form-data` with the correct `Content-Type` per part.
- Duplicate transcription detection is based on a file hash. Submitting the same file twice returns the existing job unless `force=true` is passed.
