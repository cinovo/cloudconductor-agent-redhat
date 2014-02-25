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
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cinovo.cloudconductor.agent.exceptions.ServerConnectionException;
import de.cinovo.cloudconductor.agent.helper.AgentVars;
import de.cinovo.cloudconductor.agent.helper.FileHelper;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.agent.jobs.AgentJob;
import de.cinovo.cloudconductor.agent.jobs.AuhtorizedKeysJob;
import de.cinovo.cloudconductor.agent.jobs.DefaultJob;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.Template;
import de.taimos.daemon.DaemonLifecycleAdapter;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 */
public final class NodeAgent extends DaemonLifecycleAdapter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NodeAgent.class);
	
	private static final AgentJob[] timedJobs = new AgentJob[] {new DefaultJob(), new AuhtorizedKeysJob()};
	
	
	/** Constructor */
	public NodeAgent() {
		try {
			while (!this.checkServer()) {
				try {
					Thread.sleep(AgentVars.WAIT_FOR_SERVER);
				} catch (InterruptedException e) {
					// just got on
				}
			}
		} catch (ServerConnectionException e) {
			// this is still before the logger gets initialized
			System.err.println("The given Template is not known by the server. Please check the Template");
			System.exit(1);
		}
	}
	
	private boolean checkServer() throws ServerConnectionException {
		try {
			Template template = ServerCom.getTemplate();
			if (template != null) {
				return true;
			}
			throw new ServerConnectionException();
		} catch (CloudConductorException e) {
			// this is still before the logger gets initialized
			System.err.println("Initial server connection failed! Retrying in " + (AgentVars.WAIT_FOR_SERVER / 1000) + " seconds ...");
			return false;
		}
	}
	
	@Override
	public Map<String, String> loadProperties() {
		Map<String, String> result = new HashMap<String, String>();
		try {
			result = ServerCom.getConfig();
			if ((result.get(AgentVars.YUM_NAME_PROP) == null) || result.get(AgentVars.YUM_NAME_PROP).isEmpty()) {
				result.put(AgentVars.YUM_NAME_PROP, AgentVars.YUM_NAME_PROP_DEFAULT);
			}
		} catch (RuntimeException | CloudConductorException e) {
			NodeAgent.LOGGER.warn("Couldn't retrieve properties from config server");
			result.put(AgentVars.YUM_NAME_PROP, AgentVars.YUM_NAME_PROP_DEFAULT);
		}
		return result;
	}
	
	@Override
	public void doStart() throws Exception {
		try {
			FileHelper.writeYumRepo();
		} catch (CloudConductorException | IOException e) {
			NodeAgent.LOGGER.error("Couldn't create yum repo file.", e);
			throw e;
		}
	}
	
	@Override
	public void started() {
		// start timed jobs
		for (AgentJob job : NodeAgent.timedJobs) {
			AgentState.ses.scheduleAtFixedRate(job, job.getInititalDelay(), job.getRepeatTimer(), job.getRepeatTimerUnit());
		}
	}
	
	@Override
	public void doStop() throws Exception {
		AgentState.ses.shutdown();
		super.doStop();
	}
}
