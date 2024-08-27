/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Nikolas Falco
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jenkinsci.plugins.pipeline.utility.steps.toml;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import java.io.FileNotFoundException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import org.jenkinsci.plugins.workflow.steps.MissingContextVariableException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

/**
 * Execution of {@link WriteTOMLStep}.
 *
 */
public class WriteTOMLStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    private static final long serialVersionUID = 1L;

    private transient WriteTOMLStep step;

    protected WriteTOMLStepExecution(@NonNull WriteTOMLStep step, @NonNull StepContext context) {
        super(context);
        this.step = step;
    }

    @Override
    protected Void run() throws Exception {
        FilePath ws = getContext().get(FilePath.class);
        if (ws == null) {
            throw new MissingContextVariableException(FilePath.class);
        }
        assert ws != null;

        FilePath path = ws.child(step.getFile());
        if (path.isDirectory()) {
            throw new FileNotFoundException(Messages.TOMLStepExecution_fileIsDirectory(path.getRemote()));
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(path.write(), StandardCharsets.UTF_8)) {
            step.execute(writer);
        }
        return null;
    }
}
