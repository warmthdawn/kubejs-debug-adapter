package com.warmthdawn.kubejsdebugadapter.utils;

import com.ibm.icu.impl.Pair;
import com.warmthdawn.kubejsdebugadapter.data.ScriptLocation;
import com.warmthdawn.kubejsdebugadapter.data.UserDefinedBreakpoint;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.BreakpointMeta;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.FunctionSourceData;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptSourceData;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.StatementBreakpointMeta;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.*;
import java.util.function.IntFunction;

public class BreakpointUtils {

    public static List<Pair<ScriptLocation, ScriptLocation>> collectBreakpoints(ScriptSourceData data) {

        List<Pair<ScriptLocation, ScriptLocation>> locationList = new ArrayList<>();
        LocationParser locationParser = data.getLocationParser();

        for (FunctionSourceData function : data.getFunctions()) {
            if (function.isHasBlock()) {
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

    // 查找某一行的第一个断点
    // 如果该行没有，寻找下一个行的第一个断点
    public static int binarySearchLocation(int length, IntFunction<ScriptLocation> locFunc, int line) {
        int left = 0;
        int right = length;
        while (left < right) {
            int mid = (left + right) / 2;

            ScriptLocation midLoc = locFunc.apply(mid);
            int testLine = midLoc.getLineNumber();

            if (testLine < line) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        ScriptLocation result = locFunc.apply(left);
        int resultLine = result.getLineNumber();
        if (resultLine == line) {
            return left;
        }
        // 后面没有拉！
        if (left + 1 >= length) {
            return -1;
        }
        return left + 1;
    }

    // 查找坐标
    // 如果没有这个坐标，则查找距离目标最近的位置
    public static int binarySearchLocation(int length, IntFunction<ScriptLocation> locFunc, int line, int column) {
        int left = 0;
        int right = length;

        while (left < right) {
            int mid = (left + right) / 2;

            ScriptLocation midLoc = locFunc.apply(mid);
            int testLine = midLoc.getLineNumber();
            int testColumn = midLoc.getColumnNumber();

            if (testLine < line) {
                left = mid + 1;
            } else if (testLine > line) {
                right = mid;
            } else {
                if (testColumn < column) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }
        }

        if (left >= length) {
            return -1;
        }

        ScriptLocation resultLoc = locFunc.apply(left);
        int resultLine = resultLoc.getLineNumber();
        int resultColumn = resultLoc.getColumnNumber();

        // 就是要找的
        if (resultLine == line && resultColumn == column) {
            return left;
        }

        // 结果在上一行
        if (resultLine < line) {
            return left + 1 >= length ? -1 : left + 1;
        }

        // 在同一行
        if (resultLine == line) {
            // 最后一个了，直接返回
            if (left == length - 1) {
                return left;
            }
            ScriptLocation next = locFunc.apply(left + 1);
            // 下一个位置不在同一行了，直接返回
            if (next.getLineNumber() != line) {
                return left;
            }

            int diffLeft = column - resultColumn;
            int diffRight = next.getColumnNumber() - column;

            // 返回列号更接近的
            if (diffLeft < diffRight) {
                return left;
            } else {
                return left + 1;
            }

        }

        // 结果在下一行，直接返回
        return left;

    }

    // 查找某个范围内的所有的点
    // 范围全都是闭合区间
    public static IntIntPair binarySearchLocationsBetween(int length, IntFunction<ScriptLocation> locFunc, int lineStart, int lineEnd, int columnStart, int columnEnd) {

        // 二分查找起始行
        int left = 0;
        int right = length;
        while (left < right) {
            int mid = (left + right) / 2;
            ScriptLocation midLoc = locFunc.apply(mid);
            int testLine = midLoc.getLineNumber();
            int testColumn = midLoc.getColumnNumber();

            if (testLine < lineStart) {
                left = mid + 1;
            } else if (testLine > lineStart) {
                right = mid;
            } else {
                if (testColumn < columnStart) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }
        }

        int start = left;

        // 二分查找结束行

        right = length;
        while (left < right) {
            int mid = (left + right) / 2;

            ScriptLocation midLoc = locFunc.apply(mid);
            int testLine = midLoc.getLineNumber();
            int testColumn = midLoc.getColumnNumber();

            if (testLine < lineEnd) {
                left = mid + 1;
            } else if (testLine > lineEnd) {
                right = mid;
            } else {
                if (testColumn <= columnEnd || columnEnd < 0) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }
        }

        int end = left;

        // 找到了
        return IntIntPair.of(start, end);
    }


    public static Pair<ScriptLocation, ScriptLocation> binarySearchLocation(List<Pair<ScriptLocation, ScriptLocation>> sortedLocations, int line, int column) {
        int index;
        if (column < 0) {
            index = binarySearchLocation(sortedLocations.size(), (i) -> sortedLocations.get(i).first, line);
        } else {
            index = binarySearchLocation(sortedLocations.size(), (i) -> sortedLocations.get(i).first, line, column);
        }
        return index == -1 ? null : sortedLocations.get(index);
    }

    public static List<UserDefinedBreakpoint> coerceBreakpoints(List<Pair<ScriptLocation, ScriptLocation>> sortedLocations,
                                                                List<UserDefinedBreakpoint> raw,
                                                                Collection<UserDefinedBreakpoint> changed,
                                                                IntSet toRemove) {

        Set<ScriptLocation> presentLocation = new HashSet<>();
        List<UserDefinedBreakpoint> result = new ArrayList<>();
        for (UserDefinedBreakpoint breakpoint : raw) {
            int line = breakpoint.getLine();
            int column = breakpoint.getColumn();
            Pair<ScriptLocation, ScriptLocation> pair = binarySearchLocation(sortedLocations, line, column);
            if (pair == null) {
                toRemove.add(breakpoint.getId());
                continue;
            }
            ScriptLocation location = pair.first;

            if (presentLocation.contains(location)) {
                toRemove.add(breakpoint.getId());
                continue;
            }
            presentLocation.add(location);
            result.add(breakpoint);

            if (location.getLineNumber() == line && location.getColumnNumber() == column) {
                continue;
            }

            breakpoint.setLine(location.getLineNumber());
            breakpoint.setColumn(location.getColumnNumber());
            changed.add(breakpoint);

        }
        return result;
    }
}
