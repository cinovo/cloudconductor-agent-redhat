package de.cinovo.cloudconductor.agent.jobs.handler;

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
import de.cinovo.cloudconductor.agent.executors.ScriptExecutor;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.Service;
import de.cinovo.cloudconductor.api.model.ServiceStates;
import de.cinovo.cloudconductor.api.model.ServiceStatesChanges;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 *
 * @author psigloch
 *
 */
public class ServiceHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceHandler.class);
	
	
	/**
	 * @throws ExecutionError an error occurred during execution
	 */
	public void run() throws ExecutionError {
		ServiceHandler.LOGGER.debug("Start ServiceHandler");
		ServiceHandler.LOGGER.debug("ServiceHandler : Report running services");
		List<String> runningServices = this.collectRunningServices();
		
		ServiceStates req = new ServiceStates(runningServices);
		ServiceStatesChanges serviceChanges;
		try {
			serviceChanges = ServerCom.notifyRunningServices(req);
		} catch (CloudConductorException e) {
			throw new ExecutionError(e);
		}
		
		int nStart = serviceChanges.getToStart().size();
		int nStop = serviceChanges.getToStop().size();
		int nRestart = serviceChanges.getToRestart().size();
		ServiceHandler.LOGGER.info("Service changes: " + nStart + " to be started, " + nStop + " to be stopped, " + nRestart + " to be restarted");
		
		// handle service changes
		ServiceHandler.LOGGER.debug("ServiceHandler: Handle service changes");
		ScriptExecutor serviceHandler = ScriptExecutor.generateServiceStateHandler(serviceChanges.getToRestart(), serviceChanges.getToStart(), serviceChanges.getToStop());
		try {
			serviceHandler.execute();
		} catch (ExecutionError e) {
			// just log the error but go on with execution
			ServiceHandler.LOGGER.error("Error executing service handler: ", e);
		}
		
		// notify server on current state
		ServiceHandler.LOGGER.debug("ServiceHandler : Report running services again");
		runningServices = this.collectRunningServices();
		
		req = new ServiceStates(runningServices);
		try {
			ServerCom.notifyRunningServices(req);
		} catch (CloudConductorException e) {
			throw new ExecutionError(e);
		}
		ServiceHandler.LOGGER.debug("Finished ServiceHandler");
	}
	
	private List<String> collectRunningServices() throws ExecutionError {
		Set<Service> services = null;
		try {
			services = ServerCom.getServices();
		} catch (CloudConductorException e) {
			throw new ExecutionError("Error getting services from server: ", e);
		}
		
		List<String> runningServices = new ArrayList<String>();
		ScriptExecutor serviceStateHandler = ScriptExecutor.generateCheckServiceState(services);
		serviceStateHandler.execute();
		try (Scanner s = new Scanner(serviceStateHandler.getResult())) {
			while (s.hasNextLine()) {
				String scriptName = s.next().trim();
				for(Service service : services) {
					if(service.getInitScript().equalsIgnoreCase(scriptName)) {
						runningServices.add(service.getName());
					}
				}
			}
		}
		ServiceHandler.LOGGER.info(services.size() + " services registered, " + runningServices.size() + " running.");
		
		return runningServices;
	}
}
