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

package org.jenkinsci.plugins.pipeline.utility.steps.conf.mf;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Reads a Jar Manifest.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ReadManifestStep extends AbstractStepImpl {
    private String file;
    private String text;

    /**
     * Since the user could either use {@link #setFile(String)} or {@link #setText(String)}
     * this constructor takes no parameters.
     */
    @DataBoundConstructor
    public ReadManifestStep() {
    }

    /**
     * @return path
     * @see #setFile(String)
     */
    public String getFile() {
        return file;
    }

    /**
     * Path to a file containing a Jar manifest.
     * Can be a plain text file, a .jar, .war or .ear
     *
     * @param file path to the file to read
     */
    @DataBoundSetter
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * @return manifest content
     * @see #setText(String)
     */
    public String getText() {
        return text;
    }

    /**
     * Parse text as text read from a manifest file.
     *
     * @param text manifest content
     */
    @DataBoundSetter
    public void setText(String text) {
        this.text = text;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ReadManifestStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "readManifest";
        }

        @Override
        public String getDisplayName() {
            return "Read a Jar Manifest";
        }
    }
}
