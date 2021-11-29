package org.jenkinsci.plugins.pipeline.utility.steps;

import org.kohsuke.stapler.DataBoundSetter;

public abstract class AbstractFileOrTextStep extends AbstractFileStep {
    protected String text;

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
