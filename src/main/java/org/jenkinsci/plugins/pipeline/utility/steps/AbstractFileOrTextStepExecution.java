package org.jenkinsci.plugins.pipeline.utility.steps;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.MissingContextVariableException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;


import static org.apache.commons.lang.StringUtils.isNotBlank;

public abstract class AbstractFileOrTextStepExecution<T> extends SynchronousNonBlockingStepExecution<T> {

    private transient AbstractFileOrTextStep fileOrTextStep;

    protected FilePath ws;

    protected AbstractFileOrTextStepExecution(@NonNull AbstractFileOrTextStep step, @NonNull StepContext context) {
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
