package org.jenkinsci.plugins.pipeline.utility.steps;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.structs.DescribableHelper;

import java.util.Map;

import static org.apache.commons.lang.StringUtils.isBlank;

public abstract class AbstractFileOrTextStepDescriptorImpl extends AbstractStepDescriptorImpl {
    private final Class<? extends AbstractFileOrTextStep> stepType;

    public AbstractFileOrTextStepDescriptorImpl(Class<? extends AbstractFileOrTextStep> stepType, Class<? extends AbstractFileOrTextStepExecution> executionType) {
        super(executionType);
        this.stepType = stepType;
    }

    @Override
    public AbstractFileOrTextStep newInstance(Map<String, Object> arguments) throws Exception {
        AbstractFileOrTextStep step = DescribableHelper.instantiate(stepType, arguments);
        if (isBlank(step.getFile()) && isBlank(step.getText())) {
            throw new IllegalArgumentException("At least one of file or text needs to be provided.");
        }
        return step;
    }
}
