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

import hudson.FilePath;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.jenkinsci.plugins.pipeline.utility.steps.zip.UnZipStepExecution;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import javax.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.Manifest;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Execution of {@link ReadManifestStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ReadManifestStepExecution extends AbstractSynchronousNonBlockingStepExecution<SimpleManifest> {

    private static final long serialVersionUID = 1L;

    @StepContextParameter
    private transient TaskListener listener;

    @StepContextParameter
    private transient FilePath ws;

    @Inject
    private transient ReadManifestStep step;

    @Override
    protected SimpleManifest run() throws Exception {
        if (isBlank(step.getFile()) && isBlank(step.getText())) {
            throw new IllegalArgumentException("Need to specify either file or text to readManifest.");
        } else if (!isBlank(step.getFile()) && !isBlank(step.getText())) {
            throw new IllegalArgumentException("Need to specify either file or text to readManifest, can't do both.");
        } else if (!isBlank(step.getFile())) {
            return parseFile(step.getFile());
        } else if (!isBlank(step.getText())) {
            return parseText(step.getText());
        }
        throw new IllegalStateException("A somewhat strange combination appeared.");
    }

    private SimpleManifest parseText(String text) throws IOException {
        Manifest manifest = new Manifest(new ByteArrayInputStream(text.getBytes("UTF-8")));
        return new SimpleManifest(manifest);
    }

    private SimpleManifest parseFile(String file) throws IOException, InterruptedException {
        FilePath path = ws.child(file);
        if (!path.exists()) {
            throw new FileNotFoundException(path.getRemote() + " does not exist.");
        } else if (path.isDirectory()) {
            throw new FileNotFoundException(path.getRemote() + " is a directory.");
        }
        String lcName = path.getName().toLowerCase();
        if(lcName.endsWith(".jar") || lcName.endsWith(".war") || lcName.endsWith(".ear")) {
            Map<String, String> mf = path.act(new UnZipStepExecution.UnZipFileCallable(listener, ws, "META-INF/MANIFEST.MF", true, "UTF-8"));
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
