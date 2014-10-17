package de.cinovo.cloudconductor.agent.jobs;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.agent.jobs.handler.OptionHandler;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.AgentOptions;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(AuhtorizedKeysJob.class);
	
	
	@Override
	public void run() {
		AgentOptions newOptions;
		try {
			newOptions = ServerCom.heartBeat();
		} catch (CloudConductorException e) {
			HeartBeatJob.LOGGER.error("Couldn't retrieve ssh keys from server.", e);
			return;
		}
		
		new OptionHandler(newOptions).run();
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
