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
 * Creates a zip file.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ZipStep extends AbstractStepImpl {
    private final String zipFile;
    private String dir;
    private String glob;
    private boolean archive = false;

    @DataBoundConstructor
    public ZipStep(String zipFile) throws Descriptor.FormException {
        if (StringUtils.isBlank(zipFile)) {
            throw new Descriptor.FormException("Can not be empty", "zipFile");
        }
        this.zipFile = zipFile;
    }

    /**
     * The name/path of the zip file to create.
     *
     * @return the path
     */
    public String getZipFile() {
        return zipFile;
    }

    /**
     * The relative path of the base directory to create the zip from.
     * Leave empty to create from the current working directory.
     *
     * @return the dir
     */
    public String getDir() {
        return dir;
    }

    /**
     * The relative path of the base directory to create the zip from.
     * Leave empty to create from the current working directory.
     *
     * @param dir the dir
     */
    @DataBoundSetter
    public void setDir(String dir) {
        this.dir = dir;
    }

    /**
     * <a href="https://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant style pattern</a>
     * of files to include in the zip.
     * Leave empty to include all files and directories.
     *
     * @return the include pattern
     */
    public String getGlob() {
        return glob;
    }

    /**
     * <a href="https://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant style pattern</a>
     * of files to include in the zip.
     * Leave empty to include all files and directories.
     *
     * @param glob the include pattern
     */
    @DataBoundSetter
    public void setGlob(String glob) {
        this.glob = glob;
    }

    /**
     * If the zip file should be archived as an artifact of the current build.
     * The file will still be kept in the workspace after archiving.
     *
     * @return if it should be archived or not
     */
    public boolean isArchive() {
        return archive;
    }

    /**
     * If the zip file should be archived as an artifact of the current build.
     * The file will still be kept in the workspace after archiving.
     *
     * @param archive if it should be archived or not
     */
    @DataBoundSetter
    public void setArchive(boolean archive) {
        this.archive = archive;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ZipStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "zip";
        }

        @Override
        public String getDisplayName() {
            return "Create Zip file";
        }
    }
}
