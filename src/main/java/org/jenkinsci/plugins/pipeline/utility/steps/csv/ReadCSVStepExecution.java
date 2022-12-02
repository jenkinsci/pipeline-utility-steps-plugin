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
import hudson.FilePath;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import static org.apache.commons.lang.StringUtils.isNotBlank;


/**
 *
 * @author Stuart Rowe
 */
public class ReadCSVStepExecution extends AbstractFileOrTextStepExecution<List<CSVRecord>> {
    private static final long serialVersionUID = 1L;

    private final transient ReadCSVStep step;

    protected ReadCSVStepExecution(@NonNull ReadCSVStep step, @NonNull StepContext context) {
        super(step, context);
        this.step = step;
    }

    @Override
    protected List<CSVRecord> doRun() throws Exception {
        String fName = step.getDescriptor().getFunctionName();
        if (isNotBlank(step.getFile()) && isNotBlank(step.getText())) {
            throw new IllegalArgumentException(Messages.ReadCSVStepExecution_tooManyArguments(fName));
        }

        List<CSVRecord> records = new ArrayList<>();
        try (Reader reader = createReader()) {
            if (reader != null) {
                CSVFormat format = step.getFormat();
                if (format == null) {
                    format = CSVFormat.DEFAULT;
                }

                CSVParser parser = format.parse(reader);
                records.addAll(parser.getRecords());
            }
        }
        return records;
    }

    private Reader createReader() throws IOException, InterruptedException {
        if (isNotBlank(step.getFile())) {
            FilePath f = ws.child(step.getFile());
            if (f.exists() && !f.isDirectory()) {
                return new InputStreamReader(f.read(), StandardCharsets.UTF_8);
            }
        }

        if (isNotBlank(step.getText())) {
            return new StringReader(step.getText());
        }
        return null;
    }
}
