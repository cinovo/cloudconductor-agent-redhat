package de.cinovo.cloudconductor.agent.jobs.handler;

import de.cinovo.cloudconductor.agent.AgentState;
import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.helper.FileHelper;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.Repo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
		
		RepoHandler.LOGGER.debug("Update " + repos.size() + " yum repos...");

		Set<String> repoNames = new HashSet<>();
		for (Repo repo : repos) {
			try {
				repoNames.add(repo.getName());
				FileHelper.writeYumRepo(repo);
			} catch (IOException e) {
				throw new ExecutionError("Error writing yum repo for '" + repo.getName() + "': ", e);
			}
		}
		AgentState.info().setRepos(repoNames);
		RepoHandler.LOGGER.debug("Finished RepoHandler");
	}
}
