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

import de.cinovo.cloudconductor.agent.AgentState;
import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.executors.IExecutor;
import de.cinovo.cloudconductor.agent.executors.InstalledPackages;
import de.cinovo.cloudconductor.agent.executors.ScriptExecutor;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.PackageState;
import de.cinovo.cloudconductor.api.model.PackageStateChanges;
import de.cinovo.cloudconductor.api.model.PackageVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 *
 * @author psigloch
 */
public class PackageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PackageHandler.class);


	/**
	 * @throws ExecutionError an error occurred during execution
	 */
	public void run() throws ExecutionError {
		PackageHandler.LOGGER.debug("Start PackageHandler");

		// report installed packages
		PackageStateChanges packageChanges = this.reportInstalledPackages();
		PackageHandler.LOGGER.debug("Received : " + packageChanges.getToErase().size() + " to delete, " + packageChanges.getToInstall().size() + " to install, " + packageChanges.getToUpdate().size() + " to update");

		StringBuilder repoName = new StringBuilder();
		AgentState.info().getRepos().forEach(r -> repoName.append(r + ","));
		PackageHandler.LOGGER.debug("Execute changes on repo '" + repoName + "'");

		// executed package changes for each repository

		List<PackageVersion> toDelete = packageChanges.getToErase();
		PackageHandler.LOGGER.debug("Delete : " + toDelete.toString());

		List<PackageVersion> toInstall = packageChanges.getToInstall();
		PackageHandler.LOGGER.debug("Install: " + toInstall.toString());

		List<PackageVersion> toUpdate = packageChanges.getToUpdate();
		PackageHandler.LOGGER.debug("Update: " + toUpdate.toString());

		ScriptExecutor pkgHandler = ScriptExecutor.generatePackageHandler(repoName.toString(), toDelete, toInstall, toUpdate);
		pkgHandler.execute();

		// re-report installed packages
		this.reportInstalledPackages();

		PackageHandler.LOGGER.debug("Finished PackageHandler");
	}

	private PackageStateChanges reportInstalledPackages() throws ExecutionError {
		PackageState installedPackages;
		LOGGER.debug("Sarting to report packages");
		IExecutor<List<PackageVersion>> execute = new InstalledPackages().execute();
		LOGGER.debug("Found packages to report");
		installedPackages = new PackageState(execute.getResult());
		try {
			return ServerCom.notifyInstalledPackages(installedPackages);
		} catch(CloudConductorException e) {
			PackageHandler.LOGGER.error("Error reporting installed packages: ", e);
			throw new ExecutionError(e);
		}
	}
}
