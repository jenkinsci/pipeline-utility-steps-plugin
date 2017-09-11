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

package org.jenkinsci.plugins.pipeline.utility.steps.conf.json;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.json.Messages;
import org.jenkinsci.plugins.pipeline.utility.steps.json.ReadJSONStep;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Result;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import static org.jenkinsci.plugins.pipeline.utility.steps.Messages.AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument;

/**
 * Tests for {@link ReadJSONStep}.
 *
 * @author Nikolas Falco
 */
public class ReadJSONStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void readFile() throws Exception {
        String file = writeJSON(getJSON());

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def json = readJSON file: '" + file + "'\n" +
                        "  assert json.isArray() == false\n" +
                        "  assert json.tags.isArray() == true\n" +
                        "  assert json.tags.size() == 3\n" +
                        "  assert json.tags[0] == 0\n" +
                        "  assert json.tags[1] == 1\n" +
                        "  assert json.tags[2] == 2\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                        "def json = readJSON text: '" + getJSON() + "'\n" +
                        "assert json.tags[0] == 0\n" +
                        "assert json.tags[1] == 1\n" +
                        "assert json.tags[2] == 2\n", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readDirectText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                        "def json = readJSON text: '[ { \"key\": 0 }, { \"key\": \"value\" }, { \"key\": true } ]'\n" +
                        "assert json.isArray() == true\n" +
                        "assert json.size() == 3\n" +
                        "assert json[0].key == 0\n" +
                        "assert json[1].key == 'value'\n" +
                        "assert json[2].key == true\n", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readNone() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def json = readJSON()\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument("readJSON"), run);
    }

    @Test
    public void readFileAndText() throws Exception {
        String file = writeJSON(getJSON());

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def json = readJSON( text: '{ \"key\": \"value\" }', file: '" + file + "' )\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.ReadJSONStepExecution_tooManyArguments("readJSON"), run);
    }

    private String getJSON() throws IOException {
        JSONArray tags = new JSONArray();
        for (int i = 0; i < 3; i++) {
            tags.add(i);
        }
        JSONObject root = new JSONObject();
        root.put("tags", tags);
        return root.toString();
    }

    private String writeJSON(String json) throws IOException {
        File file = temp.newFile();
        try (Writer f = new FileWriter(file); Reader r = new StringReader(json)) {
            IOUtils.copy(r, f);
        }
        return FilenameTestsUtils.toPath(file);
    }

}