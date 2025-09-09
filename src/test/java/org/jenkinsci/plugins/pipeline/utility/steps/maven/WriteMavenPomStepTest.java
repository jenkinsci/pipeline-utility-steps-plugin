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

package org.jenkinsci.plugins.pipeline.utility.steps.maven;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.Label;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import jenkins.util.VirtualFile;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Tests for {@link WriteMavenPomStep} and {@link ReadMavenPomStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
@WithJenkins
class WriteMavenPomStepTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    void testWriteAndRead() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(
                new CpsFlowDefinition( // checks the auto import
                        """
                        def doWrite() {
                          Model pom = new Model()
                          pom.artifactId = 'my-test-project'
                          pom.groupId = 'com.example.jenkins.test'
                          pom.version = '1.1-SNAPSHOT'
                          Dependency d = new Dependency()
                          d.artifactId = 'pipeline-utility-steps'
                          d.groupId = 'org.jenkins-ci.plugins'
                          d.version = '1.0'
                          d.classifier = 'hpi'
                          pom.addDependency(d)
                          writeMavenPom(pom)
                        }
                        def doRead() {
                          Model m = readMavenPom file: 'inhere/pom.xml'
                          assert m.artifactId == 'my-test-project'
                          assert m.groupId == 'com.example.jenkins.test'
                          assert m.version == '1.1-SNAPSHOT'
                          Dependency dd = m.dependencies.get(0)
                          assert dd.artifactId == 'pipeline-utility-steps'
                          assert dd.groupId == 'org.jenkins-ci.plugins'
                          assert dd.version == '1.0'
                          assert dd.classifier == 'hpi'
                          result = "success"
                        }
                        node('slaves') {
                          dir('inhere') {
                            doWrite()
                          }

                          String result = "failed"
                          doRead()
                          archive '**/pom.xml'
                        }""",
                        true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        VirtualFile file = run.getArtifactManager().root().child("inhere/pom.xml");
        assertNotNull(file);
        assertTrue(file.exists() && file.canRead());
        Document doc = readXml(file.open());
        assertThat(doc, hasXPath("project/artifactId", equalTo("my-test-project")));
    }

    private Document readXml(InputStream input) throws ParserConfigurationException, IOException, SAXException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
    }
}
