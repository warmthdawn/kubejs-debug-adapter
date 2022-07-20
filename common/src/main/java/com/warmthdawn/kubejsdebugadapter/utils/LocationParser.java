package com.warmthdawn.kubejsdebugadapter.utils;

import com.warmthdawn.kubejsdebugadapter.data.ScriptLocation;
import dev.latvian.mods.rhino.ScriptRuntime;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;
import java.util.Objects;

public class LocationParser {
    private int[] lineStartIndexes;

    private String sourceName;

    public LocationParser(int[] lineStartIndexes, String sourceName) {
        this.lineStartIndexes = lineStartIndexes;
        this.sourceName = sourceName;
    }

    public static LocationParser resolve(String sourceName, String sourceString) {
        IntList lineStartIndexes = new IntArrayList();
        for(int i = 0; i < sourceString.length() - 1; i++) {
            char ch = sourceString.charAt(i);

            if(ch == '\n') {
                lineStartIndexes.add(i + 1);
            }
        }
        return new LocationParser(lineStartIndexes.toIntArray(), sourceName);

    }

    public ScriptLocation toLocation(int index) {
        int lineIndex = binarySearchIndexLessThan(index);

        int columnIndex = index - lineStartIndexes[lineIndex];

        return new ScriptLocation(lineIndex, columnIndex, sourceName);
    }

    private int binarySearchIndexLessThan(int index) {
        int low = 0;
        int high = lineStartIndexes.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = lineStartIndexes[mid];
            if (midVal < index) {
                low = mid + 1;
            } else if (midVal > index) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return low - 1;
    }


    public int toIndex(ScriptLocation location) {
        if(!Objects.equals(location.getSourceName(), sourceName)) {
            return -1;
        }
        int lineIndex = location.getLineNumber();
        int columnIndex = location.getColumnNumber();

        return lineStartIndexes[lineIndex] + columnIndex;
    }
}
