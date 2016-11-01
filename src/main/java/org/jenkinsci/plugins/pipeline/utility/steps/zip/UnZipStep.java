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

   private String charset;
    private boolean test = false;
    private boolean read = false;

    @DataBoundConstructor
    public UnZipStep(String zipFile) throws Descriptor.FormException {
        if (StringUtils.isBlank(zipFile)) {
            throw new Descriptor.FormException("Can not be empty", "zipFile");
        }
        this.zipFile = zipFile;
    }

    /**
     * The name/path of the zip file to extract.
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
     * of files to extract from the zip.
     * Leave empty to include all files and directories.
     *
     * @return the include pattern
     */
    public String getGlob() {
        return glob;
    }

    /**
     * <a href="https://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant style pattern</a>
     * of files to extract from the zip.
     * Leave empty to include all files and directories.
     *
     * @param glob the include pattern
     */
    @DataBoundSetter
    public void setGlob(String glob) {
        this.glob = glob;
    }

    /**
     * Test the integrity of the archive instead of extracting it.
     * When this parameter is enabled, all other parameters <em>(except for {@link #getZipFile()})</em> will be ignored.
     * The step will return <code>true</code> or <code>false</code> depending on the result
     * instead of throwing an exception.
     *
     * @return if the archive should just be tested or not
     */
    public boolean isTest() {
        return test;
    }

    /**
     * Test the integrity of the archive instead of extracting it.
     * When this parameter is enabled, all other parameters <em>(except for {@link #getZipFile()})</em> will be ignored.
     * The step will return <code>true</code> or <code>false</code> depending on the result
     * instead of throwing an exception.
     *
     * @param test if the archive should just be tested or not
     */
    @DataBoundSetter
    public void setTest(boolean test) {
        this.test = test;
    }

    /**
     * Read the content of the files into a String instead of writing them to the workspace.
     * <em>E.g.</em>
     * <code>String version = unzip zipFile: 'example.zip', glob: 'version.txt', read: true</code>
     *
     * @return if the content should be read to a string instead of written to the workspace
     */
    public boolean isRead() {
        return read;
    }

    /**
     * Read the content of the files into a String instead of writing them to the workspace.
     * <em>E.g.</em>
     * <code>String version = unzip zipFile: 'example.zip', glob: 'version.txt', read: true</code>
     *
     * @param read if the content should be read to a string instead of written to the workspace
     */
    @DataBoundSetter
    public void setRead(boolean read) {
        this.read = read;
    }

   /**
    * Get the charset to use when unzipping the zip file. <em>E.g. UTF-8</em>
    * 
    * <code>String version = unzip zipFile: 'example.zip', glob: 'version.txt', read: true, charset: UTF-8</code>
    *
    * @return String specifying the charset, defaults to UTF-8
    */
   public String getCharset()
   {
      return (charset != null) ? charset : "UTF-8";

   }

   /**
    * Set the charset to use when unzipping the zip file.
    * 
    * <code>String version = unzip zipFile: 'example.zip', glob: 'version.txt', read: true , charset: UTF-8</code>
    *
    * @param charset
    *           the charset to use when unzipping, defaults to UTF-8
    */
   @DataBoundSetter
   public void setCharset(String charset)
   {
      this.charset = (charset.trim().isEmpty()) ? "UTF-8" : charset;
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
