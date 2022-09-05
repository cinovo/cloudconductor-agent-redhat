package de.cinovo.cloudconductor.agent.jobs;

import de.cinovo.cloudconductor.agent.AgentState;
import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.jobs.handler.ConfigFileHandler;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Copyright 2014 Cinovo AG<br>
 * <br>
 *
 * @author psigloch
 *
 */
public class FilesJob implements AgentJob {
	
	/** the job name, used by scheduler */
	public static final String JOB_NAME = "FILES_JOB";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FilesJob.class);
	
	
	@Override
	public void run() {
		FilesJob.LOGGER.debug("Started FilesJob");
		// only run if no other blocking job is currently running
		if (AgentState.filesExecutionLock.tryLock()) {
			try {
				new ConfigFileHandler().run();
			} catch (ExecutionError e) {
				if (e.getCause() instanceof CloudConductorException) {
					FilesJob.LOGGER.debug(e.getMessage(), e);
				}
			} finally {
				AgentState.filesExecutionLock.unlock();
			}
		}
		FilesJob.LOGGER.debug("Finished FilesJob");
	}
	
	@Override
	public String getJobIdentifier() {
		return FilesJob.JOB_NAME;
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
