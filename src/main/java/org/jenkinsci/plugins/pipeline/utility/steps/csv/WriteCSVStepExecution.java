/*
 * The MIT License
 *
 * Copyright (C) 2018 Electronic Arts Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.pipeline.utility.steps.csv;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import hudson.FilePath;
import java.io.FileNotFoundException;
import java.io.OutputStreamWriter;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

/**
 *
 * @author Stuart Rowe
 */
public class WriteCSVStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    private static final long serialVersionUID = 1L;

    private final transient WriteCSVStep step;

    protected WriteCSVStepExecution(@NonNull WriteCSVStep step, @NonNull StepContext context) {
        super(context);
        this.step = step;
    }


    @Override
    protected Void run() throws Exception {
        FilePath ws = getContext().get(FilePath.class);
        assert ws != null;

        Iterable<?> records = step.getRecords();
        if (records == null) {
            throw new IllegalArgumentException(Messages.WriteCSVStepExecution_missingRecords(step.getDescriptor().getFunctionName()));
        }

        String file = step.getFile();
        if (StringUtils.isBlank(file)) {
            throw new IllegalArgumentException(Messages.WriteCSVStepExecution_missingFile(step.getDescriptor().getFunctionName()));
        }

        FilePath path = ws.child(file);
        if (path.isDirectory()) {
            throw new FileNotFoundException(Messages.CSVStepExecution_fileIsDirectory(path.getRemote()));
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(path.write(), "UTF-8")) {
            CSVFormat format = step.getFormat();
            if (format == null) {
                format = CSVFormat.DEFAULT;
            }

            CSVPrinter printer = new CSVPrinter(writer, format);
            printer.printRecords(records);
        }
        return null;
    }
}
