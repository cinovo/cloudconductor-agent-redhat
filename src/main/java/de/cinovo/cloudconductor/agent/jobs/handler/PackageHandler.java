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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

		Map<String, PackageStateChanges> changesByRepo = this.getChangesByRepo(packageChanges);
		PackageHandler.LOGGER.debug("Package changes for " + changesByRepo.size() + " repos");

		// executed package changes for each repository
		for(Map.Entry<String, PackageStateChanges> changes : changesByRepo.entrySet()) {
			String repoName = changes.getKey();
			PackageHandler.LOGGER.info("Execute changes on repo '" + repoName + "'");

			List<PackageVersion> toDelete = changes.getValue().getToErase();
			PackageHandler.LOGGER.info("Delete : " + toDelete.toString());

			List<PackageVersion> toInstall = changes.getValue().getToInstall();
			PackageHandler.LOGGER.info("Install: " + toInstall.toString());

			List<PackageVersion> toUpdate = changes.getValue().getToUpdate();
			PackageHandler.LOGGER.info("Update: " + toUpdate.toString());

			ScriptExecutor pkgHandler = ScriptExecutor.generatePackageHandler(repoName, toDelete, toInstall, toUpdate);
			pkgHandler.execute();
		}

		// re-report installed packages
		this.reportInstalledPackages();

		PackageHandler.LOGGER.debug("Finished PackageHandler");
	}

	private Map<String, PackageStateChanges> getChangesByRepo(PackageStateChanges allChanges) {
		HashMap<String, PackageStateChanges> changesByRepo = new HashMap<>();

		for(PackageVersion pvToInstall : allChanges.getToInstall()) {

			// first find out which repo should actually be used
			String repoToUse = this.findRepo(pvToInstall);
			if(repoToUse == null) {
				continue;
			}

			// add package version for selected repo
			if(!changesByRepo.containsKey(repoToUse)) {
				changesByRepo.put(repoToUse, new PackageStateChanges(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
			}
			changesByRepo.get(repoToUse).getToInstall().add(pvToInstall);
		}

		// handle package versions to be updated
		for(PackageVersion pvToUpdate : allChanges.getToUpdate()) {
			String repoToUse = this.findRepo(pvToUpdate);
			if(repoToUse == null) {
				continue;
			}

			// add package version for selected repo
			if(!changesByRepo.containsKey(repoToUse)) {
				changesByRepo.put(repoToUse, new PackageStateChanges(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
			}
			changesByRepo.get(repoToUse).getToUpdate().add(pvToUpdate);
		}

		// handle package versions to be deleted
		for(PackageVersion pvToDelete : allChanges.getToErase()) {
			PackageHandler.LOGGER.debug("Package Version: " + pvToDelete.toString());
			String repoToUse = this.findRepo(pvToDelete);
			//we don't skip repo == null on delete, since the delete script is not repo dependant, and we assure the deinstall this way

			// add package version for selected repo
			if(!changesByRepo.containsKey(repoToUse)) {
				changesByRepo.put(repoToUse, new PackageStateChanges(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
			}
			changesByRepo.get(repoToUse).getToErase().add(pvToDelete);
		}

		return changesByRepo;
	}

	private String findRepo(PackageVersion version) {
		for(String agentRepo : AgentState.info().getRepos()) {
			if(version.getRepos().contains(agentRepo)) {
				return agentRepo;
			}
		}
		LOGGER.info("Can't handle Package " + version.toString() + " - No repository provides this package");
		return null;
	}

	private PackageStateChanges reportInstalledPackages() throws ExecutionError {
		PackageState installedPackages = null;
		IExecutor<List<PackageVersion>> execute = new InstalledPackages().execute();
		installedPackages = new PackageState(execute.getResult());

		StringBuilder sb = new StringBuilder();
		for(PackageVersion pv : installedPackages.getInstalledRpms()) {
			sb.append(pv.getName() + ":" + pv.getVersion() + ", ");
		}

		try {
			return ServerCom.notifyInstalledPackages(installedPackages);
		} catch(CloudConductorException e) {
			PackageHandler.LOGGER.error("Error reporting installed packages: ", e);
			throw new ExecutionError(e);
		}
	}
}
