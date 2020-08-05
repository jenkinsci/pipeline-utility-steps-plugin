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

package org.jenkinsci.plugins.pipeline.utility.steps.conf.mf;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepExecution;
import org.jenkinsci.plugins.pipeline.utility.steps.zip.UnZipStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import javax.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.jar.Manifest;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Execution of {@link ReadManifestStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ReadManifestStepExecution extends AbstractFileOrTextStepExecution<SimpleManifest> {
    private static final long serialVersionUID = 1L;

    private transient ReadManifestStep step;

    protected ReadManifestStepExecution(@Nonnull ReadManifestStep step, @Nonnull StepContext context) {
        super(step, context);
        this.step = step;
    }

    @Override
    protected SimpleManifest doRun() throws Exception {
        if (!isBlank(step.getFile()) && !isBlank(step.getText())) {
            throw new IllegalArgumentException("Need to specify either file or text to readManifest, can't do both.");
        } else if (!isBlank(step.getFile())) {
            return parseFile(step.getFile());
        } else if (!isBlank(step.getText())) {
            return parseText(step.getText());
        }
        throw new IllegalStateException("A somewhat strange combination appeared.");
    }

    private SimpleManifest parseText(String text) throws IOException {
        Manifest manifest = new Manifest(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        return new SimpleManifest(manifest);
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE", justification = "I don't understand what/where it is.")
    private SimpleManifest parseFile(String file) throws IOException, InterruptedException {
        TaskListener listener = getContext().get(TaskListener.class);
        FilePath path = ws.child(file);
        if (!path.exists()) {
            throw new FileNotFoundException(path.getRemote() + " does not exist.");
        } else if (path.isDirectory()) {
            throw new FileNotFoundException(path.getRemote() + " is a directory.");
        }
        String lcName = path.getName().toLowerCase();
        if(lcName.endsWith(".jar") || lcName.endsWith(".war") || lcName.endsWith(".ear")) {
            Map<String, String> mf = path.act(new UnZipStepExecution.UnZipFileCallable(listener, ws, "META-INF/MANIFEST.MF", true, "UTF-8", false));
            String text = mf.get("META-INF/MANIFEST.MF");
            if (isBlank(text)) {
                throw new FileNotFoundException(path.getRemote() + " does not seem to contain a manifest.");
            } else {
                return parseText(text);
            }
        } else {
            try (InputStream is = path.read()) {
                Manifest manifest = new Manifest(is);
                return new SimpleManifest(manifest);
            }
        }
    }
}
