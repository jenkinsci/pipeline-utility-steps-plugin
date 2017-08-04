package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import org.jenkinsci.plugins.workflow.steps.Step;

import java.util.Map;

public abstract class AbstractPropertiesStep extends Step {
    public abstract String getFile();

    public abstract void setFile(String file);

    public abstract String getText();

    public abstract void setText(String text);

    public abstract Map<String,Object> getDefaults();

    public abstract void setDefaults(Map<String, Object> defaults);
}
