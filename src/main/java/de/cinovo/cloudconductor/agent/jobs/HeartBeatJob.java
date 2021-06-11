package de.cinovo.cloudconductor.agent.jobs;

import de.cinovo.cloudconductor.agent.AgentState;
import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.agent.jobs.handler.OptionHandler;
import de.cinovo.cloudconductor.agent.jobs.handler.RepoHandler;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.AgentOption;
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
public class HeartBeatJob implements AgentJob {
	
	/** the job name, used by scheduler */
	public static final String JOB_NAME = "HEART_BEAT_JOB";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HeartBeatJob.class);
	
	
	@Override
	public void run() {
		HeartBeatJob.LOGGER.debug("Starting HeartBeatJob");
		AgentOption newOptions;
		try {
			newOptions = ServerCom.heartBeat();
			new OptionHandler(newOptions).run();
		} catch (CloudConductorException e) {
			HeartBeatJob.LOGGER.error("Error getting agent options from server: ", e);
			return;
		}
		
		if (AgentState.repoExecutionLock.tryLock()) {
			try {
				new RepoHandler().run();
			} catch (ExecutionError e) {
				HeartBeatJob.LOGGER.warn("Error updating repos: ", e);
			} catch (Exception e) {
				HeartBeatJob.LOGGER.error("Error updating repos: ", e);
			} finally {
				AgentState.repoExecutionLock.unlock();
			}
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
