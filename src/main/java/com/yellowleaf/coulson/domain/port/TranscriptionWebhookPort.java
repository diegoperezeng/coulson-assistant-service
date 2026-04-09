package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.AudioChunk;
import com.yellowleaf.coulson.domain.model.Transcription;

public interface TranscriptionWebhookPort {

    Transcription transcribe(AudioChunk audioChunk);
}
