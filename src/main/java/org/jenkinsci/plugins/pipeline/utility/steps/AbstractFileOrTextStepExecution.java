package org.jenkinsci.plugins.pipeline.utility.steps;

import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.MissingContextVariableException;

import javax.inject.Inject;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public abstract class AbstractFileOrTextStepExecution<T> extends AbstractSynchronousNonBlockingStepExecution<T> {
    @Inject
    private transient AbstractFileOrTextStep fileOrTextStep;

    protected FilePath ws;

    @Override
    protected T run() throws Exception {
        ws = getContext().get(FilePath.class);
        if (ws == null && isNotBlank(fileOrTextStep.getFile())) {
            throw new MissingContextVariableException(FilePath.class);
        }
        return null;
    }
}
