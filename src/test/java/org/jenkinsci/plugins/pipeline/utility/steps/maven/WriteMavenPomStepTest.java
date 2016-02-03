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

import hudson.model.Label;
import jenkins.util.VirtualFile;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.Assert.*;

/**
 * Tests for {@link WriteMavenPomStep} and {@link ReadMavenPomStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class WriteMavenPomStepTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() throws Exception {
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    public void testWriteAndRead() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('inhere') {\n" +
                        "    Model pom = new Model()\n" + //checks the auto import
                        "    pom.artifactId = 'my-test-project'\n" +
                        "    pom.groupId = 'com.example.jenkins.test'\n" +
                        "    pom.version = '1.1-SNAPSHOT'\n" +
                        "    Dependency d = new Dependency()\n" +
                        "    d.artifactId = 'pipeline-utility-steps'\n" +
                        "    d.groupId = 'org.jenkins-ci.plugins'\n" +
                        "    d.version = '1.0'\n" +
                        "    d.classifier = 'hpi'\n" +
                        "    pom.addDependency(d)\n" +
                        "    writeMavenPom(pom)\n" +
                        "  }\n" +
                        "  Model m = readMavenPom file: 'inhere/pom.xml'\n" +
                        "  assert m.artifactId == 'my-test-project'\n" +
                        "  assert m.groupId == 'com.example.jenkins.test'\n" +
                        "  assert m.version == '1.1-SNAPSHOT'\n" +
                        "  Dependency dd = m.dependencies.get(0)\n" +
                        "  assert dd.artifactId == 'pipeline-utility-steps'\n" +
                        "  assert dd.groupId == 'org.jenkins-ci.plugins'\n" +
                        "  assert dd.version == '1.0'\n" +
                        "  assert dd.classifier == 'hpi'\n" +
                        "  archive '**/pom.xml'\n" +
                        "}", true)); //Update of sandbox dependency seems to have fixed the whitelist issue.
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