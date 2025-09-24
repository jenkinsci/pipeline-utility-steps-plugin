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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for {@link FindFilesStep}
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
@WithJenkins
class FindFilesStepTest {

    private JenkinsRule j;
    private WorkflowJob p;
    private static final String CODE =
            """
                    node('slaves') {
                      writeFile file: '1.txt', text: 'Who rules the world? Girls!'
                      writeFile file: '2.txt', text: 'Who rules the world? Girls!'
                      dir('a') {
                        writeFile file: '3.txt', text: 'Who rules the world? Girls!'
                        writeFile file: '4.txt', text: 'Who rules the world? Girls!'
                        dir('aa') {
                          writeFile file: '5.txt', text: 'Who rules the world? Girls!'
                          writeFile file: '6.txt', text: 'Who rules the world? Girls!'
                        }
                        dir('ab') {
                          writeFile file: '7.txt', text: 'Who rules the world? Girls!'
                          writeFile file: '8.txt', text: 'Who rules the world? Girls!'
                          dir('aba') {
                            writeFile file: '9.txt', text: 'Who rules the world? Girls!'
                            writeFile file: '10.txt', text: 'Who rules the world? Girls!'
                          }
                        }
                      }
                      dir('b') {
                        writeFile file: '11.txt', text: 'Who rules the world? Girls!'
                        writeFile file: '12.txt', text: 'Who rules the world? Girls!'
                      }
                    %TESTCODE%\
                    }""";

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        j.createOnlineSlave(Label.get("slaves"));
        p = j.jenkins.createProject(WorkflowJob.class, "p");
    }

    @Test
    void simpleList() throws Exception {
        String flow = CODE.replace(
                "%TESTCODE%",
                """
                        def files = findFiles()
                        echo "${files.length} files"
                        for(int i = 0; i < files.length; i++) {
                          echo "F: ${files[i].path.replace('\\\\', '/')}"
                        }""");
        p.setDefinition(new CpsFlowDefinition(flow, true));
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
    void listAll() throws Exception {
        String flow = CODE.replace(
                "%TESTCODE%",
                """
                        def files = findFiles(glob: '**/*.txt')
                        echo "${files.length} files"
                        for(int i = 0; i < files.length; i++) {
                          echo "F: ${files[i].path.replace('\\\\', '/')}"
                        }""");
        p.setDefinition(new CpsFlowDefinition(flow, true));
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
    void listSome() throws Exception {
        String flow = CODE.replace(
                "%TESTCODE%",
                """
                        def files = findFiles(glob: '**/a/*.txt')
                        echo "${files.length} files"
                        for(int i = 0; i < files.length; i++) {
                          echo "F: ${files[i].path.replace('\\\\', '/')}"
                        }""");
        p.setDefinition(new CpsFlowDefinition(flow, true));
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

    @Test
    void listSomeWithExclusions() throws Exception {
        String flow = CODE.replace(
                "%TESTCODE%",
                """
                        def files = findFiles(glob: '**/*.txt', excludes: 'b/*.txt,**/aba/*.txt')
                        echo "${files.length} files"
                        for(int i = 0; i < files.length; i++) {
                          echo "F: ${files[i].path.replace('\\\\', '/')}"
                        }""");
        p.setDefinition(new CpsFlowDefinition(flow, true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        j.assertLogContains("8 files", run);
        j.assertLogContains("F: 1.txt", run);
        j.assertLogContains("F: 2.txt", run);
        j.assertLogContains("F: a/3.txt", run);
        j.assertLogContains("F: a/4.txt", run);
        j.assertLogContains("F: a/aa/5.txt", run);
        j.assertLogContains("F: a/aa/6.txt", run);
        j.assertLogContains("F: a/ab/7.txt", run);
        j.assertLogContains("F: a/ab/8.txt", run);
        j.assertLogNotContains("F: a/ab/aba/9.txt", run);
        j.assertLogNotContains("F: a/ab/aba/10.txt", run);
        j.assertLogNotContains("F: b/11.txt", run);
        j.assertLogNotContains("F: b/12.txt", run);
    }
}
