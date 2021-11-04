package org.jenkinsci.plugins.pipeline.utility.steps;

import org.kohsuke.stapler.DataBoundSetter;

public abstract class AbstractFileDeCompressStep extends AbstractFileStep {
    private String dir;
    private String glob;
    private boolean test = false;
    private boolean quiet = false;

    /**
     * The relative path of the base directory to create the archive from.
     * Leave empty to create from the current working directory.
     *
     * @return the dir
     */
    public String getDir() {
        return dir;
    }

    /**
     * The relative path of the base directory to create the archive from.
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
     * of files to extract from the archive.
     * Leave empty to include all files and directories.
     *
     * @return the include pattern
     */
    public String getGlob() {
        return glob;
    }

    /**
     * <a href="https://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant style pattern</a>
     * of files to extract from the archive.
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
     * When this parameter is enabled, all other parameters <em>(except for {@link #getFile()})</em> will be ignored.
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
     * When this parameter is enabled, all other parameters <em>(except for {@link #getFile()})</em> will be ignored.
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
     * Suppress the verbose output that logs every single file that is dealt with.
     *
     * @return if verbose logging should be suppressed
     */
    public boolean isQuiet() {
        return quiet;
    }

    /**
     * Suppress the verbose output that logs every single file that is dealt with.
     *
     * @param quiet if verbose logging should be suppressed
     */
    @DataBoundSetter
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

}
