package org.jenkinsci.plugins.pipeline.utility.steps;

import hudson.FilePath;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.MissingContextVariableException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public abstract class AbstractFileOrTextStepExecution<T> extends SynchronousNonBlockingStepExecution<T> {

    private transient AbstractFileOrTextStep fileOrTextStep;

    protected FilePath ws;

    protected AbstractFileOrTextStepExecution(@Nonnull AbstractFileOrTextStep step, @Nonnull StepContext context) {
        super(context);
        this.fileOrTextStep = step;
    }

    @Override
    protected final T run() throws Exception {
        ws = getContext().get(FilePath.class);
        if (ws == null && isNotBlank(fileOrTextStep.getFile())) {
            throw new MissingContextVariableException(FilePath.class);
        }
        return doRun();
    }

    protected abstract T doRun() throws Exception;
}
