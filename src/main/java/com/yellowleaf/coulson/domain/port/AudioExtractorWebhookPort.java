package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.AudioChunk;
import com.yellowleaf.coulson.domain.model.VideoChunk;

public interface AudioExtractorWebhookPort {

    AudioChunk extract(VideoChunk videoChunk);
}
