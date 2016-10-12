/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 CloudBees Inc.
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

package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests for {@link FindFilesStep}
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class FindFilesStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();
    private WorkflowJob p;
    private static final String CODE =
            "node('slaves') {\n" +
            "  writeFile file: '1.txt', text: 'Who rules the world? Girls!'\n" +
            "  writeFile file: '2.txt', text: 'Who rules the world? Girls!'\n" +
            "  dir('a') {\n" +
            "    writeFile file: '3.txt', text: 'Who rules the world? Girls!'\n" +
            "    writeFile file: '4.txt', text: 'Who rules the world? Girls!'\n" +
            "    dir('aa') {\n" +
            "      writeFile file: '5.txt', text: 'Who rules the world? Girls!'\n" +
            "      writeFile file: '6.txt', text: 'Who rules the world? Girls!'\n" +
            "    }\n" +
            "    dir('ab') {\n" +
            "      writeFile file: '7.txt', text: 'Who rules the world? Girls!'\n" +
            "      writeFile file: '8.txt', text: 'Who rules the world? Girls!'\n" +
            "      dir('aba') {\n" +
            "        writeFile file: '9.txt', text: 'Who rules the world? Girls!'\n" +
            "        writeFile file: '10.txt', text: 'Who rules the world? Girls!'\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "  dir('b') {\n" +
            "    writeFile file: '11.txt', text: 'Who rules the world? Girls!'\n" +
            "    writeFile file: '12.txt', text: 'Who rules the world? Girls!'\n" +
            "  }\n" +
            "%TESTCODE%" +
            "}";

    @Before
    public void setup() throws Exception {
        j.createOnlineSlave(Label.get("slaves"));
        p = j.jenkins.createProject(WorkflowJob.class, "p");
    }

    @Test
    public void simpleList() throws Exception {
        String flow = CODE.replace("%TESTCODE%",
                "def files = findFiles()\n" +
                        "echo \"${files.length} files\"\n" +
                        "for(int i = 0; i < files.length; i++) {\n" +
                        "  echo \"F: ${files[i].path.replace('\\\\', '/')}\"\n" +
                        "}"
        );
        p.setDefinition(new CpsFlowDefinition(flow, false));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("4 files", run);
        j.assertLogContains("F: 1.txt", run);
        j.assertLogContains("F: 2.txt", run);
        j.assertLogContains("F: a/", run);
        j.assertLogContains("F: b/", run);
        j.assertLogNotContains("F: a/3.txt", run);
        j.assertLogNotContains("F: a/ab/7.txt", run);
    }

    @Test
    public void listAll() throws Exception {
        String flow = CODE.replace("%TESTCODE%",
                "def files = findFiles(glob: '**/*.txt')\n" +
                        "echo \"${files.length} files\"\n" +
                        "for(int i = 0; i < files.length; i++) {\n" +
                        "  echo \"F: ${files[i].path.replace('\\\\', '/')}\"\n" +
                        "}"
        );
        p.setDefinition(new CpsFlowDefinition(flow, false));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        j.assertLogContains("12 files", run);
        j.assertLogContains("F: 1.txt", run);
        j.assertLogContains("F: 2.txt", run);
        j.assertLogContains("F: a/3.txt", run);
        j.assertLogContains("F: a/4.txt", run);
        j.assertLogContains("F: a/aa/5.txt", run);
        j.assertLogContains("F: a/aa/6.txt", run);
        j.assertLogContains("F: a/ab/7.txt", run);
        j.assertLogContains("F: a/ab/8.txt", run);
        j.assertLogContains("F: a/ab/aba/9.txt", run);
        j.assertLogContains("F: a/ab/aba/10.txt", run);
        j.assertLogContains("F: b/11.txt", run);
        j.assertLogContains("F: b/12.txt", run);
    }

    @Test
    public void listSome() throws Exception {
        String flow = CODE.replace("%TESTCODE%",
                "def files = findFiles(glob: '**/a/*.txt')\n" +
                        "echo \"${files.length} files\"\n" +
                        "for(int i = 0; i < files.length; i++) {\n" +
                        "  echo \"F: ${files[i].path.replace('\\\\', '/')}\"\n" +
                        "}"
        );
        p.setDefinition(new CpsFlowDefinition(flow, false));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        j.assertLogContains("2 files", run);
        j.assertLogContains("F: a/3.txt", run);
        j.assertLogContains("F: a/4.txt", run);
        j.assertLogNotContains("F: 1.txt", run);
        j.assertLogNotContains("F: 2.txt", run);
        j.assertLogNotContains("F: a/aa/5.txt", run);
        j.assertLogNotContains("F: a/aa/6.txt", run);
        j.assertLogNotContains("F: a/ab/7.txt", run);
        j.assertLogNotContains("F: a/ab/8.txt", run);
        j.assertLogNotContains("F: a/ab/aba/9.txt", run);
        j.assertLogNotContains("F: a/ab/aba/10.txt", run);
        j.assertLogNotContains("F: b/11.txt", run);
        j.assertLogNotContains("F: b/12.txt", run);
    }
}