/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 CloudBees Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jenkinsci.plugins.pipeline.utility.steps;

import hudson.FilePath;
import jenkins.MasterToSlaveFileCallable;

import java.io.File;
import java.io.IOException;

public abstract class AbstractFileCallable<T> extends MasterToSlaveFileCallable<T> {
    private FilePath destination;
    private boolean allowExtractionOutsideDestination = false;

    public FilePath getDestination() {
        return destination;
    }

    public void setDestination(FilePath destination) {
        this.destination = destination;
    }

    /**
     * SECURITY-2169 escape hatch.
     * Controlled by {@link DecompressStepExecution#ALLOW_EXTRACTION_OUTSIDE_DESTINATION}.
     *
     * @return true if so.
     */
    public boolean isAllowExtractionOutsideDestination() {
        return allowExtractionOutsideDestination;
    }

    public void setAllowExtractionOutsideDestination(boolean allowExtractionOutsideDestination) {
        this.allowExtractionOutsideDestination = allowExtractionOutsideDestination;
    }

    protected boolean isDescendantOfDestination(FilePath f) throws IOException {
        if (allowExtractionOutsideDestination) {
            return true;
        }
        //Assumes destination and f is on the local host
        if (destination == null) {
            return false;
        }
        File dst = new File(destination.getRemote()).getCanonicalFile();
        File child = new File(f.getRemote()).getCanonicalFile();
        return child.toPath().startsWith(dst.toPath());
    }
}