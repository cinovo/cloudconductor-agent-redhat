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
import java.util.Map;
import java.util.Set;

import de.cinovo.cloudconductor.api.lib.manager.DirectoryHandler;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import de.cinovo.cloudconductor.agent.AgentState;
import de.cinovo.cloudconductor.agent.exceptions.TransformationErrorException;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.lib.manager.AgentHandler;
import de.cinovo.cloudconductor.api.lib.manager.ConfigFileHandler;
import de.cinovo.cloudconductor.api.lib.manager.ConfigValueHandler;
import de.cinovo.cloudconductor.api.model.AgentOptions;
import de.cinovo.cloudconductor.api.model.ConfigFile;
import de.cinovo.cloudconductor.api.model.PackageState;
import de.cinovo.cloudconductor.api.model.PackageStateChanges;
import de.cinovo.cloudconductor.api.model.SSHKey;
import de.cinovo.cloudconductor.api.model.Service;
import de.cinovo.cloudconductor.api.model.ServiceStates;
import de.cinovo.cloudconductor.api.model.ServiceStatesChanges;
import de.cinovo.cloudconductor.api.model.Template;
import de.cinovo.cloudconductor.api.model.Directory;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 *
 * @author psigloch
 *
 */
public class ServerCom {

	private static final AgentHandler agent = new AgentHandler(AgentState.info().getServer());
	private static final ConfigValueHandler config = new ConfigValueHandler(AgentState.info().getServer());
	private static final ConfigFileHandler file = new ConfigFileHandler(AgentState.info().getServer());
	private static final DirectoryHandler directory = new DirectoryHandler(AgentState.info().getServer());


	private ServerCom() {
		// prevent instantiation
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
		try {
			return ServerCom.config.getConfig(AgentState.info().getTemplate(), AgentVars.SERVICE_NAME);
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
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
	 * @param req the service update req
	 * @return the response
	 * @throws CloudConductorException thrown if communication with cloudconductor failed
	 */
	public static ServiceStatesChanges notifyRunningServices(ServiceStates req) throws CloudConductorException {
		try {
			String template = AgentState.info().getTemplate();
			String host = AgentState.info().getHost();
			return ServerCom.agent.notifyServiceState(template, host, req);
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
			return ServerCom.agent.notifyPackageState(template, host, installedPackages);
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
	}

	/**
	 * @return the response
	 * @throws CloudConductorException thrown if communication with cloudconductor failed
	 */
	public static AgentOptions heartBeat() throws CloudConductorException {
		try {
			String template = AgentState.info().getTemplate();
			String host = AgentState.info().getHost();
			return ServerCom.agent.heartBeat(template, host);
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}
	}

	/**
	 * @return the response
	 * @throws CloudConductorException thrown if communication with cloudconductor failed
	 */
	public static Set<Directory> getDirectories() throws CloudConductorException {
		try{
			String template = AgentState.info().getTemplate();
			return ServerCom.directory.getDirectoryByTemplate(template);
		} catch (RuntimeException e) {
			throw new CloudConductorException(e.getMessage());
		}

	}

	/**
	 *
	 * @param dirName
	 * @return directory "filemode"
	 * @throws CloudConductorException
	 */
	public static String getDirectoryMode(String dirName) throws CloudConductorException {
		try {
			return ServerCom.agent.getDirectoryFileMode(dirName);
		} catch (RuntimeException e){
			throw new CloudConductorException(e.getMessage());
		}
	}

	/**
	 *
	 * @param fileName
	 * @return get file filemode
	 * @throws CloudConductorException
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
