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

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 */
public interface AgentVars {
	
	/** the name of the service */
	String SERVICE_NAME = "cloudconductor-agent";
	
	/**
	 * The wait time for server connection retries at start up
	 */
	long WAIT_FOR_SERVER = 5000;
	
	/**
	 * property for the server url
	 */
	String CLOUDCONDUCTOR_URL_PROP = "CLOUDCONDUCTOR_URL";
	/**
	 * additional url path for api access of server
	 */
	String CLOUDCONDUCTOR_API_PATH = "/api";
	
	/**
	 * property for the template name
	 */
	String TEMPLATE_PROP = "TEMPLATE_NAME";
	
	/**
	 * property for the agent name
	 */
	String AGENT_PROP = "AGENT_NAME";
	
	/**
	 * property for the authentication token
	 */
	String TOKEN_PROP = "AUTH_TOKEN";
	/**
	 * property for the yum repo name
	 */
	String YUM_NAME_PROP = "nodeagent.repo.name";
	/**
	 * derfault yum repo name
	 */
	String YUM_NAME_PROP_DEFAULT = "cinovo";
	
	/**
	 * system path for yum repos
	 */
	String YUM_REPO_FOLDER = "/etc/yum.repos.d/";
	/**
	 * file ending for yum repo definition files
	 */
	String YUM_REPO_ENDING = ".repo";
	
	/**
	 * service state script
	 */
	String SCRIPT_SERVICE_STATE = "serviceState.sh";
	/**
	 * service handler script
	 */
	String SCRIPT_SERVICE_HANDLER = "serviceHandler.sh";
	/**
	 * yum handler script
	 */
	String SCRIPT_YUM_HANDLER = "yumHandler.sh";
	/**
	 * relative path to the script folder
	 */
	String SCRIPTFOLDER = "scripts/";

	/**
	 * property for agent / host uuid
	 */
	String UUID_PROP = "UUID";
	/**
	 * property for agent communication protocol
	 */
	String COMMUNICATION_PROTOCOL = "COM_PROTOCOL";
	/**
	 * default agent communication protocol
	 */
	String COMMUNICATION_PROTOCOL_DEFAULT = "http";

}
