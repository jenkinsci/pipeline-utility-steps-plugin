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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.csv.Messages;
import org.jenkinsci.plugins.pipeline.utility.steps.csv.WriteCSVStep;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Result;

/**
 * Tests for {@link WriteCSVStep}.
 *
 * @author Stuart Rowe
 */
public class WriteCSVStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void writeFile() throws Exception {
        File output = temp.newFile();

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                "  def recordsString = \"key,value,attr\\na,b,c\\n1,2,3\\n\"\n" +
                "  List<CSVRecord> records = readCSV text: recordsString\n" +
                "  writeCSV file: '" + FilenameTestsUtils.toPath(output) + "', records: records\n" +
                "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        // file exists by default so we check that should not be empty
        assertThat(output.length(), greaterThan(0l));

        String lines = new String(Files.readAllBytes(Paths.get(output.toURI())));
        assertThat(lines.split("\r\n|\r|\n").length, equalTo(3));
    }

    @Test
    public void checkRequiredFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                "  def recordsString = \"key,value,attr\\na,b,c\\n1,2,3\\n\"\n" +
                "  List<CSVRecord> records = readCSV text: recordsString\n" +
                "  writeCSV records: records" +
                "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.WriteCSVStepExecution_missingFile("writeCSV"), run);
    }

    @Test
    public void checkRequiredRecords() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                "  writeCSV file: 'output.csv'" +
                "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.WriteCSVStepExecution_missingRecords("writeCSV"), run);
    }

    @Test
    public void writeFileWithHeader() throws Exception {
        File output = temp.newFile();

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                "  def recordsString = \"a,b,c\\n1,2,3\\n\"\n" +
                "  List<CSVRecord> records = readCSV text: recordsString\n" +
                "  def format = CSVFormat.EXCEL.withHeader('key', 'value', 'attr')\n" +
                "  writeCSV file: '" + FilenameTestsUtils.toPath(output) + "', records: records, format: format\n" +
                "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        // file exists by default so we check that should not be empty
        assertThat(output.length(), greaterThan(0l));

        String lines = new String(Files.readAllBytes(Paths.get(output.toURI())));
        assertThat(lines.split("\r\n|\r|\n").length, equalTo(3));
    }

}