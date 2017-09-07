package org.jenkinsci.plugins.pipeline.utility.steps;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

import java.util.Map;

import static org.apache.commons.lang.StringUtils.isBlank;

public abstract class AbstractFileOrTextStepDescriptorImpl extends AbstractStepDescriptorImpl {
    protected AbstractFileOrTextStepDescriptorImpl(Class<? extends StepExecution> executionType) {
        super(executionType);
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
