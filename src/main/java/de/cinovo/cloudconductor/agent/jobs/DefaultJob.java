package de.cinovo.cloudconductor.agent.jobs;

/*
 * #%L
 * Node Agent for cloudconductor framework
 * %%
 * Copyright (C) 2013 - 2014 Cinovo AG
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cinovo.cloudconductor.agent.AgentState;
import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.jobs.handler.PackageHandler;
import de.cinovo.cloudconductor.agent.jobs.handler.ServiceHandler;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 *
 * @author psigloch
 *
 */
public class DefaultJob implements AgentJob {
	
	/** the job name, used by scheduler */
	public static final String JOB_NAME = "DEFAULT_JOB";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJob.class);
	
	
	@Override
	public void run() {
		DefaultJob.LOGGER.debug("Started DefaultJob");
		// only run if no other blocking job is currently running
		if (AgentState.packageExecutionLock.tryLock()) {
			this.handlePackages();
			this.handleServices();
			AgentState.packageExecutionLock.unlock();
		}
		DefaultJob.LOGGER.debug("Finished DefaultJob");
	}
	
	private void handleServices() {
		try {
			ServiceHandler serviceHandler = new ServiceHandler();
			serviceHandler.run();
		} catch (ExecutionError e) {
			DefaultJob.LOGGER.error("Error handling services: " + e.getMessage(), e);
		}
	}
	
	private void handlePackages() {
		try {
			PackageHandler packageHandler = new PackageHandler();
			packageHandler.run();
		} catch (ExecutionError e) {
			if (e.getCause() instanceof CloudConductorException) {
				DefaultJob.LOGGER.error("Error handling packages: " + e.getMessage(), e);
			} else {
				DefaultJob.LOGGER.error("Error handling packages: " + e.getMessage());
			}
		}
	}
	
	@Override
	public String getJobIdentifier() {
		return DefaultJob.JOB_NAME;
	}
	
	@Override
	public boolean isDefaultStart() {
		return false;
	}
	
	@Override
	public long defaultStartTimer() {
		return 0;
	}
	
	@Override
	public TimeUnit defaultStartTimerUnit() {
		return null;
	}
}
