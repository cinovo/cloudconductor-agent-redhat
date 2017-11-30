package de.cinovo.cloudconductor.agent.jobs.handler.api;

/*
 * #%L
 * cloudconductor-api
 * %%
 * Copyright (C) 2013 - 2014 Cinovo AG
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * #L%
 */

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cinovo.cloudconductor.api.IRestPath;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.AgentOption;
import de.cinovo.cloudconductor.api.model.Authentication;
import de.cinovo.cloudconductor.api.model.ConfigFile;
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
 * @author psigloch
 * 
 */
public class AgentHandler extends AbstractApiHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractApiHandler.class);
	
	
	/**
	 * @param cloudconductorUrl the config server url
	 */
	public AgentHandler(String cloudconductorUrl) {
		super(cloudconductorUrl);
	}
	
	/**
	 * @param authToken the authentication token to use
	 * @return the JWT
	 * @throws CloudConductorException Error indicating connection or data problems
	 */
	public String getJWT(String authToken) throws CloudConductorException {
		return this._put("/auth", new Authentication(authToken), String.class);
	}
	
	/**
	 * @param template the template name
	 * @param host the host name
	 * @param state the package state
	 * @param uuid the UUID of the agent
	 * @return changes to the package state
	 * @throws CloudConductorException Error indicating connection or data problems
	 */
	public PackageStateChanges notifyPackageState(String template, String host, PackageState state, String uuid) throws CloudConductorException {
		String path = this.pathGenerator(IRestPath.AGENT + IRestPath.AGENT_PACKAGE_STATE, template, host, uuid);
		return this._put(path, state, PackageStateChanges.class);
	}
	
	/**
	 * @param template the template name
	 * @param host the host name
	 * @param state the package state
	 * @param uuid the UUID of the agent
	 * @return changes to the service state
	 * @throws CloudConductorException Error indicating connection or data problems
	 */
	public ServiceStatesChanges notifyServiceState(String template, String host, ServiceStates state, String uuid) throws CloudConductorException {
		String path = this.pathGenerator(IRestPath.AGENT + IRestPath.AGENT_SERVICE_STATE, template, host, uuid);
		return this._put(path, state, ServiceStatesChanges.class);
	}
	
	/**
	 * @param configFilename the name of the config file
	 * @return the data of the config file
	 * @throws CloudConductorException Error indicating connection or data problems
	 */
	public String getConfigFileData(String configFilename) throws CloudConductorException {
		String path = this.pathGenerator(IRestPath.FILE + IRestPath.FILE_DATA, configFilename);
		return this._get(path, String.class);
	}
	
	/**
	 *
	 * @param fileName the name of the file or directory
	 * @return file file mode the file mode of the given file or directory
	 * @throws CloudConductorException Error indicating connection or data problems
	 */
	public String getFileFileMode(String fileName) throws CloudConductorException {
		String path = this.pathGenerator(IRestPath.FILE + IRestPath.FILE_DETAILS, fileName);
		ConfigFile file = this._get(path, ConfigFile.class);
		return file.getFileMode();
	}
	
	/**
	 * @param template the template name
	 * @return the template
	 * @throws CloudConductorException Error indicating connection or data problems
	 */
	public Template getTemplate(String template) throws CloudConductorException {
		String path = this.pathGenerator(IRestPath.TEMPLATE + IRestPath.DEFAULT_NAME, template);
		
		return this._get(path, Template.class);
	}
	
	/**
	 * @param template the template name
	 * @return the services of the template
	 * @throws CloudConductorException Error indicating connection or data problems
	 */
	// TODO update path
	@SuppressWarnings("unchecked")
	public Set<Service> getServices(String template) throws CloudConductorException {
		String path = this.pathGenerator(IRestPath.TEMPLATE + IRestPath.TEMPLATE_SERVICE, template);
		return (Set<Service>) this._get(path, this.getSetType(Service.class));
	}
	
	/**
	 * @param template the template name
	 * @return the ssh keys of the template
	 * @throws CloudConductorException Error indicating connection or data problems
	 */
	@SuppressWarnings("unchecked")
	public Set<SSHKey> getSSHKeys(String template) throws CloudConductorException {
		String path = this.pathGenerator(IRestPath.TEMPLATE + IRestPath.TEMPLATE_SSHKEY, template);
		AgentHandler.LOGGER.info("Get SSH keys from '" + path + "'...");
		Object result = this._get(path, this.getSetType(SSHKey.class));
		return (Set<SSHKey>) result;
	}
	
	/**
	 * @return collection of alive host names
	 * @throws CloudConductorException Error indicating connection or data problems
	 */
	@SuppressWarnings("unchecked")
	// TODO needed?
	public Set<String> getAliveAgents() throws CloudConductorException {
		String path = this.pathGenerator(IRestPath.AGENT);
		return (Set<String>) this._get(path, this.getSetType(String.class));
	}
	
	/**
	 * @param template the template name
	 * @param host the host name
	 * @param agent the name of the agent
	 * @param uuid the UUID of the agent
	 * @return the agent options of the template
	 * @throws CloudConductorException Error indicating connection or data problems
	 */
	public AgentOption heartBeat(String template, String host, String agent, String uuid) throws CloudConductorException {
		String path = this.pathGenerator(IRestPath.AGENT + IRestPath.AGENT_HEART_BEAT, template, host, agent, uuid);
		AgentHandler.LOGGER.info("Send heartbeat to '" + path + "'");
		return this._get(path, AgentOption.class);
	}
	
	/**
	 * @return collection of alive host names
	 * @throws CloudConductorException Error indicating connection or data problems
	 */
	public Boolean isServerAlive() throws CloudConductorException {
		String path = this.pathGenerator(IRestPath.AGENT + IRestPath.AGENT_PING);
		return this._get(path, Boolean.class);
	}
	
	/**
	 * @param templateName the name of the template
	 * @return set of repos for the given template
	 * @throws CloudConductorException Error indicating connection or data problems
	 */
	@SuppressWarnings("unchecked")
	public Set<Repo> getRepos(String templateName) throws CloudConductorException {
		String path = this.pathGenerator(IRestPath.TEMPLATE + IRestPath.TEMPLATE_REPO, templateName);
		return (Set<Repo>) this._get(path, this.getSetType(Repo.class));
	}
}
