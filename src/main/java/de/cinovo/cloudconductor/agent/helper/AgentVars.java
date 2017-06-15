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
	public static final String SERVICE_NAME = "cloudconductor-agent";
	
	/**
	 * The wait time for server connection retries at start up
	 */
	public static final long WAIT_FOR_SERVER = 5000;
	
	/**
	 * property for the server url
	 */
	public static final String CLOUDCONDUCTOR_URL_PROP = "CLOUDCONDUCTOR_URL";
	/**
	 * additional url path for api access of server
	 */
	public static final String CLOUDCONDUCTOR_API_PATH = "/api";
	
	/**
	 * property for the template name
	 */
	public static final String TEMPLATE_PROP = "TEMPLATE_NAME";
	
	/**
	 * property for the agent name
	 */
	public static final String AGENT_PROP = "AGENT_NAME";
	
	/**
	 * property for the authentication token
	 */
	public static final String TOKEN_PROP = "AUTH_TOKEN";
	/**
	 * property for the yum repo name
	 */
	public static final String YUM_NAME_PROP = "nodeagent.repo.name";
	/**
	 * derfault yum repo name
	 */
	public static final String YUM_NAME_PROP_DEFAULT = "cinovo";
	
	/**
	 * system path for yum repos
	 */
	public static final String YUM_REPO_FOLDER = "/etc/yum.repos.d/";
	/**
	 * file ending for yum repo definition files
	 */
	public static final String YUM_REPO_ENDING = ".repo";
	
	/**
	 * service state script
	 */
	public static final String SCRIPT_SERVICE_STATE = "serviceState.sh";
	/**
	 * service handler script
	 */
	public static final String SCRIPT_SERVICE_HANDLER = "serviceHandler.sh";
	/**
	 * yum handler script
	 */
	public static final String SCRIPT_YUM_HANDLER = "yumHandler.sh";
	/**
	 * relative path to the script folder
	 */
	public static final String SCRIPTFOLDER = "scripts/";

	/**
	 * property for agent / host uuid
	 */
	public static final String UUID_PROP = "UUID";

}
