package de.cinovo.cloudconductor.agent.jobs;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cinovo.cloudconductor.agent.AgentState;
import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.jobs.handler.DirectoryHandler;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;

/**
 * Created by janweisssieker on 22.12.16.
 */
public class DirectoriesJob implements AgentJob {
	
	/** the job name, used by scheduler */
	public static final String JOB_NAME = "DIRECTORIES_JOB";
	private static final Logger LOGGER = LoggerFactory.getLogger(DirectoriesJob.class);
	
	
	@Override
	public String getJobIdentifier() {
		return DirectoriesJob.JOB_NAME;
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
	
	@Override
	public void run() {
		DirectoriesJob.LOGGER.debug("Started Directory Job");
		
		if (AgentState.directoryExecutionLock.tryLock()) {
			try {
				new DirectoryHandler().run();
			} catch (ExecutionError e) {
				if (e.getCause() instanceof CloudConductorException) {
					DirectoriesJob.LOGGER.debug(e.getMessage(), e);
				}
			} finally {
				AgentState.directoryExecutionLock.unlock();
			}
		}
		DirectoriesJob.LOGGER.debug("finished Directory Job");
	}
}
