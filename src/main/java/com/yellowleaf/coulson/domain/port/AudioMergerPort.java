package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.AudioChunk;

import java.util.List;

public interface AudioMergerPort {

    AudioChunk merge(String videoId, List<AudioChunk> chunks);
}
