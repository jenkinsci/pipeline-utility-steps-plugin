/*
 * The MIT License
 *
 * Copyright 2017 CloudBees, Inc.
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

package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.Extension;
import hudson.FilePath;
import hudson.console.ConsoleLogFilter;
import hudson.model.Run;
import hudson.remoting.Channel;
import hudson.remoting.RemoteOutputStream;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Set;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.io.output.TeeOutputStream;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.BodyInvoker;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

public class TeeStep extends Step {

    public final String file;

    @DataBoundConstructor
    public TeeStep(String file) {
        this.file = file;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(context, file);
    }

    private static final class TeeTail extends BodyExecutionCallback.TailCall {

        private static final long serialVersionUID = 1L;

        private final TeeFilter filter;

        TeeTail(TeeFilter filter) {
            this.filter = filter;
        }

        @Override
        protected void finished(StepContext sc) throws Exception {
            filter.close();
        }
    }

    private static class Execution extends StepExecution {

        private final String file;

        Execution(StepContext context, String file) {
            super(context);
            this.file = file;
        }

        @Override
        public boolean start() throws Exception {
            FilePath f = getContext().get(FilePath.class).child(file);
            TeeFilter filter = new TeeFilter(f);
            getContext().newBodyInvoker().
                withContext(BodyInvoker.mergeConsoleLogFilters(getContext().get(ConsoleLogFilter.class), filter)).
                withCallback(new TeeTail(filter)).
                start();
            return false;
        }

        private static final long serialVersionUID = 1;

    }

    private static class TeeFilter extends ConsoleLogFilter implements Serializable {

        private final FilePath f;
        private boolean transferredToRemote = false;
        private transient OutputStream stream = null;

        TeeFilter(FilePath f) {
            this.f = f;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public OutputStream decorateLogger(Run build, final OutputStream logger) throws IOException, InterruptedException {
            return new TeeOutputStream(logger, stream = append(f, stream));
        }

        private static final long serialVersionUID = 1;

        void close() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        private void writeObject(ObjectOutputStream oos) throws IOException, InterruptedException {
            /*
              This gets called either to serialize to allow resume or to transfer to a remote.
              When resuming, the stream gets recreated as and when needed.
              When transferring the stream needs to transfer as well.

              The transferredToRemote is used to detect whether the stream got serialized as well
              which is also guaranteeing backward compatibility when resuming after plug-in upgrade.
             */
            transferredToRemote = Channel.current() != null;
            oos.defaultWriteObject();
            // Reset the transferredToRemote to ensure it doesn't get stored for resuming later.
            boolean saveStream = transferredToRemote;
            transferredToRemote = false;
            if (saveStream) {
                oos.writeObject(stream = append(f, stream));
            }
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            // Reset the transferredToRemote to ensure it doesn't get stored for resuming later.
            boolean saveStream = transferredToRemote;
            transferredToRemote = false;
            if (saveStream) {
                // Transferred, so the stream got transferred as well.
                stream = (OutputStream) ois.readObject();
            }
        }

        private Object readResolve() {
            return this;
        }

    }

    /** @see FilePath#write() */
    private static OutputStream append(FilePath fp, OutputStream stream) throws IOException, InterruptedException {
        if (stream == null) {
            return fp.act(new MasterToSlaveFileCallable<OutputStream>() {
                private static final long serialVersionUID = 1L;
                @Override
                public OutputStream invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                    f = f.getAbsoluteFile();
                    if (!f.getParentFile().exists() && !f.getParentFile().mkdirs()) {
                        throw new IOException("Failed to create directory " + f.getParentFile());
                    }
                    try {
                        return new RemoteOutputStream(Files.newOutputStream(f.toPath(), StandardOpenOption.CREATE, StandardOpenOption.APPEND/*, StandardOpenOption.DSYNC*/));
                    } catch (InvalidPathException e) {
                        throw new IOException(e);
                    }
                }
            });
        }
        return stream;
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(FilePath.class);
        }

        @Override
        public String getFunctionName() {
            return "tee";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Tee output to file";
        }

    }

}
