package org.jenkinsci.plugins.pipeline.utility.steps;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundSetter;

public abstract class AbstractFileOrTextStep extends Step {
    protected String file;
    protected String text;

    /**
     * The path to a file in the workspace to read from.
     *
     * @return the path
     */
    public String getFile() {
        return file;
    }

    /**
     * The path to a file in the workspace to read from.
     *
     * @param file the path
     */
    @DataBoundSetter
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * A String containing the formatted data.
     *
     * @return text to parse
     */
    public String getText() {
        return text;
    }

    /**
     * A String containing the formatted data.
     *
     * @param text text to parse
     */
    @DataBoundSetter
    public void setText(String text) {
        this.text = text;
    }

}
