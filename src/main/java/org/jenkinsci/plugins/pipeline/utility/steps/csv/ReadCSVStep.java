/*
 * The MIT License
 *
 * Copyright (c) 2018 Stuart Rowe
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

import org.apache.commons.csv.CSVFormat;
import hudson.Extension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import javax.annotation.Nonnull;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStep;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepDescriptorImpl;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.cps.GroovyShellDecorator;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author Stuart Rowe
 */
public class ReadCSVStep extends AbstractFileOrTextStep {

    private static final String ORG_APACHE_COMMONS_CSV = "org.apache.commons.csv";
    private static final String[] ORG_APAHACE_COMMONS_CSV_WHITELISTED_CLASSES = {"CSVFormat", "CSVRecord"};
    private CSVFormat format;

    @DataBoundConstructor
    public ReadCSVStep() {
        this.format = CSVFormat.DEFAULT;
    }

    public CSVFormat getFormat() {
        return this.format;
    }

    @DataBoundSetter
    public void setFormat(CSVFormat format) {
        this.format = format;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ReadCSVStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends AbstractFileOrTextStepDescriptorImpl {

        public DescriptorImpl() {

        }

        @Override
        public String getFunctionName() {
            return "readCSV";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return Messages.ReadCSVStep_DescriptorImpl_displayName();
        }
    }
    
    /**
     * Auto imports org.apache.commons.csv.* .
     */
    @Extension(optional = true)
    public static class PackageAutoImporter extends GroovyShellDecorator {
        @Override
        public void customizeImports(CpsFlowExecution context, ImportCustomizer ic) {
            ic.addStarImports(ORG_APACHE_COMMONS_CSV);
        }
    }

    /**
     * Whitelists all non static setters, getters and constructors in the package org.apache.commons.csv.
     * As well as static fields.
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
            
            return aPackage.getName().equals(ORG_APACHE_COMMONS_CSV)
                    && Arrays.asList(ORG_APAHACE_COMMONS_CSV_WHITELISTED_CLASSES).contains(aClass.getSimpleName());
        }

        @Override
        public boolean permitsConstructor(@Nonnull Constructor<?> constructor, @Nonnull Object[] args) {
            final Class<?> aClass = constructor.getDeclaringClass();
            final Package aPackage = aClass.getPackage();

            if (aPackage == null) {
                return false;
            }

            return aPackage.getName().equals(ORG_APACHE_COMMONS_CSV)
                    && Arrays.asList(ORG_APAHACE_COMMONS_CSV_WHITELISTED_CLASSES).contains(aClass.getSimpleName());
        }

        @Override
        public boolean permitsStaticMethod(@Nonnull Method method, @Nonnull Object[] args) {
            final Class<?> aClass = method.getDeclaringClass();
            final Package aPackage = aClass.getPackage();

            if (aPackage == null) {
                return false;
            }
            return aPackage.getName().equals(ORG_APACHE_COMMONS_CSV)
                    && Arrays.asList(ORG_APAHACE_COMMONS_CSV_WHITELISTED_CLASSES).contains(aClass.getSimpleName());
        }

        @Override
        public boolean permitsFieldGet(@Nonnull Field field, @Nonnull Object receiver) {

            final Class<?> aClass = field.getDeclaringClass();
            final Package aPackage = aClass.getPackage();

            if (aPackage == null) {
                return false;
            }
            return aPackage.getName().equals(ORG_APACHE_COMMONS_CSV)
                    && Arrays.asList(ORG_APAHACE_COMMONS_CSV_WHITELISTED_CLASSES).contains(aClass.getSimpleName());
        }

        @Override
        public boolean permitsFieldSet(@Nonnull Field field, @Nonnull Object receiver, Object value) {
            final Class<?> aClass = receiver.getClass();
            final Package aPackage = aClass.getPackage();

            if (aPackage == null) {
                return false;
            }
            return aPackage.getName().equals(ORG_APACHE_COMMONS_CSV)
                    && Arrays.asList(ORG_APAHACE_COMMONS_CSV_WHITELISTED_CLASSES).contains(aClass.getSimpleName());
        }

        @Override
        public boolean permitsStaticFieldGet(@Nonnull Field field) {
            final Class<?> aClass = field.getDeclaringClass();
            final Package aPackage = aClass.getPackage();

            if (aPackage == null) {
                return false;
            }
            return aPackage.getName().equals(ORG_APACHE_COMMONS_CSV)
                    && Arrays.asList(ORG_APAHACE_COMMONS_CSV_WHITELISTED_CLASSES).contains(aClass.getSimpleName());
        }

        @Override
        public boolean permitsStaticFieldSet(@Nonnull Field field, Object value) {
            return false;
        }
    }
}
