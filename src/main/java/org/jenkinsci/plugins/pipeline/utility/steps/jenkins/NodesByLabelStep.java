/*
 * The MIT License
 *
 * Copyright (c) 2018, Joseph Petersen
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

package org.jenkinsci.plugins.pipeline.utility.steps.jenkins;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.util.ComboBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.pipeline.utility.steps.Messages;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Obtains a list of node names by their label
 */
public class NodesByLabelStep extends Step {

    private final String label;
    private boolean offline = false;

    @DataBoundConstructor
    public NodesByLabelStep(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isOffline() {
        return offline;
    }

    @DataBoundSetter
    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    @Override
    public StepExecution start(StepContext context) {
        return new Execution(label, offline, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "nodesByLabel";
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.NodesByLabelStep_displayName();
        }

        @SuppressWarnings("unused") // used by stapler
        public ComboBoxModel doFillLabelItems() {
            ComboBoxModel cbm = new ComboBoxModel();
            Set<Label> labels = Jenkins.getInstance().getLabels();
            for (Label label : labels) {
                cbm.add(label.getDisplayName());
            }
            return cbm;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }
    }

    public static class Execution extends SynchronousStepExecution<List<String>> {

        private static final long serialVersionUID = 1L;
        private transient final String label;
        private transient final boolean includeOffline;

        Execution(String label, boolean includeOffline, StepContext context) {
            super(context);
            this.label = label;
            this.includeOffline = includeOffline;
        }

        @Override
        protected List<String> run() throws Exception {
            Label aLabel = Label.get(this.label);
            Set<Node> nodeSet = aLabel.getNodes();
            TaskListener listener = getContext().get(TaskListener.class);
            assert listener != null;
            PrintStream logger = listener.getLogger();
            List<String> nodes = new ArrayList<>();
            if (nodeSet != null && !nodeSet.isEmpty()) {
                for (Node node : nodeSet) {
                    Computer computer = node.toComputer();
                    if (!includeOffline && (computer == null || computer.isOffline())) {
                        continue;
                    }
                    nodes.add(node.getNodeName());
                }
            }
            if (nodes.isEmpty()) {
                logger.println("Could not find any nodes with '" + label + "' label");
            } else {
                logger.println("Found a total of " + nodes.size() + " nodes with the '" + label + "' label");
            }
            return nodes;
        }

    }

}
