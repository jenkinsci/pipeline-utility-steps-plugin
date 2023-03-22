/*
 * The MIT License
 *
 * Copyright (c) 2023 CloudBees, Inc.
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
package org.jenkinsci.plugins.pipeline.utility.steps;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ClassicPluginStrategy;
import hudson.Extension;
import hudson.ExtensionComponent;
import hudson.ExtensionFinder;
import hudson.ExtensionList;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.Util;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Hudson;
import hudson.security.Permission;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.ExtensionComponentSet;
import jenkins.ExtensionFilter;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Extension
@Symbol("filterUtilitySteps")
public class StepSelectionConfiguration extends GlobalConfiguration {

    private Set<String> allow;
    private Set<String> deny;

    public StepSelectionConfiguration() {
        load();
        if (allow == null) {
            allow = Collections.emptySet();
        }
        if (deny == null) {
            deny = Collections.emptySet();
        }
    }

    public synchronized String getAllow() {
        return String.join(" ", allow);
    }

    public synchronized Set<String> getAllowList() {
        return Collections.unmodifiableSet(allow);
    }

    @DataBoundSetter
    public synchronized void setAllow(String allow) {
        this.allow = split(allow);
        saveAndRefreshExtensions();
    }

    public synchronized String getDeny() {
        return String.join(" ", deny);
    }

    public synchronized Set<String> getDenyList() {
        return Collections.unmodifiableSet(deny);
    }

    @DataBoundSetter
    public synchronized void setDeny(String deny) {
        this.deny = split(deny);
        saveAndRefreshExtensions();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        req.bindJSON(this, json);
        saveAndRefreshExtensions();
        return true;
    }

    public FormValidation doCheckAllow(@QueryParameter String value) {
        return checkFunctionNames(value);
    }

    public FormValidation doCheckDeny(@QueryParameter String value) {
        return checkFunctionNames(value);
    }

    private static FormValidation checkFunctionNames(String value) {
        Set<String> s = split(value);
        if (s.isEmpty()) {
            return FormValidation.ok();
        } else {
            Collection<String> names = getAllStepNames();
            Set<String> bad = new HashSet<>();
            for (String sf : s) {
                if (!names.contains(sf)) {
                    bad.add(sf);
                }
            }
            if (bad.isEmpty()) {
                return FormValidation.ok();
            } else {
                return FormValidation.warning("Unrecognised function name(s): " + String.join(", ", bad));
            }
        }
    }

    public AutoCompletionCandidates doAutoCompleteAllow(@QueryParameter String value) {
        return autoCompleteFunctionName(value);
    }

    public AutoCompletionCandidates doAutoCompleteDeny(@QueryParameter String value) {
        return autoCompleteFunctionName(value);
    }

    public static AutoCompletionCandidates autoCompleteFunctionName(String value) {
        Collection<String> names = getAllStepNames();
        List<String> values = splitToList(value);
        if (values.isEmpty() || value.endsWith(" ") || value.endsWith("\n")) {
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            candidates.getValues().addAll(names);
            return candidates;
        } else {
            String v = values.get(values.size() - 1);
            return names.stream().filter(s -> s.startsWith(v))
                    .collect(AutoCompletionCandidates::new, AutoCompletionCandidates::add,
                            (l, r) -> l.getValues().addAll(r.getValues()));
        }
    }

    @NonNull
    @Override
    public Permission getRequiredGlobalConfigPagePermission() {
        return Jenkins.MANAGE;
    }

    private void saveAndRefreshExtensions() {
        save();

        Jenkins jenkins = Jenkins.get();
        ExtensionList<StepDescriptor> stepDescriptors = ExtensionList.lookup(StepDescriptor.class);

        FilterImpl filter = ExtensionList.lookupSingleton(FilterImpl.class);
        Collection<StepDescriptor> definedStepDescriptors = getAllStepDescriptors();
        List<StepDescriptor> toRemove = new ArrayList<>();
        final List<StepDescriptor> toAdd = new ArrayList<>();
        for (StepDescriptor descriptor : definedStepDescriptors) {
            if (stepDescriptors.contains(descriptor)) {
                if (!filter.allows(StepDescriptor.class, new ExtensionComponent<>(descriptor))) {
                    toRemove.add(descriptor);
                }
            } else if (filter.allows(StepDescriptor.class, new ExtensionComponent<>(descriptor))) {
                toAdd.add(descriptor);
            }
        }
        stepDescriptors.refresh(new ExtensionComponentSet() {
            @Override
            public <T> Collection<ExtensionComponent<T>> find(Class<T> type) {
                if (type == StepDescriptor.class) {
                    return toAdd.stream().map(stepDescriptor -> new ExtensionComponent<T>(type.cast(stepDescriptor))).collect(Collectors.toSet());
                } else {
                    return Collections.emptyList();
                }
            }
        });
        stepDescriptors.removeAll(toRemove);
    }

    static Set<String> split(String list) {
        return new HashSet<>(splitToList(list));
    }

    static List<String> splitToList(String list) {
        list = Util.fixNull(list);
        return Arrays.asList(StringUtils.split(list));
    }

    public static StepSelectionConfiguration get() {
        return ExtensionList.lookupSingleton(StepSelectionConfiguration.class);
    }

    /**
     * Finds all {@link Step} names defined in this plugin.
     *
     * @return The unfiltered list of all {@link Step} names defined in this plugin.
     * @see #getAllStepDescriptors()
     */
    public static Collection<String> getAllStepNames() {
        return getAllStepDescriptors().stream().map(StepDescriptor::getFunctionName).collect(Collectors.toSet());
    }

    /**
     * Finds all {@link StepDescriptor}s defined in this plugin.
     * Because we are filtering the steps we need to find extensions from a lower level than {@link ExtensionList}.
     * But if the {@link hudson.PluginStrategy} is something unexpected (i.e. not {@link ClassicPluginStrategy})
     * we might not be doing it as expected.
     *
     * @return The unfiltered list of all {@link StepDescriptor}s defined in this plugin.
     */
    public static Collection<StepDescriptor> getAllStepDescriptors() {
        Hudson jenkins = Hudson.getInstance();
        if (jenkins == null) {
            return Collections.emptyList();
        }
        PluginManager pluginManager = jenkins.getPluginManager();

        List<ExtensionFinder> finders = jenkins.getExtensionList(ExtensionFinder.class);

        PluginWrapper myPlugin = pluginManager.whichPlugin(StepSelectionConfiguration.class);
        if (myPlugin == null) {
            return Collections.emptyList(); //In case we are running in some maven test environment or other weird scenario.
        }

        List<ExtensionComponent<StepDescriptor>> r = new ArrayList<>();
        for (ExtensionFinder finder : finders) {
            try {
                r.addAll(finder.find(StepDescriptor.class, jenkins));
            } catch (AbstractMethodError e) {
                //deprecated method is restricted so ignore
            }
        }

        return r.stream().filter(c -> pluginManager.whichPlugin(c.getInstance().getClass()) == myPlugin).map(ExtensionComponent::getInstance).collect(Collectors.toSet());
    }


    @Extension
    public static class FilterImpl extends ExtensionFilter {
        @Override
        public <T> boolean allows(Class<T> type, ExtensionComponent<T> component) {
            if (!type.isAssignableFrom(StepDescriptor.class)) {
                return true;
            }
            Jenkins jenkins = Jenkins.get();
            StepSelectionConfiguration configuration = StepSelectionConfiguration.get();
            Set<String> allow = configuration.getAllowList();
            Set<String> deny = configuration.getDenyList();
            if (allow.isEmpty() && deny.isEmpty()) {
                return true;
            }
            PluginWrapper me = jenkins.getPluginManager().whichPlugin(FilterImpl.class);
            if (me == null) {
                return true; //In case we are running in some maven test environment or other weird scenario.
            }
            StepDescriptor d = (StepDescriptor) component.getInstance();
            if (jenkins.getPluginManager().whichPlugin(d.getClass()) == me) {
                if (!deny.isEmpty() && deny.contains(d.getFunctionName())) {
                    return false;
                }
                if (!allow.isEmpty() && !allow.contains(d.getFunctionName())) {
                    return false;
                }
            }
            return true;
        }
    }
}
