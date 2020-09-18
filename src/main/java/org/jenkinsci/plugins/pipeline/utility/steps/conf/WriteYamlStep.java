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

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.DumperOptions;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.Yaml;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Writes a yaml file from the workspace.
 *
 * @author Javier DELGADO &lt;witokondoria@gmail.com&gt;.
 */
public class WriteYamlStep extends Step {

    private String file;
    private Object data;
    private String charset;
    private boolean overwrite;
    private boolean returnText;

    @DataBoundConstructor
    public WriteYamlStep(String file, @Nonnull Object data) {
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

    public String getCharset() {
        return charset;
    }

    /**
     * The charset encoding to use when writing the file. Defaults to UTF-8.
     * @param charset the charset
     * @see Charset
     * @see Charset#forName(String)
     */
    @DataBoundSetter
    public void setCharset(String charset) {
        this.charset = charset;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    @DataBoundSetter
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean isReturnText() {
        return returnText;
    }

    @DataBoundSetter
    public void setReturnText(boolean returnText) {
        this.returnText = returnText;
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

    @Override
    public StepExecution start(StepContext context) throws Exception {
        if (this.returnText) {
            if (this.file != null) {
                throw new IllegalArgumentException("cannot provide both returnText and file to writeYaml");
            }
            if (this.charset != null) {
                throw new IllegalArgumentException("cannot provide both returnText and charset to writeYaml");
            }
            if (this.overwrite) {
                throw new IllegalArgumentException("cannot provide both returnText and overwrite to writeYaml");
            }

            return new ReturnTextExecution(context, this);
        }

        if (isBlank(this.file)) {
            throw new IllegalArgumentException("either file or returnText must be provided to writeYaml");
        }

        return new Execution(context, this);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        public DescriptorImpl() {

        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, FilePath.class);
        }

        @Override
        public String getFunctionName() {
            return "writeYaml";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Write a yaml from an object.";
        }
    }

    private static class ReturnTextExecution extends SynchronousNonBlockingStepExecution<String> {
        private static final long serialVersionUID = 1L;

        private transient WriteYamlStep step;

        protected ReturnTextExecution(@Nonnull StepContext context, WriteYamlStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected String run() throws Exception {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            return yaml.dump(step.getData());
        }
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        private transient WriteYamlStep step;

        protected Execution(@Nonnull StepContext context, WriteYamlStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run () throws Exception {
            FilePath ws = getContext().get(FilePath.class);
            assert ws != null;
            FilePath path = ws.child(step.getFile());
            if (path.isDirectory()) {
                throw new FileNotFoundException(path.getRemote() + " is a directory.");
            }
            if (!step.isOverwrite() && path.exists()) {
                throw new FileAlreadyExistsException(path.getRemote());
            }

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);

            Charset cs;

            if (StringUtils.isEmpty(step.getCharset())) {
                cs = Charset.forName("UTF-8"); //If it doesn't exist then something is broken in the jvm
            } else {
                cs = Charset.forName(step.getCharset()); //Will throw stuff directly to the user
            }

            try (OutputStreamWriter writer = new OutputStreamWriter(path.write(), cs)) {
                yaml.dump (step.getData(), writer);
            }

            return null;
        }
    }
}
