/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 CloudBees Inc.
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

package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * Execution of {@link FindFilesStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class FindFilesStepExecution extends SynchronousNonBlockingStepExecution<FileWrapper[]> {
    private static final long serialVersionUID = 1L;

    @Inject
    private transient FindFilesStep step;

    protected FindFilesStepExecution(@NonNull StepContext context, FindFilesStep step) {
        super(context);
        this.step = step;
    }

    @Override
    protected FileWrapper[] run() throws Exception {
        FilePath ws = getContext().get(FilePath.class);
        assert ws != null;
        List<FilePath> list;
        if (StringUtils.isBlank(step.getGlob())) {
            list = ws.list();
        } else {
            list = Arrays.asList(ws.list(step.getGlob(), step.getExcludes()));
        }
        FileWrapper[] res = new FileWrapper[list.size()];
        for(int i = 0; i < list.size(); i++) {
            res[i] = new FileWrapper(ws, list.get(i));
        }
        return res;
    }
}
