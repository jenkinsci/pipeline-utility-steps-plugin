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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;

/**
 * Execution of {@link ReadTOMLStep}.
 *
 */
public class ReadTOMLStepExecution extends AbstractFileOrTextStepExecution<Object> {
    private static final long serialVersionUID = 1L;

    private transient ReadTOMLStep step;

    protected ReadTOMLStepExecution(@NonNull ReadTOMLStep step, @NonNull StepContext context) {
        super(step, context);
        this.step = step;
    }

    @Override
    protected Object doRun() throws Exception {
        String fName = step.getDescriptor().getFunctionName();
        if (isNotBlank(step.getFile()) && isNotBlank(step.getText())) {
            throw new IllegalArgumentException(Messages.ReadTOMLStepExecution_tooManyArguments(fName));
        }

        Object toml = null;
        TomlMapper mapper = new TomlMapper();
        if (!isBlank(step.getFile())) {
            FilePath f = ws.child(step.getFile());

            if (!f.exists()) {
                throw new FileNotFoundException(Messages.TOMLStepExecution_fileNotFound(f.getRemote()));
            }

            if (f.isDirectory()) {
                throw new IllegalArgumentException(Messages.TOMLStepExecution_fileIsDirectory(f.getRemote()));
            }

            try (InputStream is = f.read()) {
                toml = mapper.readValue(IOUtils.toString(is, StandardCharsets.UTF_8), Object.class);
            }
        }

        if (!isBlank(step.getText())) {
            toml = mapper.readValue(step.getText().trim(), Object.class);
        }

        return toml;
    }
}
