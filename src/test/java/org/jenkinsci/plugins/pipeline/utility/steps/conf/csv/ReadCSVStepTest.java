/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2018 Electronic Arts Inc. All rights reserved.
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

package org.jenkinsci.plugins.pipeline.utility.steps.conf.csv;

import static org.jenkinsci.plugins.pipeline.utility.steps.Messages.AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument;

import hudson.model.Result;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.csv.Messages;
import org.jenkinsci.plugins.pipeline.utility.steps.csv.ReadCSVStep;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for {@link ReadCSVStep}.
 *
 * @author Stuart Rowe
 */
@WithJenkins
class ReadCSVStepTest {

    private JenkinsRule j;

    @TempDir
    private File temp;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void readFile() throws Exception {
        String file = writeCSV(getCSV());

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  List<CSVRecord> records = readCSV file: '"
                        + file + "'\n" + "  assert records.size() == 3\n"
                        + "  assert records[0].get(0) == 'key'\n"
                        + "  assert records[1].get(1) == 'b'\n"
                        + "  assert records[2].get(2) == '3'\n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void readDirectText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node {
                          def recordsString = "key,value,attr\\na,b,c\\n1,2,3\\n"
                          List<CSVRecord> records = readCSV text: recordsString
                          assert records.size() == 3
                          assert records[0].get(0) == 'key'
                          assert records[1].get(1) == 'b'
                          assert records[2].get(2) == '3'
                        }""",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void readNone() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node {
                          List<CSVRecord> records = readCSV()
                        }""",
                true));
        WorkflowRun run = p.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, run);
        j.assertLogContains(AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument("readCSV"), run);
    }

    @Test
    void readFileAndText() throws Exception {
        String file = writeCSV(getCSV());

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  def recordsString = \"key,value,attr\\na,b,c\\n1,2,3\\n\"\n"
                        + "  List<CSVRecord> records = readCSV text: recordsString, file: '"
                        + file + "'\n" + "  assert records.size() == 3\n"
                        + "  assert records[0].get(0) == 'key'\n"
                        + "  assert records[1].get(1) == 'b'\n"
                        + "  assert records[2].get(2) == '3'\n"
                        + "}",
                true));
        WorkflowRun run = p.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, run);
        j.assertLogContains(Messages.ReadCSVStepExecution_tooManyArguments("readCSV"), run);
    }

    @Test
    void readFileWithFormat() throws Exception {
        String file = writeCSV(getCSV());

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n"
                        + "  def format = CSVFormat.EXCEL.withHeader('key', 'value', 'attr').withSkipHeaderRecord(true)\n"
                        + "  List<CSVRecord> records = readCSV file: '"
                        + file + "', format: format\n" + "  assert records.size() == 2\n"
                        + "  assert records[0].get('key') == 'a'\n"
                        + "  assert records[0].get('value') == 'b'\n"
                        + "  assert records[0].get('attr') == 'c'\n"
                        + "  assert records[1].get('key') == '1'\n"
                        + "  assert records[1].get('value') == '2'\n"
                        + "  assert records[1].get('attr') == '3'\n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    private String getCSV(char separator) {
        String line0 = String.join(String.valueOf(separator), "key", "value", "attr");
        String line1 = String.join(String.valueOf(separator), "a", "b", "c");
        String line2 = String.join(String.valueOf(separator), "1", "2", "3");
        return String.join("\n", line0, line1, line2);
    }

    private String getCSV() {
        return getCSV(',');
    }

    private String writeCSV(String csv) throws IOException {
        File file = File.createTempFile("junit", null, temp);
        try (Writer f = new FileWriter(file);
                Reader r = new StringReader(csv)) {
            IOUtils.copy(r, f);
        }
        return FilenameTestsUtils.toPath(file);
    }
}
