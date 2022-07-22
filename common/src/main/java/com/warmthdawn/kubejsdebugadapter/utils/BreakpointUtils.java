package com.warmthdawn.kubejsdebugadapter.utils;

import com.ibm.icu.impl.Pair;
import com.warmthdawn.kubejsdebugadapter.data.ScriptLocation;
import com.warmthdawn.kubejsdebugadapter.data.UserDefinedBreakpoint;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.BreakpointMeta;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.FunctionSourceData;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptSourceData;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.StatementBreakpointMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class BreakpointUtils {

    public static List<Pair<ScriptLocation, ScriptLocation>> collectBreakpoints(ScriptSourceData data) {

        List<Pair<ScriptLocation, ScriptLocation>> locationList = new ArrayList<>();
        LocationParser locationParser = data.getLocationParser();

        for (FunctionSourceData function : data.getFunctions()) {
            if(function.isHasBlock()) {
                // 方法断点
                int lcStart = function.getLcStart();
                locationList.add(
                    Pair.of(
                        locationParser.toLocation(lcStart),
                        locationParser.toLocation(lcStart + 1)
                    ));
                int rcEnd = function.getRcEnd();
                locationList.add(
                    Pair.of(
                        locationParser.toLocation(rcEnd),
                        locationParser.toLocation(rcEnd + 1)
                    ));
            }

            for (StatementBreakpointMeta meta : function.getStatementBreakpointMetas()) {
                if (meta.shouldBreakHere()) {
                    int position = meta.getPosition();
                    int length = meta.getLength();
                    locationList.add(
                        Pair.of(
                            locationParser.toLocation(position),
                            locationParser.toLocation(position + length)
                        ));
                }
            }

            for (BreakpointMeta meta : function.getExpressionBreakpointMetas()) {
                int position = meta.getPosition();
                int length = meta.getLength();
                locationList.add(
                    Pair.of(
                        locationParser.toLocation(position),
                        locationParser.toLocation(position + length)
                    ));
            }


        }

        locationList.sort(
            Comparator.comparingInt((Pair<ScriptLocation, ScriptLocation> o) -> o.first.getLineNumber()).
                thenComparingInt(o -> o.first.getColumnNumber())
        );
        return locationList;

    }



    public static ScriptLocation binarySearchClosestLocation(List<Pair<ScriptLocation, ScriptLocation>> sortedLocations, int line, int column) {
        int low = 0;
        int high = sortedLocations.size() - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            Pair<ScriptLocation, ScriptLocation> midVal = sortedLocations.get(mid);
            int midLine = midVal.first.getLineNumber();
            int midColumn = midVal.first.getColumnNumber();
            if (midLine < line || (midLine == line && midColumn <= column)) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        if (low >= sortedLocations.size()) {
            return null;
        }
        return sortedLocations.get(low).first;
    }

    public static void coerceBreakpoints(List<Pair<ScriptLocation, ScriptLocation>> sortedLocations,
                                         List<UserDefinedBreakpoint> raw,
                                         List<UserDefinedBreakpoint> changed,
                                         List<Integer> toRemove) {

        Iterator<UserDefinedBreakpoint> iterator = raw.iterator();
        while (iterator.hasNext()) {
            UserDefinedBreakpoint breakpoint = iterator.next();
            int line = breakpoint.getLine();
            int column = breakpoint.getColumn();
            ScriptLocation location = binarySearchClosestLocation(sortedLocations, line, column);

            if (location == null) {
                toRemove.add(breakpoint.getId());
                iterator.remove();
                continue;
            }

            if (location.getLineNumber() == line && location.getColumnNumber() == column) {
                continue;
            }

            breakpoint.setLine(location.getLineNumber());
            breakpoint.setColumn(location.getColumnNumber());
            changed.add(breakpoint);

        }
    }
}
