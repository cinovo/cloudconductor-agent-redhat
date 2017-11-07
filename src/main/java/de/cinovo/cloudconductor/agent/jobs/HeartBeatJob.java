package de.cinovo.cloudconductor.agent.jobs;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cinovo.cloudconductor.agent.AgentState;
import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.agent.jobs.handler.OptionHandler;
import de.cinovo.cloudconductor.agent.jobs.handler.RepoHandler;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.AgentOption;

/**
 * Copyright 2014 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 */
public class HeartBeatJob implements AgentJob {
	
	/** the job name, used by scheduler */
	public static final String JOB_NAME = "HEART_BEAT_JOB";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizedKeysJob.class);
	
	
	@Override
	public void run() {
		HeartBeatJob.LOGGER.debug("Starting HeartBeatJob");
		AgentOption newOptions;
		try {
			newOptions = ServerCom.heartBeat();
		} catch (CloudConductorException e) {
			HeartBeatJob.LOGGER.error("Error getting options from server: ", e);
			return;
		}
		
		new OptionHandler(newOptions).run();
		
		try {
			if (AgentState.repoExecutionLock.tryLock()) {
				new RepoHandler().run();
			}
		} catch (ExecutionError e) {
			HeartBeatJob.LOGGER.error("Error updating repos: ", e);
		} finally {
			AgentState.repoExecutionLock.unlock();
		}
		HeartBeatJob.LOGGER.debug("Finished HeartBeatJob");
	}
	
	@Override
	public String getJobIdentifier() {
		return HeartBeatJob.JOB_NAME;
	}
	
	@Override
	public boolean isDefaultStart() {
		return true;
	}
	
	@Override
	public long defaultStartTimer() {
		return 1;
	}
	
	@Override
	public TimeUnit defaultStartTimerUnit() {
		return TimeUnit.MINUTES;
	}
}
