/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package com.warmthdawn.kubejsdebugadapter.api;

/**
 * This interface exposes debugging information from executable code (either functions or top-level
 * scripts).
 */
public interface DebuggableScript {
    boolean isTopLevel();

    /**
     * Returns true if this is a function, false if it is a script.
     */
    boolean isFunction();

    /**
     * Get name of the function described by this script. Return null or an empty string if this
     * script is not a function.
     */
    String getFunctionName();

    /**
     * Get number of declared parameters in the function. Return 0 if this script is not a function.
     *
     * @see #getParamAndVarCount()
     * @see #getParamOrVarName(int index)
     */
    int getParamCount();

    /**
     * Get number of declared parameters and local variables. Return number of declared global
     * variables if this script is not a function.
     *
     * @see #getParamCount()
     * @see #getParamOrVarName(int index)
     */
    int getParamAndVarCount();

    /**
     * Get name of a declared parameter or local variable. <code>index</code> should be less then
     * {@link #getParamAndVarCount()}. If <code>index&nbsp;&lt;&nbsp;{@link #getParamCount()}</code>
     * , return the name of the corresponding parameter, otherwise return the name of variable. If
     * this script is not function, return the name of the declared global variable.
     */
    String getParamOrVarName(int index);

    /**
     * Get the name of the source (usually filename or URL) of the script.
     */
    String getSourceName();

    int getFunctionCount();

    DebuggableScript getFunction(int index);

    DebuggableScript getParent();

    int getFunctionScriptId();
}
