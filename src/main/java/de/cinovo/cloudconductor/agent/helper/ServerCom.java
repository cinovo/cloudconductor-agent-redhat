package de.cinovo.cloudconductor.agent.helper;

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
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cinovo.cloudconductor.agent.AgentState;
import de.cinovo.cloudconductor.agent.exceptions.TransformationErrorException;
import de.cinovo.cloudconductor.agent.jobs.handler.api.AgentHandler;
import de.cinovo.cloudconductor.agent.jobs.handler.api.ConfigFileHandler;
import de.cinovo.cloudconductor.agent.jobs.handler.api.ConfigValueHandler;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.AgentOption;
import de.cinovo.cloudconductor.api.model.ConfigFile;
import de.cinovo.cloudconductor.api.model.ConfigValue;
import de.cinovo.cloudconductor.api.model.PackageState;
import de.cinovo.cloudconductor.api.model.PackageStateChanges;
import de.cinovo.cloudconductor.api.model.Repo;
import de.cinovo.cloudconductor.api.model.SSHKey;
import de.cinovo.cloudconductor.api.model.Service;
import de.cinovo.cloudconductor.api.model.ServiceStates;
import de.cinovo.cloudconductor.api.model.ServiceStatesChanges;
import de.cinovo.cloudconductor.api.model.Template;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch, ablehm
 * 
 */
public class ServerCom {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ServerCom.class);
	
	private static final AgentHandler agent = new AgentHandler(AgentState.info().getServer());
	private static final ConfigValueHandler config = new ConfigValueHandler(AgentState.info().getServer(), AgentState.info().getToken(), AgentState.info().getAgent());
	private static final ConfigFileHandler file = new ConfigFileHandler(AgentState.info().getServer(), AgentState.info().getToken(), AgentState.info().getAgent());
	
	
	private ServerCom() {
		// prevent instantiation
	}
	
	/**
	 * @return the new JWT
	 * @throws CloudConductorException if retrieval fails
	 */
	public static String getJWT() throws CloudConductorException {
		return ServerCom.agent.getJWT(AgentState.info().getToken());
	}
	
	/**
	 * @return the template for this agent
	 * @throws CloudConductorException error if retrieval fails
	 */
	public static Template getTemplate() throws CloudConductorException {
		try {
			return ServerCom.agent.getTemplate(AgentState.info().getTemplate());
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
	}
	
	/**
	 * @return the config for this agent
	 * @throws CloudConductorException error if retrieval fails
	 */
	public static Map<String, String> getConfig() throws CloudConductorException {
		Map<String, String> configMap = new HashMap<>();
		
		try {
			for (ConfigValue c : ServerCom.config.getConfig(AgentState.info().getTemplate(), AgentVars.SERVICE_NAME)) {
				if ((c.getService() != null) && c.getService().equals(AgentVars.SERVICE_NAME)) {
					ServerCom.LOGGER.info("Add config: " + c.getKey() + ": " + c.getValue());
					configMap.put(c.getKey(), (String) c.getValue());
				}
			}
		} catch (RuntimeException e) {
			ServerCom.LOGGER.error("Error getting config from server: ", e);
			throw new CloudConductorException(e.getMessage());
		}
		
		return configMap;
	}
	
	/**
	 * @return the yum repo path
	 * @throws CloudConductorException error if retrieval fails
	 */
	public static String getYumPath() throws CloudConductorException {
		try {
			Template template = ServerCom.getTemplate();
			return template.getYum();
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
	}
	
	/**
	 * @return the services of the host
	 * @throws CloudConductorException thrown if communication with cloudconductor failed
	 */
	public static Set<Service> getServices() throws CloudConductorException {
		try {
			String template = AgentState.info().getTemplate();
			return ServerCom.agent.getServices(template);
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
	}
	
	/**
	 * @param cf the file
	 * @return the data
	 * @throws CloudConductorException thrown if communication with cloudconductor failed
	 * @throws TransformationErrorException error on generating the localized config file
	 */
	public static String getFileData(ConfigFile cf) throws CloudConductorException, TransformationErrorException {
		try {
			String content = ServerCom.agent.getConfigFileData(cf.getName());
			content = content.replaceAll("\\r\\n", "\n");
			content = content.replaceAll("\\r", "\n");
			if (!cf.isTemplate()) {
				return content;
			}
			StringWriter w = new StringWriter();
			try {
				Velocity.evaluate(AgentState.vContext(), w, "configfileGen", content);
			} catch (ParseErrorException | MethodInvocationException | ResourceNotFoundException | IOException e) {
				throw new TransformationErrorException("Failed to generate template", e);
			}
			return w.toString();
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
	}
	
	/**
	 * @return the ssh keys
	 * @throws CloudConductorException thrown if communication with cloudconductor failed
	 */
	public static Set<SSHKey> getSSHKeys() throws CloudConductorException {
		try {
			String template = AgentState.info().getTemplate();
			return ServerCom.agent.getSSHKeys(template);
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
	}
	
	/**
	 * 
	 * @return set of repos for current template
	 * @throws CloudConductorException thrown if communication with cloudconductor failed
	 */
	public static Set<Repo> getRepos() throws CloudConductorException {
		try {
			String template = AgentState.info().getTemplate();
			return ServerCom.agent.getRepos(template);
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
	}
	
	/**
	 * @param req the service update req
	 * @return the response
	 * @throws CloudConductorException thrown if communication with cloudconductor failed
	 */
	public static ServiceStatesChanges notifyRunningServices(ServiceStates req) throws CloudConductorException {
		try {
			String template = AgentState.info().getTemplate();
			String host = AgentState.info().getHost();
			String uuid = AgentState.info().getUuid();
			return ServerCom.agent.notifyServiceState(template, host, req, uuid);
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
	}
	
	/**
	 * @param installedPackages the installed packages
	 * @return the response
	 * @throws CloudConductorException thrown if communication with cloudconductor failed
	 */
	public static PackageStateChanges notifyInstalledPackages(PackageState installedPackages) throws CloudConductorException {
		try {
			String template = AgentState.info().getTemplate();
			String host = AgentState.info().getHost();
			String uuid = AgentState.info().getUuid();
			return ServerCom.agent.notifyPackageState(template, host, installedPackages, uuid);
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
	}
	
	/**
	 * @return the response
	 * @throws CloudConductorException thrown if communication with cloudconductor failed
	 */
	public static AgentOption heartBeat() throws CloudConductorException {
		try {
			String template = AgentState.info().getTemplate();
			String host = AgentState.info().getHost();
			String uuid = AgentState.info().getUuid();
			String agentName = AgentState.info().getAgent();
			return ServerCom.agent.heartBeat(template, host, agentName, uuid);
		} catch (RuntimeException e) {
			ServerCom.LOGGER.error("Error sending heart beat: ", e);
			throw new CloudConductorException(e.getMessage());
		}
	}
	
	/**
	 *
	 * @param fileName the file name
	 * @return get file filemode
	 * @throws CloudConductorException thrown if communication with cloudconductor failed
	 */
	public static String getFileMode(String fileName) throws CloudConductorException {
		try {
			return ServerCom.agent.getFileFileMode(fileName);
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
	}
	
	/**
	 * @return the response
	 * @throws CloudConductorException thrown if communication with cloudconductor failed
	 */
	public static Set<ConfigFile> getFiles() throws CloudConductorException {
		try {
			String template = AgentState.info().getTemplate();
			return ServerCom.file.getConfigFilesByTemplate(template);
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
	}
	
	/**
	 * @return the response
	 * @throws CloudConductorException thrown if communication with cloudconductor failed
	 */
	public static boolean isServerAlive() throws CloudConductorException {
		try {
			return ServerCom.agent.isServerAlive();
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
	}
}
