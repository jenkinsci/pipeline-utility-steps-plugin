/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Philippe GRANET
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

package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStep;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepDescriptorImpl;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepExecution;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.Yaml;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.constructor.SafeConstructor;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.reader.UnicodeReader;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;

/**
 * Reads a yaml file from the workspace.
 *
 * @author Philippe GRANET &lt;philippe.granet@gmail.com&gt;.
 */
public class ReadYamlStep extends AbstractFileOrTextStep {

	@DataBoundConstructor
	public ReadYamlStep() {
	}

	@Extension
	public static class DescriptorImpl extends AbstractFileOrTextStepDescriptorImpl {

		public DescriptorImpl() {
			super(Execution.class);
		}

		@Override
		public String getFunctionName() {
			return "readYaml";
		}

		@Override
		public String getDisplayName() {
			return "Read yaml from files in the workspace or text.";
		}
	}

	public static class Execution extends AbstractFileOrTextStepExecution<Object> {
		private static final long serialVersionUID = 1L;

		@StepContextParameter
		private transient TaskListener listener;

		@Inject
		private transient ReadYamlStep step;

		/**
		  * @return <ul>
		  * 	<li>Map&lt;String, Object&gt; if only one YAML document</li>
		  * 	<li>list of Map&lt;String, Object&gt; if multiple YAML document</li>
		 * 	</ul>
		 */
		@Override
		protected Object doRun() throws Exception {
			String yamlText = "";
			if (!isBlank(step.getFile())) {
				FilePath path = ws.child(step.getFile());
				if (!path.exists()) {
					throw new FileNotFoundException(path.getRemote() + " does not exist.");
				}
				if (path.isDirectory()) {
					throw new FileNotFoundException(path.getRemote() + " is a directory.");
				}
				
				// Generic unicode textreader, which will use BOM mark to identify the encoding
				// to be used. If BOM is not found then use a given default or system encoding.
				try(Reader reader=new UnicodeReader(path.read())){
					yamlText = IOUtils.toString(reader);
				};
			}
			if (!isBlank(step.getText())) {
				yamlText += System.getProperty("line.separator") + step.getText();
			}
			
			// Use SafeConstructor to limit objects to standard Java objects like List or Long
			Iterable<Object> yaml = new Yaml(new SafeConstructor()).loadAll(yamlText);
			
			List<Object> result = new LinkedList<Object>();
			for (Object data : yaml) {
				result.add(data);
			}
			
			// Ensure that result is serializable
			// Everything used in the pipeline needs to be Serializable
			try(ObjectOutputStream out=new ObjectOutputStream(new ByteArrayOutputStream())){
				out.writeObject(result);
			};
			
			// if only one YAML document, return it directly
			if (result.size() == 1) {
				return result.get(0);
			}
			
			return result;
		}
	}
}