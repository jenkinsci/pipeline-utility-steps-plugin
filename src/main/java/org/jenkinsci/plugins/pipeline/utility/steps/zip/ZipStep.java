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
package org.jenkinsci.plugins.pipeline.utility.steps.zip;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.Set;

/**
 * Creates a zip file.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ZipStep extends Step {
    private final String zipFile;
    private String dir;
    private String glob;
    private String exclude;
    private boolean archive = false;
    private boolean overwrite = false;

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
     * <a href="https://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant style pattern</a>
     * of files to exclude from the zip.
     *
     * @return the exclude pattern
     */
    public String getExclude() {
        return exclude;
    }

    /**
     * <a href="https://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant style pattern</a>
     * of files to exclude in the zip.
     *
     * @param exclude the exclude pattern
     */
    @DataBoundSetter
    public void setExclude(String exclude) {
        this.exclude = exclude;
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

    /**
     * If the zip file should be overwritten in case of already existing a file with the same name.
     *
     * @return if the file should be overwritten or not in case of existing.
     */
    public boolean isOverwrite() {
        return overwrite;
    }

    /**
     * If the zip file should be overwritten in case of already existing a file with the same name.
     *
     * @param overwrite if the file should be overwritten or not in case of existing.
     */
    @DataBoundSetter
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }


    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ZipStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        public DescriptorImpl() {

        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, FilePath.class);
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
