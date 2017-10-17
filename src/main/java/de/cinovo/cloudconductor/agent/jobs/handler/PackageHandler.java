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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.executors.IExecutor;
import de.cinovo.cloudconductor.agent.executors.InstalledPackages;
import de.cinovo.cloudconductor.agent.executors.ScriptExecutor;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.PackageState;
import de.cinovo.cloudconductor.api.model.PackageStateChanges;
import de.cinovo.cloudconductor.api.model.PackageVersion;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 */
public class PackageHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PackageHandler.class);
	
	
	/**
	 * @throws ExecutionError an error occurred during execution
	 */
	public void run() throws ExecutionError {
		PackageHandler.LOGGER.info("Start PackageHandler");
		
		// report installed packages
		PackageStateChanges packageChanges = this.reportInstalledPackages();
		
		// TODO remove me
		StringBuilder stInstall = new StringBuilder();
		for (PackageVersion pv : packageChanges.getToInstall()) {
			stInstall.append(pv.getName() + ": " + pv.getVersion() + ", ");
		}
		PackageHandler.LOGGER.info("Install: [" + stInstall.toString() + "]");
		
		StringBuilder stUpdate = new StringBuilder();
		for (PackageVersion pv : packageChanges.getToUpdate()) {
			stUpdate.append(pv.getName() + ": " + pv.getVersion() + ", ");
		}
		PackageHandler.LOGGER.info("Update: [" + stUpdate.toString() + "]");
		
		StringBuilder stDel = new StringBuilder();
		for (PackageVersion pv : packageChanges.getToErase()) {
			stDel.append(pv.getName() + ": " + pv.getVersion() + ", ");
		}
		PackageHandler.LOGGER.info("Delete: [" + stDel.toString() + "]");
		
		// handle package changes
		ScriptExecutor pkgHandler = ScriptExecutor.generatePackageHandler(packageChanges.getToErase(), packageChanges.getToInstall(), packageChanges.getToUpdate());
		pkgHandler.execute();
		
		// re-report installed packages
		this.reportInstalledPackages();
		
		PackageHandler.LOGGER.debug("Finished PackageHandler");
	}
	
	private PackageStateChanges reportInstalledPackages() throws ExecutionError {
		PackageState installedPackages = null;
		IExecutor<List<PackageVersion>> execute = new InstalledPackages().execute();
		installedPackages = new PackageState(execute.getResult());
		
		StringBuilder sb = new StringBuilder();
		for (PackageVersion pv : installedPackages.getInstalledRpms()) {
			sb.append(pv.getName() + ":" + pv.getVersion() + ", ");
		}
		
		try {
			return ServerCom.notifyInstalledPackages(installedPackages);
		} catch (CloudConductorException e) {
			PackageHandler.LOGGER.error("Error reporting installed packages: ", e);
			throw new ExecutionError(e);
		}
	}
}
