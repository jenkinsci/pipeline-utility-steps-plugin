package org.jenkinsci.plugins.pipeline.utility.steps;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import java.io.IOException;
import org.jenkinsci.plugins.workflow.steps.MissingContextVariableException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;


import static org.apache.commons.lang3.StringUtils.isNotBlank;

public abstract class AbstractFileOrTextStepExecution<T> extends SynchronousNonBlockingStepExecution<T> {

    @VisibleForTesting
    interface WorkspaceHook {
        void accept(@CheckForNull FilePath workspace) throws IOException;
    }

    @Restricted(NoExternalUse.class)
    @VisibleForTesting
    static WorkspaceHook afterWorkspaceResolved = workspace -> {};

    private transient AbstractFileOrTextStep fileOrTextStep;

    protected FilePath ws;

    protected AbstractFileOrTextStepExecution(@NonNull AbstractFileOrTextStep step, @NonNull StepContext context) {
        super(context);
        this.fileOrTextStep = step;
    }

    @Override
    protected final T run() throws Exception {
        ws = getContext().get(FilePath.class);
        afterWorkspaceResolved.accept(ws);
        if (ws == null && isNotBlank(fileOrTextStep.getFile())) {
            throw new MissingContextVariableException(FilePath.class);
        }
        return doRun();
    }

    protected abstract T doRun() throws Exception;
}
