package org.jenkinsci.plugins.pipeline.utility.steps;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundSetter;

public abstract class AbstractFileStep extends Step {
    private String file;

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

}
