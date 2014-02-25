package de.cinovo.cloudconductor.agent.jobs;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.executors.FileExecutor;
import de.cinovo.cloudconductor.agent.executors.IExecutor;
import de.cinovo.cloudconductor.agent.executors.ScriptExecutor;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.Service;
import de.cinovo.cloudconductor.api.model.ServiceStates;
import de.cinovo.cloudconductor.api.model.ServiceStatesChanges;

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
		List<String> runningServices = this.collectRunningServices();
		ServiceStates req = new ServiceStates(runningServices);
		ServiceStatesChanges serviceChanges;
		try {
			serviceChanges = ServerCom.notifyRunningServices(req);
		} catch (CloudConductorException e) {
			throw new ExecutionError(e);
		}
		
		// handle files
		IExecutor<Set<String>> files = new FileExecutor(serviceChanges.getConfigFiles());
		try {
			files.execute();
		} catch (ExecutionError e) {
			// just log the error but go on with execution
			ServiceHandler.LOGGER.error(e.getMessage());
		}
		
		Set<String> servicesToRestart = files.getResult();
		servicesToRestart.addAll(serviceChanges.getToRestart());
		
		// handle service changes
		ScriptExecutor serviceHandler = ScriptExecutor.generateServiceStateHandler(servicesToRestart, serviceChanges.getToStart(), serviceChanges.getToStop());
		try {
			serviceHandler.execute();
		} catch (ExecutionError e) {
			// just log the error but go on with execution
			ServiceHandler.LOGGER.error(e.getMessage());
		}
		
		// notify server on current state
		runningServices = this.collectRunningServices();
		req = new ServiceStates(runningServices);
		try {
			ServerCom.notifyRunningServices(req);
		} catch (CloudConductorException e) {
			throw new ExecutionError(e);
		}
	}
	
	private List<String> collectRunningServices() throws ExecutionError {
		Set<Service> services = null;
		try {
			services = ServerCom.getServices();
		} catch (CloudConductorException e) {
			throw new ExecutionError(e);
		}
		
		List<String> runningServices = new ArrayList<String>();
		ScriptExecutor serviceStateHandler = ScriptExecutor.generateCheckServiceState(services);
		serviceStateHandler.execute();
		try (Scanner s = new Scanner(serviceStateHandler.getResult())) {
			while (s.hasNextLine()) {
				runningServices.add(s.next().trim());
			}
		}
		
		return runningServices;
	}
}
