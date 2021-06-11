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

import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.exceptions.ServerConnectionException;
import de.cinovo.cloudconductor.agent.helper.AgentVars;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.agent.jobs.AgentJob;
import de.cinovo.cloudconductor.agent.jobs.RefreshJWTJob;
import de.cinovo.cloudconductor.agent.jobs.handler.OptionHandler;
import de.cinovo.cloudconductor.agent.jobs.handler.RepoHandler;
import de.cinovo.cloudconductor.agent.tasks.SchedulerService;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.Template;
import de.taimos.daemon.properties.FilePropertyProvider;
import de.taimos.daemon.properties.IPropertyProvider;
import de.taimos.dvalin.daemon.DvalinLifecycleAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 *
 * @author psigloch
 */
public final class NodeAgent extends DvalinLifecycleAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(NodeAgent.class);

	/**
	 * the node agent constructor
	 */
	NodeAgent() {
		this.setupLogging();
	}

	@Override
	public IPropertyProvider getPropertyProvider() {
		return new FilePropertyProvider("cloudconductor-agent.properties");
	}

	@Override
	protected void doBeforeSpringStart() {
		this.assure(AgentVars.CLOUDCONDUCTOR_URL_PROP, "The CloudConductor url is not set.");
		this.assure(AgentVars.TEMPLATE_PROP, "No template was set.");
		try {
			System.getProperties().putAll(ServerCom.getConfig());
		} catch(RuntimeException | CloudConductorException e) {
			NodeAgent.LOGGER.warn("Couldn't retrieve properties from config server", e);
		}
		super.doBeforeSpringStart();
	}


	@Override
	public void doAfterSpringStart() {
		super.doAfterSpringStart();
		this.waitForServer();

		// initialize yum repos
		if (AgentState.repoExecutionLock.tryLock()) {
			try {
				new RepoHandler().run();
			} catch(ExecutionError e){
				NodeAgent.LOGGER.error("Error initializing yum repos: ", e);
				return;
			} finally{
				AgentState.repoExecutionLock.unlock();
			}
		} else {
			NodeAgent.LOGGER.error("Could not aquire lock to int yum repos");
		}

		// start timed jobs
		for(Class<AgentJob> jobClazz : OptionHandler.jobRegistry) {
			AgentJob job;
			try {
				job = jobClazz.newInstance();
				if(job.isDefaultStart()) {
					SchedulerService.instance.register(job.getJobIdentifier(), job, job.defaultStartTimer(), job.defaultStartTimerUnit());
					NodeAgent.LOGGER.info("Registered " + job.getJobIdentifier() + " as defaultstart with " + job.defaultStartTimer() + ":" + job.defaultStartTimerUnit());
				} else {
					SchedulerService.instance.register(job.getJobIdentifier(), job);
					NodeAgent.LOGGER.info("Registered " + job.getJobIdentifier());
				}
			} catch(InstantiationException | IllegalAccessException e) {
				NodeAgent.LOGGER.error("Couldn't start job: " + jobClazz.getName(), e);
			}
		}
	}

	@Override
	public void doBeforeSpringStop() {
		SchedulerService.instance.shutdown();
		super.doBeforeSpringStop();
	}

	private void waitForServer() {
		try {
			while(!this.checkServer()) {
				try {
					Thread.sleep(AgentVars.WAIT_FOR_SERVER);
				} catch(InterruptedException e) {
					// just got on
				}
			}
		} catch(ServerConnectionException e) {
			NodeAgent.LOGGER.error("The Template " + AgentState.info().getTemplate() + " is not known by the server. I have no work to do. Quitting...");
			System.exit(1);
		}
	}

	private boolean checkServer() throws ServerConnectionException {
		NodeAgent.LOGGER.info("Perform first authentication...");
		new RefreshJWTJob().run();

		NodeAgent.LOGGER.info("Get template from server...");

		try {
			Template template = ServerCom.getTemplate();
			if(template != null) {
				return true;
			}
			throw new ServerConnectionException();
		} catch(CloudConductorException e) {
			NodeAgent.LOGGER.error("Error getting template from server: ", e);
			throw new ServerConnectionException();
		}
	}

	private void assure(String prop, String errorMsg) {
		if((System.getProperty(prop) == null) || System.getProperty(prop).isEmpty()) {
			if((System.getenv(prop) == null) || System.getenv(prop).isEmpty()) {
				System.err.println(errorMsg);
				System.exit(1);
			} else {
				System.setProperty(prop, System.getenv(prop));
			}
		}
	}

}
