package org.jenkinsci.plugins.pipeline.utility.steps;

import org.kohsuke.stapler.DataBoundSetter;

public abstract class AbstractFileCompressStep extends AbstractFileStep {
    private String dir;
    private String glob;
    private String exclude;
    private boolean archive = false;
    private boolean overwrite = false;
    private boolean defaultExcludes = true;

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
     * of files to include in the archive.
     * Leave empty to include all files and directories.
     *
     * @return the include pattern
     */
    public String getGlob() {
        return glob;
    }

    /**
     * <a href="https://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant style pattern</a>
     * of files to include in the archive.
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
     * of files to exclude from the archive.
     *
     * @return the exclude pattern
     */
    public String getExclude() {
        return exclude;
    }

    /**
     * <a href="https://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant style pattern</a>
     * of files to exclude in the archive.
     *
     * @param exclude the exclude pattern
     */
    @DataBoundSetter
    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    /**
     * If the archive file should be archived as an artifact of the current build.
     * The file will still be kept in the workspace after archiving.
     *
     * @return if it should be archived or not
     */
    public boolean isArchive() {
        return archive;
    }

    /**
     * If the archive file should be archived as an artifact of the current build.
     * The file will still be kept in the workspace after archiving.
     *
     * @param archive if it should be archived or not
     */
    @DataBoundSetter
    public void setArchive(boolean archive) {
        this.archive = archive;
    }

    /**
     * If the archive file should be overwritten in case of already existing a file with the same name.
     *
     * @return if the file should be overwritten or not in case of existing.
     */
    public boolean isOverwrite() {
        return overwrite;
    }

    /**
     * If the archive file should be overwritten in case of already existing a file with the same name.
     *
     * @param overwrite if the file should be overwritten or not in case of existing.
     */
    @DataBoundSetter
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * <a href="https://ant.apache.org/manual/dirtasks.html#defaultexcludes" target="_blank">Ant style pattern</a>
     * of files to disable default excludes from the archive.
     *
     * @return the defaultExcludes boolean
     */
    public boolean isDefaultExcludes() {
        return defaultExcludes;
    }

    /**
     * <a href="https://ant.apache.org/manual/dirtasks.html#defaultexcludes" target="_blank">Ant style pattern</a>
     * of files to disable default excludes from the archive.
     *
     * @return the defaultExcludes boolean
     */
    @DataBoundSetter
    public void setDefaultExcludes(boolean defaultExcludes) {
        this.defaultExcludes = defaultExcludes;
    }

}
