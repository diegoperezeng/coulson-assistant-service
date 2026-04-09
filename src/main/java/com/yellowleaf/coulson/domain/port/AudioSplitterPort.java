package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.AudioChunk;

import java.util.List;

public interface AudioSplitterPort {

    List<AudioChunk> split(AudioChunk audio, double maxDurationSeconds);
}
