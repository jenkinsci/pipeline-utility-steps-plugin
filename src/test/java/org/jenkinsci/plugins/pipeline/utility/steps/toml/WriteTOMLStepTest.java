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
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import hudson.model.Result;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests for {@link WriteTOMLStep}.
 *
 */
public class WriteTOMLStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void writeFile() throws Exception {
        int elements = 3;
        String groovyArrayOfLines =
                Arrays.deepToString(Arrays.stream(getTOML(elements).split("\n"))
                        .map(line -> "\"" + line + "\"")
                        .toArray());

        File output = temp.newFile();

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  def toml = readTOML text: "
                        + groovyArrayOfLines + ".join('\\n')\n" + "  toml.array[0] = [:]\n"
                        + "  toml.array["
                        + elements + "] = 45\n" + "  writeTOML file: '"
                        + FilenameTestsUtils.toPath(output) + "', toml: toml\n" + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        // file exists by default so we check that should not be empty
        assertThat(output.length(), greaterThan(0L));

        Object tomlRaw = new TomlMapper().readValue(output, Object.class);

        LinkedHashMap<?, ?> toml = (LinkedHashMap<?, ?>) tomlRaw;
        ArrayList<?> tomlArray = (ArrayList<?>) toml.get("array");

        assertFalse("Saved toml is an empty map", toml.isEmpty());
        assertThat(tomlArray.size(), is(elements + 1));
        assertNotNull("Unexpected element value", tomlArray.get(0));
        assertEquals("Unexpected element value", 45, tomlArray.get(elements));
    }

    @Test
    public void returnText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  String written = writeTOML returnText: true, toml: ['a': 1, 'b': 2] \n"
                        + "  def toml = readTOML text: written \n"
                        + "  assert toml == ['a': 1, 'b': 2] \n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void checkRequiredFileOrReturnText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  def toml = readTOML text: '\"\" = \"\"'\n" + "  writeTOML toml: toml" + "}", true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.WriteTOMLStepExecution_missingReturnTextAndFile("writeTOML"), run);
    }

    @Test
    public void checkRequiredTOML() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node {\n" + "  writeTOML file: 'output.toml'" + "}", true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.WriteTOMLStepExecution_missingTOML("writeTOML"), run);
    }

    @Test
    public void checkCannotReturnTextAndFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  writeTOML returnText: true, file: 'output.toml', toml: [foo: 'bar']" + "}", true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.WriteTOMLStepExecution_bothReturnTextAndFile("writeTOML"), run);
    }

    private String getTOML(int elements) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < elements; i++) {
            stringBuilder
                    .append("[[array]]")
                    .append("\n")
                    .append("index=")
                    .append(i)
                    .append("\n");
        }

        return stringBuilder.toString();
    }
}
