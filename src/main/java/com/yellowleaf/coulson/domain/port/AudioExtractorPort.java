package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.AudioChunk;
import com.yellowleaf.coulson.domain.model.VideoChunk;

public interface AudioExtractorPort {

    AudioChunk extract(VideoChunk videoChunk);
}
