/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 CloudBees Inc.
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
package org.jenkinsci.plugins.pipeline.utility.steps;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.graphanalysis.DepthFirstScanner;
import org.jenkinsci.plugins.workflow.graphanalysis.NodeStepTypePredicate;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class CompareVersionsStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testIsNewerThan() throws Exception {
        testExpressions(new TestDataBuilder()
                .newerThan("2.0.*", "2.0")
                .newerThan("2.1-SNAPSHOT", "2.0.*")
                .newerThan("2.1", "2.1-SNAPSHOT")
                .newerThan("2.0.*", "2.0.1")
                .newerThan("2.0.1", "2.0.1-SNAPSHOT")
                .newerThan("2.0.1-SNAPSHOT", "2.0.0.99")
                .newerThan("2.0.0.99", "2.0.0")
                .newerThan("2.0.0", "2.0.ea")
                .newerThan("2.0", "2.0.ea")
                .equals("2.0.0", "2.0")
                .data);
    }

    @Test
    public void testOlderThan() throws Exception {
        testExpressions(new TestDataBuilder()
                .olderThan("1.7", "1.8")
                .olderThan("1", "1.8")
                .equals("1.8", "1.8")
                .data);

    }

    private void testExpressions(List<TestDataBuilder.Data> testData) throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class, "compareTheVersions");
        StringBuilder str = new StringBuilder();
        for (TestDataBuilder.Data data : testData) {
            str.append(data.expression()).append('\n');
        }
        job.setDefinition(new CpsFlowDefinition(str.toString(), true));
        final WorkflowRun run = j.buildAndAssertSuccess(job);

        DepthFirstScanner scanner = new DepthFirstScanner();
        assertThat(scanner.filteredNodes(run.getExecution(), new NodeStepTypePredicate("compareVersions")), hasSize(testData.size()));
    }

    static class TestDataBuilder {
        static class Data {
            String v1;
            String v2;
            String test;

            String expression() {
                StringBuilder str = new StringBuilder("assert compareVersions(");
                str.append(" v1: '").append(v1).append("'");
                str.append(", v2: '").append(v2).append("'");
                str.append(") ").append(test);
                return str.toString();
            }
        }

        List<Data> data = new ArrayList<>();
        Data current = new Data();

        public TestDataBuilder v1(String v) {
            current.v1 = v;
            return this;
        }

        public TestDataBuilder v2(String v) {
            current.v2 = v;
            return this;
        }

        public TestDataBuilder olderThan(String v1, String v2) {
            return v1(v1).v2(v2).test("< 0");
        }

        public TestDataBuilder newerThan(String v1, String v2) {
            return v1(v1).v2(v2).test("> 0");
        }

        public TestDataBuilder equals(String v1, String v2) {
            return v1(v1).v2(v2).test("== 0");
        }

        public TestDataBuilder test(String expression) {
            current.test = expression;
            data.add(current);
            current = new Data();
            return this;
        }

    }
}