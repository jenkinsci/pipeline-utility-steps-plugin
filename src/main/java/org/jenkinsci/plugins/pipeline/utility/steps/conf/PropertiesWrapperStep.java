package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PropertiesWrapperStep extends AbstractPropertiesStep implements Serializable {
    private String file;
    private String text;
    private Map<String, Object> defaults;

    @DataBoundConstructor
    public PropertiesWrapperStep() {
    }

    @Override
    public String getFile() {
        return file;
    }

    @DataBoundSetter
    @Override
    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public String getText() {
        return text;
    }

    @DataBoundSetter
    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public Map<String, Object> getDefaults() {
        return defaults;
    }

    @DataBoundSetter
    @Override
    public void setDefaults(Map<String, Object> defaults) {
        this.defaults = defaults;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ExecutionImpl(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public String getFunctionName() {
            return "propertiesWrapper"; // TODO: Probably a better name is needed
        }

        @Override
        public String getDisplayName() {
            return "Read properties from a file in the workspace or text and add the properties to the environment";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(FilePath.class);
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public Step newInstance(Map<String, Object> arguments) throws Exception {
            return ConfReaderUtils.propertiesReaderFromMap(new PropertiesWrapperStep(), arguments);
        }

    }

    public static class ExecutionImpl extends StepExecution {
        private final PropertiesWrapperStep step;

        public ExecutionImpl(@Nonnull PropertiesWrapperStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        public boolean start() throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            FilePath ws = getContext().get(FilePath.class);

            Map<String, String> result = new HashMap<>();

            if (ws != null && listener != null) {
                if (!StringUtils.isBlank(step.getFile())) {
                    result.putAll(ConfReaderUtils.toStringMap(ConfReaderUtils.readProperties(step.getFile(),
                            step.getDefaults(), ws, listener)));
                }
            }

            if (!StringUtils.isBlank(step.getText())) {
                result.putAll(ConfReaderUtils.toStringMap(ConfReaderUtils.readProperties(step.getText(),
                        step.getDefaults())));
            }

            getContext().newBodyInvoker()
                    .withContexts(EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class),
                            EnvironmentExpander.constant(result)))
                    .withCallback(BodyExecutionCallback.wrap(getContext()))
                    .start();
            return false;
        }

        @Override
        public void stop(Throwable cause) throws Exception {
        }
    }

    private static final long serialVersionUID = 1L;
}
