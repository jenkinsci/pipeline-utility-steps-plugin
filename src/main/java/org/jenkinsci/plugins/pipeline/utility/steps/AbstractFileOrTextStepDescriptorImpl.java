package org.jenkinsci.plugins.pipeline.utility.steps;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isBlank;

public abstract class AbstractFileOrTextStepDescriptorImpl extends StepDescriptor {

    protected AbstractFileOrTextStepDescriptorImpl() {
    }

    @Override
    public Set<? extends Class<?>> getRequiredContext() {
        return Collections.singleton(TaskListener.class);
    }

    @Override
    public AbstractFileOrTextStep newInstance(Map<String, Object> arguments) throws Exception {
        AbstractFileOrTextStep step = (AbstractFileOrTextStep) super.newInstance(arguments);
        if (isBlank(step.getFile()) && isBlank(step.getText())) {
            throw new IllegalArgumentException(Messages.AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument(getFunctionName()));
        }
        return step;
    }

    @Override
    public String argumentsToString(Map<String, Object> namedArgs) {
        if (!namedArgs.containsKey("file")) {
            return null;
        } else {
            return super.argumentsToString(Map.of("file", namedArgs.get("file")));
        }
    }

}
