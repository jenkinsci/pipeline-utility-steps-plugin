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

package org.jenkinsci.plugins.workflow.utility.steps.zip;

import hudson.Extension;
import hudson.model.Descriptor;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Unzips a zip file.
 * Can also be used to test a zip file.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class UnZipStep extends AbstractStepImpl {
    private final String zipFile;
    private String dir;
    private String glob;
    private boolean test = false;
    private boolean read = false;

    @DataBoundConstructor
    public UnZipStep(String zipFile) throws Descriptor.FormException {
        if (StringUtils.isBlank(zipFile)) {
            throw new Descriptor.FormException("Can not be empty", "zipFile");
        }
        this.zipFile = zipFile;
    }

    public String getZipFile() {
        return zipFile;
    }

    public String getDir() {
        return dir;
    }

    @DataBoundSetter
    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getGlob() {
        return glob;
    }

    @DataBoundSetter
    public void setGlob(String glob) {
        this.glob = glob;
    }

    public boolean isTest() {
        return test;
    }

    @DataBoundSetter
    public void setTest(boolean test) {
        this.test = test;
    }

    public boolean isRead() {
        return read;
    }

    @DataBoundSetter
    public void setRead(boolean read) {
        this.read = read;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(UnZipStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "unzip";
        }

        @Override
        public String getDisplayName() {
            return "Extract Zip file";
        }
    }
}
