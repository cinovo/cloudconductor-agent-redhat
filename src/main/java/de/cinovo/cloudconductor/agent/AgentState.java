package de.cinovo.cloudconductor.agent;

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

import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.velocity.VelocityContext;

import de.cinovo.cloudconductor.agent.helper.AgentVars;
import de.cinovo.cloudconductor.api.model.AgentOptions;
import de.taimos.daemon.DaemonStarter;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 *
 * @author psigloch
 *
 */
public class AgentState {
	
	/** a write lock for jobs */
	public static final Lock executionLock = new ReentrantLock();
	
	private static AgentState instance;
	private static VelocityContext velocityContext;
	private static AgentOptions options;


	/**
	 * @return the agent state instance
	 */
	public static AgentState info() {
		if (AgentState.instance == null) {
			AgentState.instance = new AgentState();
		}
		return AgentState.instance;
	}
	
	/**
	 * @return the global velocity context
	 */
	public static VelocityContext vContext() {
		if (AgentState.velocityContext == null) {
			AgentState.velocityContext = new VelocityContext();
			for (Entry<String, String> entry : System.getenv().entrySet()) {
				AgentState.velocityContext.put(entry.getKey(), entry.getValue());
			}
			for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
				AgentState.velocityContext.put((String) entry.getKey(), entry.getValue());
			}
		}
		return AgentState.velocityContext;
	}
	
	
	private String cloudconductor;
	
	
	private AgentState() {
		this.initCloudConductorServer();
	}
	
	private void initCloudConductorServer() {
		this.cloudconductor = System.getProperty(AgentVars.CLOUDCONDUCTOR_URL_PROP);
		if (this.cloudconductor.endsWith("/")) {
			this.cloudconductor = this.cloudconductor.substring(0, this.cloudconductor.length() - 1);
		}
		if (!this.cloudconductor.startsWith("http://")) {
			this.cloudconductor = "http://" + this.cloudconductor;
		}
		if (!this.cloudconductor.endsWith(AgentVars.CLOUDCONDUCTOR_API_PATH)) {
			this.cloudconductor = this.cloudconductor + AgentVars.CLOUDCONDUCTOR_API_PATH;
		}
	}
	
	/**
	 * @return the host name
	 */
	public String getHost() {
		return DaemonStarter.getHostname();
	}
	
	/**
	 * @return the template name
	 */
	public String getTemplate() {
		return System.getProperty(AgentVars.TEMPLATE_PROP);
	}
	
	/**
	 * @return the config server api url
	 */
	public String getServer() {
		return this.cloudconductor;
	}

	/**
	 * @return the options
	 */
	public static AgentOptions getOptions() {
		return AgentState.options;
	}

	/**
	 * @param options the options to set
	 */
	public static void setOptions(AgentOptions options) {
		AgentState.options = options;
	}
}
