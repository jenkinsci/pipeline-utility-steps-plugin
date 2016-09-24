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
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.Yaml;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.constructor.SafeConstructor;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.reader.UnicodeReader;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;

/**
 * Reads a yaml file from the workspace.
 *
 * @author Philippe GRANET &lt;philippe.granet@gmail.com&gt;.
 */
public class ReadYamlStep extends AbstractStepImpl {

	private String file;
	private String text;

	@DataBoundConstructor
	public ReadYamlStep() {
	}

	/**
	 * Name of the yaml file to read.
	 *
	 * @return file name
	 */
	public String getFile() {
		return file;
	}

	/**
	 * Name of the yaml file to read.
	 *
	 * @param file
	 *            file name
	 */
	@DataBoundSetter
	public void setFile(String file) {
		this.file = file;
	}

	/**
	 * A String containing properties formatted data.
	 *
	 * @return text to parse
	 */
	public String getText() {
		return text;
	}

	/**
	 * A String containing properties formatted data.
	 *
	 * @param text
	 *            text to parse
	 */
	@DataBoundSetter
	public void setText(String text) {
		this.text = text;
	}

	@Extension
	public static class DescriptorImpl extends AbstractStepDescriptorImpl {

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

		@Override
		public Step newInstance(Map<String, Object> arguments) throws Exception {
			ReadYamlStep step = new ReadYamlStep();
			if (arguments.containsKey("file")) {
				Object file = arguments.get("file");
				if (file != null) {
					step.setFile(file.toString());
				}
			}
			if (arguments.containsKey("text")) {
				Object text = arguments.get("text");
				if (text != null) {
					step.setText(text.toString());
				}
			}
			if (isBlank(step.getFile()) && isBlank(step.getText())) {
				throw new IllegalArgumentException("At least one of file or text needs to be provided to readYaml.");
			}
			return step;
		}
	}

	public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Object> {
		private static final long serialVersionUID = 1L;

		@StepContextParameter
		private transient TaskListener listener;

		@StepContextParameter
		private transient FilePath ws;

		@Inject
		private transient ReadYamlStep step;

		/**
		  * @return 
		  * 	- Map<String, Object> if only one YAML document
		  * 	- list of Map<String, Object> if multiple YAML document
		 */
		@Override
		protected Object run() throws Exception {
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