package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.SpeakerMapping;
import com.yellowleaf.coulson.domain.model.TranscriptionSegment;

import java.util.List;

public interface SpeakerResolutionPort {

    List<SpeakerMapping> resolve(List<TranscriptionSegment> segments);
}
