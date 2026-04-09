package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.Video;
import com.yellowleaf.coulson.domain.model.VideoChunk;

import java.util.List;

public interface VideoSplitterPort {

    List<VideoChunk> split(Video video);
}
