/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Nikolas Falco
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

package org.jenkinsci.plugins.pipeline.utility.steps.toml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.jenkinsci.plugins.pipeline.utility.steps.Messages.AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import hudson.model.Result;
import java.io.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils;
import org.jenkinsci.plugins.workflow.actions.ArgumentsAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graphanalysis.DepthFirstScanner;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for {@link ReadTOMLStep}.
 *
 */
@WithJenkins
class ReadTOMLStepTest {
    private static final String SOME_TOML =
            """
            title = "TOML Example"
            year = 2024
            pi = 3.14
            is_active = true
            dob = 1992-04-15
            fruits = ["apple", "banana"]
            numbers = [1, 2, 3]
            # Extra owner information
            [owner]
              name = "Alice"
              dob = 1985-06-23
              [owner.location]
                city = "NYC"
                country = "USA"
              [owner.contact]
                location = { street = "123 Elm St", zip = "10001" }
            [[products]]
              name = "Laptop"
              price = 999.99
            [[products]]
              name = "Phone"
              price = 499.99
            """;

    private JenkinsRule j;

    @TempDir
    private File temp;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void readFile() throws Exception {
        String file = writeTOML(getTOML());

        WorkflowJob p1 = j.jenkins.createProject(WorkflowJob.class, "p1");
        p1.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  def toml = readTOML file: '"
                        + file + "'\n" + "  assert toml.tags.size() == 3\n"
                        + "  assert toml.tags[0] == 0\n"
                        + "  assert toml.tags[1] == 1\n"
                        + "  assert toml.tags[2] == 2\n"
                        + "  assert toml.key == 'value'\n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p1.scheduleBuild2(0));

        file = writeTOML(SOME_TOML);

        WorkflowJob p2 = j.jenkins.createProject(WorkflowJob.class, "p2");
        p2.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  def toml = readTOML file: '"
                        + file + "'\n" + "  println toml \n"
                        + "  assert toml.title == 'TOML Example'\n"
                        + "  assert toml.year == 2024\n"
                        + "  assert toml.pi == 3.14\n"
                        + "  assert toml.is_active == true\n"
                        + "  assert toml.dob == '1992-04-15'\n"
                        + "  assert toml.fruits.size() == 2\n"
                        + "  assert toml.fruits[0] == 'apple'\n"
                        + "  assert toml.fruits[1] == 'banana'\n"
                        + "  assert toml.numbers.size() == 3\n"
                        + "  assert toml.numbers[0] == 1\n"
                        + "  assert toml.numbers[1] == 2\n"
                        + "  assert toml.numbers[2] == 3\n"
                        + "  assert toml.owner.name == 'Alice'\n"
                        + "  assert toml.owner.dob == '1985-06-23'\n"
                        + "  assert toml.owner.location.city == 'NYC'\n"
                        + "  assert toml.owner.location.country == 'USA'\n"
                        + "  assert toml.owner.contact.location.street == '123 Elm St'\n"
                        + "  assert toml.owner.contact.location.zip == '10001'\n"
                        + "  assert toml.products.size() == 2\n"
                        + "  assert toml.products[0].name == 'Laptop'\n"
                        + "  assert toml.products[0].price == 999.99\n"
                        + "  assert toml.products[1].name == 'Phone'\n"
                        + "  assert toml.products[1].price == 499.99\n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p2.scheduleBuild2(0));
    }

    @Test
    void readText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        String groovyArrayOfLines = Arrays.deepToString(Arrays.stream(getTOML().split("\n"))
                .map(line -> "\"" + line + "\"")
                .toArray());

        p.setDefinition(new CpsFlowDefinition(
                "def toml = readTOML text: " + groovyArrayOfLines + ".join('\\n')\n"
                        + "  assert toml.tags.size() == 3\n"
                        + "  assert toml.tags[0] == 0\n"
                        + "  assert toml.tags[1] == 1\n"
                        + "  assert toml.tags[2] == 2\n"
                        + "  assert toml.key == 'value'\n",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void readDirectText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        def toml = readTOML text: 'a = 1\\nb = [2, 3]\\n[c]\\nd = 4'
                        assert toml.a == 1
                        assert toml.b.size() == 2
                        assert toml.b[0] == 2
                        assert toml.b[1] == 3
                        assert toml.c.d == 4
                        """,
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void readNone() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                node {
                  def toml = readTOML()
                }""", true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument("readTOML"), run);
    }

    @Test
    void readFileAndText() throws Exception {
        String file = writeTOML(getTOML());

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  def toml = readTOML( text: 'a = 1', file: '" + file + "' )\n" + "}", true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.ReadTOMLStepExecution_tooManyArguments("readTOML"), run);
    }

    private String getTOML() throws Exception {
        LinkedHashMap<Object, Object> toml = new LinkedHashMap<>();
        toml.put("tags", Arrays.asList(0, 1, 2));
        toml.put("key", "value");

        Writer writer = new StringWriter();
        new TomlMapper().writeValue(writer, toml);

        return writer.toString();
    }

    private String writeTOML(String toml) throws IOException {
        File file = File.createTempFile("junit", null, temp);
        try (Writer f = new FileWriter(file);
                Reader r = new StringReader(toml)) {
            IOUtils.copy(r, f);
        }
        return FilenameTestsUtils.toPath(file);
    }

    @Test
    void readTextHideContents() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("readTOML text: 'val = \"s3cr3t\"'", true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        for (FlowNode n : new DepthFirstScanner().allNodes(b.getExecution())) {
            assertThat(
                    "did not leak secret in " + n + " ~ " + n.getDisplayName(),
                    ArgumentsAction.getStepArgumentsAsString(n),
                    not(containsString("s3cr3t")));
        }
    }
}
