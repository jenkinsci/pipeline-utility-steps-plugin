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

import hudson.ExtensionList;
import org.hamcrest.Matcher;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RealJenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

public class StepSelectionConfigurationTest {

    @Rule
    public RealJenkinsRule r = new RealJenkinsRule().withDebugPort(4001).withDebugServer(false);

    @Test
    @WithoutJenkins
    public void split() {
        Set<String> list = StepSelectionConfiguration.split("one two\n three  \n  four");
        assertThat(list, containsInAnyOrder(equalTo("one"),
                equalTo("two"), equalTo("three"), equalTo("four")));
    }

    private static final Matcher<String>[] allSteps = new Matcher[]{
            equalTo("readProperties"),
            equalTo("readYaml"),
            equalTo("writeYaml"),
            equalTo("readCSV"),
            equalTo("writeCSV"),
            equalTo("sha1"),
            equalTo("sha256"),
            equalTo("verifySha1"),
            equalTo("verifySha256"),
            equalTo("findFiles"),
            equalTo("prependToFile"),
            equalTo("tee"),
            equalTo("touch"),
            equalTo("nodesByLabel"),
            equalTo("readJSON"),
            equalTo("writeJSON"),
            equalTo("readMavenPom"),
            equalTo("writeMavenPom"),
            equalTo("tar"),
            equalTo("untar"),
            equalTo("zip"),
            equalTo("unzip"),
            equalTo("compareVersions"),
            equalTo("readManifest")
    };


    @Test
    public void getAllStepNames() throws Throwable {
        r.then((RealJenkinsRule.Step) r -> {
            assertThat(StepSelectionConfiguration.getAllStepNames(), containsInAnyOrder(allSteps));
            //blacklist something
            StepSelectionConfiguration.get().setDeny("findFiles readMavenPom");
            //All steps should still be listed
            assertThat(StepSelectionConfiguration.getAllStepNames(), containsInAnyOrder(allSteps));
            //allow something
            StepSelectionConfiguration.get().setAllow("zip tar");
            //All steps should still be listed
            assertThat(StepSelectionConfiguration.getAllStepNames(), containsInAnyOrder(allSteps));
        });

    }

    @Test
    public void filteredSteps() throws Throwable {
        r.then((RealJenkinsRule.Step) r -> {
            Set<String> steps = ExtensionList.lookup(StepDescriptor.class).stream().map(StepDescriptor::getFunctionName).collect(Collectors.toSet());
            assertThat(steps, hasItems(allSteps));

            //blacklist something
            StepSelectionConfiguration.get().setDeny("findFiles readMavenPom");
            //Now try to list them again
            steps = ExtensionList.lookup(StepDescriptor.class).stream().map(StepDescriptor::getFunctionName).collect(Collectors.toSet());
            assertThat(steps, not(hasItems(equalTo("findFiles"), equalTo("readMavenPom"))));

            //Clear the filter
            StepSelectionConfiguration.get().setDeny("");
            //Now it should have them all again
            steps = ExtensionList.lookup(StepDescriptor.class).stream().map(StepDescriptor::getFunctionName).collect(Collectors.toSet());
            assertThat(steps, hasItems(allSteps));

            //Allow something
            StepSelectionConfiguration.get().setAllow("zip tar");
            //Now try to list them again
            steps = ExtensionList.lookup(StepDescriptor.class).stream().map(StepDescriptor::getFunctionName).collect(Collectors.toSet());
            assertThat(steps, hasItems(equalTo("zip"), equalTo("tar")));
            Matcher<String>[] rest = Arrays.stream(allSteps).filter(m -> !m.toString().equals(equalTo("zip").toString()) && !m.toString().equals(equalTo("tar").toString())).collect(Collectors.toList()).toArray(new Matcher[1]);
            assertThat(steps, not(hasItems(rest)));

            //reset
            StepSelectionConfiguration.get().setAllow("");
            //Now it should have them all again
            steps = ExtensionList.lookup(StepDescriptor.class).stream().map(StepDescriptor::getFunctionName).collect(Collectors.toSet());
            assertThat(steps, hasItems(allSteps));
        });
    }
}