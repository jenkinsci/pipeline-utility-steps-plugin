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

package org.jenkinsci.plugins.pipeline.utility.steps.maven;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.cps.GroovyShellDecorator;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Reads a maven pom file from the workspace.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ReadMavenPomStep extends Step {

    private static final String ORG_APACHE_MAVEN_MODEL = "org.apache.maven.model";
    private String file;

    @DataBoundConstructor
    public ReadMavenPomStep() {
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(this, context);
    }

    /**
     * Optional name of the maven file to read.
     * If empty 'pom.xml' in the current working directory will be used.
     *
     * @return file name
     */
    public String getFile() {
        return file;
    }

    /**
     * Optional name of the maven file to read.
     * If empty 'pom.xml' in the current working directory will be used.
     *
     * @param file optional file name
     */
    @DataBoundSetter
    public void setFile(String file) {
        this.file = file;
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
            return "readMavenPom";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Read a maven project file.";
        }
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<Model> {

        private transient ReadMavenPomStep step;

        protected Execution(@Nonnull ReadMavenPomStep step, @Nonnull StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Model run() throws Exception {
            FilePath ws = getContext().get(FilePath.class);
            assert ws != null;
            FilePath path;
            if (!StringUtils.isBlank(step.getFile())) {
                path = ws.child(step.getFile());
            } else {
                path = ws.child("pom.xml");
            }
            if (!path.exists()) {
                throw new FileNotFoundException(path.getRemote() + " does not exist.");
            }
            if (path.isDirectory()) {
                throw new FileNotFoundException(path.getRemote() + " is a directory.");
            }
            try (InputStream is = path.read()) {
                return new MavenXpp3Reader().read(is);
            }
        }
    }

    /**
     * Auto imports org.apache.maven.model.* .
     */
    @Extension
    public static class PackageAutoImporter extends GroovyShellDecorator {
        @Override
        public void customizeImports(CpsFlowExecution context, ImportCustomizer ic) {
            ic.addStarImports(ORG_APACHE_MAVEN_MODEL);
        }
    }

    /**
     * Whitelists all non static setters, getters and constructors in the package org.apache.maven.model.
     */
    @Extension
    public static class WhiteLister extends Whitelist {
        public WhiteLister() {
            super();
        }

        @Override
        public boolean permitsMethod(Method method, Object receiver, Object[] args) {
            if (receiver == null) {
                return false;
            }

            final Class<?> aClass = receiver.getClass();
            final Package aPackage = aClass.getPackage();

            if(aPackage == null) {
                return false;
            }

            final String name = aPackage.getName();
            return name.equals(ORG_APACHE_MAVEN_MODEL)
                    && (   method.getName().startsWith("set")
                        || method.getName().startsWith("get")
                        || method.getName().startsWith("add")
                        || method.getName().startsWith("find") //in case we add some helpers to the meta object
                       );
        }

        @Override
        public boolean permitsConstructor(@Nonnull Constructor<?> constructor, @Nonnull Object[] args) {
            if (constructor == null) {
                return false;
            }

            final Package aPackage = constructor.getDeclaringClass().getPackage();

            if (aPackage == null) {
                return false;
            }

            return aPackage.getName().equals(ORG_APACHE_MAVEN_MODEL);
        }

        @Override
        public boolean permitsStaticMethod(@Nonnull Method method, @Nonnull Object[] args) {
            return false;
        }

        @Override
        public boolean permitsFieldGet(@Nonnull Field field, @Nonnull Object receiver) {

            if (receiver == null) {
                return false;
            }


            final Package aPackage = receiver.getClass().getPackage();
            if (aPackage == null) {
                return false;
            }

            return aPackage.getName().equals(ORG_APACHE_MAVEN_MODEL);
        }

        @Override
        public boolean permitsFieldSet(@Nonnull Field field, @Nonnull Object receiver, Object value) {

            if (receiver == null) {
                return false;
            }

            final Package aPackage = receiver.getClass().getPackage();
            if (aPackage == null) {
                return false;
            }

            return aPackage.getName().equals(ORG_APACHE_MAVEN_MODEL);
        }

        @Override
        public boolean permitsStaticFieldGet(@Nonnull Field field) {
            return false;
        }

        @Override
        public boolean permitsStaticFieldSet(@Nonnull Field field, Object value) {
            return false;
        }
    }
}
