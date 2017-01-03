package de.cinovo.cloudconductor.agent.jobs.handler;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.executors.DirectoryExecutor;
import de.cinovo.cloudconductor.agent.executors.IExecutor;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.Directory;

/**
 * Created by janweisssieker on 22.12.16.
 */
public class DirectoryHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryHandler.class);
	
	
	/**
	 * @throws ExecutionError an error occurred during execution
	 */
	public void run() throws ExecutionError {
		DirectoryHandler.LOGGER.debug("Start Directory Handler");
		Set<Directory> directories;
		try {
			directories = ServerCom.getDirectories();
		} catch (CloudConductorException e) {
			throw new ExecutionError(e);
		}
		DirectoryHandler.LOGGER.debug("Handle Directories");
		IExecutor<Set<String>> dirs = new DirectoryExecutor(directories);
		try {
			dirs.execute();
		} catch (ExecutionError e) {
			DirectoryHandler.LOGGER.error(e.getMessage());
		}
		
		DirectoryHandler.LOGGER.debug("Finished Directory Handler");
	}
}
