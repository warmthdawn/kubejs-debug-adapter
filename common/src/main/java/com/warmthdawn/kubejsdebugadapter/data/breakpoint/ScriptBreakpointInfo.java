package com.warmthdawn.kubejsdebugadapter.data.breakpoint;

import com.warmthdawn.kubejsdebugadapter.utils.LocationParser;

public class ScriptBreakpointInfo {
    private final String origin;
    private final ScriptLocation location;
    private final ScriptLocation end;
    private final boolean lowPriority;

    public ScriptBreakpointInfo(String origin, ScriptLocation location, ScriptLocation end, boolean lowPriority) {
        this.origin = origin;
        this.location = location;
        this.end = end;
        this.lowPriority = lowPriority;
    }

    public static ScriptBreakpointInfo create(String origin, LocationParser parser, int begin, int length, boolean lowPriority) {
        return new ScriptBreakpointInfo(origin, parser.toLocation(begin), parser.toLocation(begin + length), lowPriority);
    }
    public static ScriptBreakpointInfo create(String origin, LocationParser parser, int begin, int length) {
        return new ScriptBreakpointInfo(origin, parser.toLocation(begin), parser.toLocation(begin + length), false);
    }

    public String getOrigin() {
        return origin;
    }

    public ScriptLocation getLocation() {
        return location;
    }

    public ScriptLocation getEnd() {
        return end;
    }

    public boolean isLowPriority() {
        return lowPriority;
    }
}
