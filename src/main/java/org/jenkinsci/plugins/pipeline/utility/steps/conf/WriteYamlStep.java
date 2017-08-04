/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Javier DELGADO
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

package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.DumperOptions;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.Yaml;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.*;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Writes a yaml file from the workspace.
 *
 * @author Javier DELGADO &lt;witokondoria@gmail.com&gt;.
 */
public class WriteYamlStep extends AbstractStepImpl {

    private String file;
    private Object data;

    @DataBoundConstructor
    public WriteYamlStep(@Nonnull String file, @Nonnull Object data) {
        if ((file == null) || (isBlank(file))) {
            throw new IllegalArgumentException("file parameter must be provided to writeYaml");
        }
        this.file = file;
        if (data == null) {
            throw new IllegalArgumentException("data parameter must be provided to writeYaml");
        } else if (!isValidObjectType(data)) {
            throw new IllegalArgumentException("data parameter has invalid content (no-basic classes)");
        }
        this.data = data;
    }

    /**
     * Name of the yaml file to write.
     *
     * @return file name
     */
    public String getFile() {
        return file;
    }

    /**
     * An Object containing data to be saved.
     *
     * @return data to save as yaml
     */
    public Object getData() {
        return data;
    }

    private boolean isValidObjectType(Object obj) {
        if ((obj instanceof Boolean) || (obj instanceof Character) ||
            (obj instanceof Number) || (obj instanceof String) ||
            (obj instanceof URL) || (obj instanceof Calendar) ||
            (obj instanceof Date) || (obj instanceof UUID) ||
            (obj == null)) {
            return true;
        } else if (obj instanceof Map)  {
            for (Object entry : ((Map)obj).entrySet()) {
                if (!isValidObjectType(((Map.Entry)entry).getKey()) || !isValidObjectType(((Map.Entry)entry).getValue())) {
                    return false;
                }
            }
            return true;
        } else if (obj instanceof Collection)  {
            for (Object o : ((Collection)obj)) {
                if (!isValidObjectType(o)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "writeYaml";
        }

        @Override
        public String getDisplayName() {
            return "Write a yaml from an object.";
        }
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient FilePath ws;

        @Inject
        private transient WriteYamlStep step;

        @Override
        protected Void run () throws Exception {
            FilePath path = ws.child(step.getFile());
            if (path.exists()) {
                throw new FileAlreadyExistsException(path.getRemote() + " already exist.");
            }
            if (path.isDirectory()) {
                throw new FileNotFoundException(path.getRemote() + " is a directory.");
            }

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);

            try (OutputStreamWriter writer = new OutputStreamWriter(path.write())) {
                yaml.dump (step.getData(), writer);
            }

            return null;
        }
    }
}