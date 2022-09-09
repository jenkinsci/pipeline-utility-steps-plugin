/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Martin d'Anjou
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

package org.jenkinsci.plugins.pipeline.utility.steps.template;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepExecution;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.GroovySandbox;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.ProxyWhitelist;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.io.IOException;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Execution of {@link SimpleTemplateEngineStep}.
 *
 * @author Martin d'Anjou
 */
public class SimpleTemplateEngineStepExecution extends AbstractFileOrTextStepExecution<Object> {
    private static final long serialVersionUID = 1L;

    private transient SimpleTemplateEngineStep step;

    protected SimpleTemplateEngineStepExecution(@NonNull SimpleTemplateEngineStep step, @NonNull StepContext context) {
        super(step, context);
        this.step = step;
    }

    @Override
    protected String doRun() throws Exception {
        String fName = step.getDescriptor().getFunctionName();
        if (isNotBlank(step.getFile()) && isNotBlank(step.getText())) {
            throw new IllegalArgumentException(Messages.SimpleTemplateEngineStepExecution_tooManyArguments(fName));
        }
        
        if (step.getBindings() == null) {
            throw new IllegalArgumentException(Messages.SimpleTemplateEngineStepExecution_missingBindings(fName));
        }

        SimpleTemplateEngine engine = new SimpleTemplateEngine();
        Template template = null;
        if (isNotBlank(step.getFile())) {
            FilePath f = ws.child(step.getFile());
            if (f.exists() && !f.isDirectory()) {
                try (InputStream is = f.read()) {
                    template = engine.createTemplate(IOUtils.toString(is, StandardCharsets.UTF_8));
                }
            } else if (f.isDirectory()) {
                throw new IllegalArgumentException(Messages.SimpleTemplateEngineStepExecution_fileIsDirectory(f.getRemote()));
            } else if (!f.exists()) {
                throw new FileNotFoundException(Messages.SimpleTemplateEngineStepExecution_fileNotFound(f.getRemote()));
	    }
        }
        if (isNotBlank(step.getText())) {
            template = engine.createTemplate(step.getText().trim());
        }

        String renderedTemplate = "";
        final Template templateR = template;
        final Map<String, Object> bindings = step.getBindings();
        if (step.isRunInSandbox()) {
            renderedTemplate = GroovySandbox.runInSandbox(
                () -> {
                    return templateR.make(bindings).toString();
                },
                new ProxyWhitelist(Whitelist.all())
            );
        } else {
            renderedTemplate = templateR.make(bindings).toString();
        }
        return renderedTemplate;
    }
}

