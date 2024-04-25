package org.jenkinsci.plugins.pipeline.utility.steps.jenkins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Node;
import org.jenkinsci.plugins.pipeline.utility.steps.Messages;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Collections;
import java.util.Set;

public class CurrentAgentStep extends Step {

    @DataBoundConstructor
    public CurrentAgentStep() {
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }

        @Override
        public String getFunctionName() {
            return "currentAgent";
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.CurrentAgentStep_displayName();
        }


    }
    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(context);
    }

    public static class Execution extends SynchronousStepExecution<String> {

        protected Execution(@NonNull StepContext context) {
            super(context);
        }

        @Override
        protected String run() throws Exception {
            Node node = getContext().get(Node.class);
            if (node == null) {
                return "";
            }
            if ("".equals(node.getNodeName())) {
                return "built-in";
            }
            return node.getNodeName();
        }
    }
}
