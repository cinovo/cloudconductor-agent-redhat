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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.cinovo.cloudconductor.agent.helper.AgentVars;
import de.cinovo.cloudconductor.api.model.AgentOption;
import de.taimos.daemon.DaemonStarter;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 *
 * @author psigloch
 */
public class AgentState {
	
	/**
	 * a write lock for package jobs
	 */
	public static final Lock packageExecutionLock = new ReentrantLock();
	/**
	 * a write lock for file jobs
	 */
	public static final Lock filesExecutionLock = new ReentrantLock();
	/**
	 * a write lock for repo changes
	 */
	public static final Lock repoExecutionLock = new ReentrantLock();
	
	private static AgentState instance;
	private static VelocityContext velocityContext;
	private static AgentOption options;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private String cloudconductor;
	private String jwt;
	private Set<String> repos = new HashSet<>();
	
	
	private AgentState() {
		this.initCloudConductorServer();
	}
	
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
	
	/**
	 * @return the options
	 */
	public static AgentOption getOptions() {
		return AgentState.options;
	}
	
	/**
	 * @param options the options to set
	 */
	public static void setOptions(AgentOption options) {
		AgentState.options = options;
		AgentState.info().updateTemplate(options.getTemplateName());
		AgentState.info().updateUuid(options.getUuid());
	}
	
	private void initCloudConductorServer() {
		this.cloudconductor = System.getProperty(AgentVars.CLOUDCONDUCTOR_URL_PROP);
		if (this.cloudconductor.endsWith("/")) {
			this.cloudconductor = this.cloudconductor.substring(0, this.cloudconductor.length() - 1);
		}
		
		String protocol = System.getProperty(AgentVars.COMMUNICATION_PROTOCOL, AgentVars.COMMUNICATION_PROTOCOL_DEFAULT);
		if (protocol.equalsIgnoreCase("https")) {
			protocol = "https://";
		} else {
			protocol = "http://";
		}
		
		if (!this.cloudconductor.startsWith(protocol)) {
			this.cloudconductor = protocol + this.cloudconductor;
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
	 * @return uuid
	 */
	public String getUuid() {
		if ((System.getProperty(AgentVars.UUID_PROP) == null) || System.getProperty(AgentVars.UUID_PROP).trim().isEmpty()) {
			return AgentState.info().getHost();
		}
		return System.getProperty(AgentVars.UUID_PROP);
	}
	
	/**
	 * @return the config server api url
	 */
	public String getServer() {
		return this.cloudconductor;
	}
	
	/**
	 * @return the agent name of this agent
	 */
	public String getAgent() {
		return System.getProperty(AgentVars.AGENT_PROP, this.getHost());
	}
	
	/**
	 * @return the token for authentication
	 */
	public String getToken() {
		return System.getProperty(AgentVars.TOKEN_PROP, null);
	}
	
	/**
	 * @return the jwt to use
	 */
	public String getJWT() {
		return this.jwt;
	}
	
	/**
	 * @param newJWT the new JWT to set
	 */
	public void setJWT(String newJWT) {
		this.jwt = newJWT;
	}
	
	/**
	 * @return the repos
	 */
	public Set<String> getRepos() {
		return this.repos;
	}
	
	/**
	 * @param repos the repos to set
	 */
	public void setRepos(Set<String> repos) {
		this.repos = repos;
	}
	
	/**
	 * updates the templatename
	 *
	 * @param templateName the name of the template
	 */
	public void updateTemplate(String templateName) {
		Charset charset = StandardCharsets.UTF_8;
		if (this.getTemplate().equals(templateName)) {
			return;
		}
		System.setProperty(AgentVars.TEMPLATE_PROP, templateName);
		try {
			Path path = Paths.get("/opt/cloudconductor-agent/cloudconductor-agent.properties");
			
			String content = new String(Files.readAllBytes(path), charset);
			content = content.replaceAll("TEMPLATE_NAME=", "TEMPLATE_NAME=" + templateName);
			Files.write(path, content.getBytes(charset));
		} catch (IOException ex) {
			this.logger.error("Failed to write template name to cloudconductor-agent.properties", ex);
		}
	}
	
	/**
	 * update host uuid
	 *
	 * @param uuid of the host
	 */
	public void updateUuid(String uuid) {
		if (this.getUuid() != null && this.getUuid().equals(uuid)) {
			return;
		}
		System.setProperty(AgentVars.UUID_PROP, uuid);
		try {
			Path path = Paths.get("/opt/cloudconductor-agent/cloudconductor-agent.properties");
			
			Charset charset = StandardCharsets.UTF_8;
			String content = new String(Files.readAllBytes(path), charset);
			if (content.contains(AgentVars.UUID_PROP)) {
				content = content.replaceAll(AgentVars.UUID_PROP + "=.*", AgentVars.UUID_PROP + "=" + uuid);
			} else {
				content = content + "export " + AgentVars.UUID_PROP + "=" + uuid + "\n";
			}
			Files.write(path, content.getBytes(charset));
		} catch (IOException ex) {
			this.logger.error("Failed to write uuid to cloudconductor-agent.properties", ex);
		}
	}
	
}
