package de.cinovo.cloudconductor.agent.jobs.handler;

import de.cinovo.cloudconductor.agent.AgentState;
import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.helper.AgentVars;
import de.cinovo.cloudconductor.agent.helper.FileHelper;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.Repo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * Copyright 2017 Cinovo AG<br>
 * <br>
 * 
 * @author mweise
 *
 */
public class RepoHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RepoHandler.class);
	
	
	/**
	 * @throws ExecutionError an error occurred during the execution
	 */
	public void run() throws ExecutionError {
		RepoHandler.LOGGER.debug("Start RepoHandler");
		Set<Repo> repos;
		try {
			repos = ServerCom.getRepos();
		} catch (CloudConductorException e) {
			throw new ExecutionError("Error getting repositories for template: ", e);
		}
		
		RepoHandler.LOGGER.debug("Update {} yum repos", repos.size());

		Set<String> repoNames = new HashSet<>();
		Set<String> repoFileNames = new HashSet<>();
		for (Repo repo : repos) {
			try {
				repoNames.add(repo.getName());
				File repoFile = FileHelper.writeYumRepo(repo);
				repoFileNames.add(repoFile.getName());
			} catch (IOException e) {
				throw new ExecutionError("Error writing yum repo for '" + repo.getName() + "': ", e);
			}
		}
		AgentState.info().setRepos(repoNames);
		this.cleanUpUnneededRepoFiles(repoFileNames);
		RepoHandler.LOGGER.debug("Finished RepoHandler");
	}
	
	private void cleanUpUnneededRepoFiles(Set<String> neededRepoFiles) {
		File yumRepoDir = new File(AgentVars.YUM_REPO_FOLDER);
		File[] unneededRepoFiles = yumRepoDir.listFiles((dir, name) -> name.startsWith(AgentVars.YUM_REPO_PREFIX) && //
				name.endsWith(AgentVars.YUM_REPO_ENDING) && //
				!neededRepoFiles.contains(name));
		if (unneededRepoFiles == null || unneededRepoFiles.length < 1) {
			return;
		}
		
		for (File unneededRepoFile : unneededRepoFiles) {
			try {
				Files.delete(unneededRepoFile.toPath());
				RepoHandler.LOGGER.info("Deleted unneeded repo file {}", unneededRepoFile.getName());
			} catch (IOException e) {
				RepoHandler.LOGGER.warn("Error deleting unneeded repo file '" + unneededRepoFile.getName() + "': ", e);
			}
		}
	}
}
