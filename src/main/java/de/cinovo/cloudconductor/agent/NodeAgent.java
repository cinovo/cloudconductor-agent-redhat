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
import de.cinovo.cloudconductor.agent.jobs.handler.OptionHandler;
import de.cinovo.cloudconductor.agent.tasks.SchedulerService;
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
	
	
	private boolean checkServer() throws ServerConnectionException {
		try {
			Template template = ServerCom.getTemplate();
			if (template != null) {
				return true;
			}
			throw new ServerConnectionException();
		} catch (CloudConductorException e) {
			NodeAgent.LOGGER.error("Error getting template from server: ", e);
			throw new ServerConnectionException();
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
	public void started() {
		this.waitForServer();
		this.initYum();
		// start timed jobs
		for (Class<AgentJob> jobClazz : OptionHandler.jobRegistry) {
			AgentJob job;
			try {
				job = jobClazz.newInstance();
				if (job.isDefaultStart()) {
					SchedulerService.instance.register(job.getJobIdentifier(), job, job.defaultStartTimer(), job.defaultStartTimerUnit());
					NodeAgent.LOGGER.info("Registered " + job.getJobIdentifier() + " as defaultstart with " + job.defaultStartTimer() + ":" + job.defaultStartTimerUnit());
				} else {
					SchedulerService.instance.register(job.getJobIdentifier(), job);
					NodeAgent.LOGGER.info("Registered " + job.getJobIdentifier());
				}
			} catch (InstantiationException | IllegalAccessException e) {
				NodeAgent.LOGGER.error("Couldn't start job: " + jobClazz.getName(), e);
			}
			
		}
	}
	
	private void waitForServer() {
		try {
			while (!this.checkServer()) {
				try {
					Thread.sleep(AgentVars.WAIT_FOR_SERVER);
				} catch (InterruptedException e) {
					// just got on
				}
			}
		} catch (ServerConnectionException e) {
			NodeAgent.LOGGER.error("The Template " + AgentState.info().getTemplate() + " is not known by the server. I have no work to do. Quitting...");
			System.exit(1);
		}
	}
	
	/**
	 * yum initialization
	 */
	public void initYum() {
		try {
			FileHelper.writeYumRepo();
		} catch (CloudConductorException | IOException e) {
			NodeAgent.LOGGER.error("Couldn't create yum repo file.", e);
			return;
		}
		NodeAgent.LOGGER.info("Wrote yum repo");
	}
	
	@Override
	public void doStop() throws Exception {
		SchedulerService.instance.shutdown();
		super.doStop();
	}
}
