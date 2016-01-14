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

package org.jenkinsci.plugins.workflow.utility.steps.fs;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * List files in/under current working directory.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class FindFilesStep extends AbstractStepImpl {
    private String glob;

    @DataBoundConstructor
    public FindFilesStep() {
    }

    /**
     * Pattern of files to include in the list.
     * When omitting this only direct descendants of cwd will be listed
     * otherwise all descendants can potentially be listed.
     *
     * @return the search pattern
     */
    public String getGlob() {
        return glob;
    }

    /**
     * Pattern of files to include in the list.
     * When omitting this only direct descendants of cwd will be listed
     * otherwise all descendants can potentially be listed.
     *
     * @param glob the search pattern
     */
    @DataBoundSetter
    public void setGlob(String glob) {
        this.glob = glob;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(FindFilesStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "findFiles";
        }

        @Override
        public String getDisplayName() {
            return "Find files in the workspace";
        }
    }
}
