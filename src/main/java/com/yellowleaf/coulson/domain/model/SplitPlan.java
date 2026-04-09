package com.yellowleaf.coulson.domain.model;

import java.util.Collections;
import java.util.List;

public class SplitPlan {

    private final List<SplitSegment> segments;
    private final boolean requiresSplitting;

    private SplitPlan(List<SplitSegment> segments, boolean requiresSplitting) {
        this.segments = Collections.unmodifiableList(segments);
        this.requiresSplitting = requiresSplitting;
    }

    public static SplitPlan noSplit() {
        return new SplitPlan(Collections.emptyList(), false);
    }

    public static SplitPlan withSegments(List<SplitSegment> segments) {
        return new SplitPlan(segments, true);
    }

    public List<SplitSegment> getSegments() {
        return segments;
    }

    public boolean requiresSplitting() {
        return requiresSplitting;
    }
}
