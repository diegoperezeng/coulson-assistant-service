package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.SpeakerMapping;
import com.yellowleaf.coulson.domain.model.TranscriptionSegment;

import java.util.List;

public interface SpeakerResolutionWebhookPort {

    List<SpeakerMapping> resolve(List<TranscriptionSegment> segments);
}
