package de.cinovo.cloudconductor.agent.executors;

/*
 * #%L
 * Node Agent for cloudconductor framework
 * %%
 * Copyright (C) 2013 - 2014 Cinovo AG
 * %%
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * #L%
 */

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.executors.helper.AbstractExecutor;
import de.cinovo.cloudconductor.agent.helper.AgentVars;
import de.cinovo.cloudconductor.api.model.PackageVersion;
import de.cinovo.cloudconductor.api.model.Service;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 */
public class ScriptExecutor extends AbstractExecutor<String> {
	
	/**
	 * @param remove packages to remove, separated by semicolon
	 * @param install packages to install, separated by semicolon
	 * @param update packages to update, separated by semicolon
	 * @return the executor
	 */
	public static ScriptExecutor generatePackageHandler(Collection<PackageVersion> remove, Collection<PackageVersion> install, Collection<PackageVersion> update) {
		String scriptName = AgentVars.SCRIPT_YUM_HANDLER;
		String repoArg = "-y " + System.getProperty(AgentVars.YUM_NAME_PROP);
		StringBuilder d = new StringBuilder();
		StringBuilder i = new StringBuilder();
		StringBuilder u = new StringBuilder();
		if ((remove != null) && !remove.isEmpty()) {
			d.append("-d ");
			for (PackageVersion pck : remove) {
				d.append(ScriptExecutor.rpmToString(pck));
				d.append(";");
			}
		}
		
		if ((install != null) && !install.isEmpty()) {
			i.append("-i ");
			for (PackageVersion pck : install) {
				i.append(ScriptExecutor.rpmToString(pck));
				i.append(";");
			}
		}
		
		if ((update != null) && !update.isEmpty()) {
			u.append("-u ");
			for (PackageVersion pck : update) {
				u.append(ScriptExecutor.rpmToString(pck));
				u.append(";");
			}
		}
		return new ScriptExecutor(scriptName, repoArg, d.toString(), i.toString(), u.toString());
	}
	
	/**
	 * @param restart services to restart
	 * @param start services to start
	 * @param stop services to stops
	 * @return the executor
	 */
	public static ScriptExecutor generateServiceStateHandler(Collection<String> restart, Collection<String> start, Collection<String> stop) {
		String scriptName = AgentVars.SCRIPT_SERVICE_HANDLER;
		StringBuilder r = new StringBuilder();
		StringBuilder s = new StringBuilder();
		StringBuilder u = new StringBuilder();
		if ((start != null) && !start.isEmpty()) {
			r.append("-r ");
			for (String service : start) {
				r.append(service);
				r.append(";");
			}
		}
		
		if ((stop != null) && !stop.isEmpty()) {
			s.append("-s ");
			for (String service : stop) {
				s.append(service);
				s.append(";");
			}
		}
		
		if ((restart != null) && !restart.isEmpty()) {
			u.append("-u ");
			for (String service : restart) {
				u.append(service);
				u.append(";");
			}
		}
		return new ScriptExecutor(scriptName, r.toString(), s.toString(), u.toString());
	}
	
	/**
	 * @param services the services
	 * @return the executor
	 */
	public static ScriptExecutor generateCheckServiceState(Collection<Service> services) {
		String scriptName = AgentVars.SCRIPT_SERVICE_STATE;
		StringBuilder cmd = new StringBuilder();
		if ((services != null) && !services.isEmpty()) {
			for (Service svc : services) {
				cmd.append(svc.getInitScript());
				cmd.append(" ");
			}
		}
		return new ScriptExecutor(scriptName, cmd.toString().trim());
	}
	
	private static String rpmToString(PackageVersion rpm) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(rpm.getName());
		buffer.append("-");
		buffer.append(rpm.getVersion());
		return buffer.toString();
	}
	
	
	private String script;
	private String[] args;
	private String result;
	
	
	/**
	 * @param script the script name
	 * @param args arguments to pass to the script
	 */
	private ScriptExecutor(String script, String... args) {
		this.script = script;
		this.args = args;
	}
	
	@Override
	protected Process genProcess() throws IOException {
		File scriptPath = new File(AgentVars.SCRIPTFOLDER + this.script);
		if (!scriptPath.exists()) {
			throw new IOException("The script " + this.script + " couldn't be found");
		}
		StringBuilder scriptBuilder = new StringBuilder();
		scriptBuilder.append(scriptPath.getAbsolutePath());
		for (String s : this.args) {
			scriptBuilder.append(" ");
			scriptBuilder.append(s.trim());
		}
		return Runtime.getRuntime().exec(scriptBuilder.toString());
	}
	
	@Override
	protected void analyzeStream(String[] dev, String[] error) throws ExecutionError {
		StringBuilder devString = new StringBuilder();
		for (String d : dev) {
			devString.append(d);
			devString.append(System.lineSeparator());
		}
		this.result = devString.toString().trim();
		
		StringBuilder errorString = new StringBuilder();
		for (String e : error) {
			errorString.append(e);
			errorString.append(System.lineSeparator());
		}
		if (!errorString.toString().trim().isEmpty()) {
			throw new ExecutionError(errorString.toString().trim());
		}
	}
	
	@Override
	public String getResult() {
		return this.result;
	}
	
}
