package org.jenkinsci.plugins.pipeline.utility.steps;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isBlank;

public abstract class AbstractFileOrTextStepDescriptorImpl extends StepDescriptor {
    @Deprecated
    protected AbstractFileOrTextStepDescriptorImpl(Class<? extends StepExecution> executionType) {
        super();
    }

    public AbstractFileOrTextStepDescriptorImpl() {
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
}
