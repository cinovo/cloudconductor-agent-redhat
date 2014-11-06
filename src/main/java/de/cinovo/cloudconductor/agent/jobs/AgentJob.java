package de.cinovo.cloudconductor.agent.jobs;

import java.util.concurrent.TimeUnit;

/**
 * Copyright 2014 Cinovo AG<br>
 * <br>
 *
 * @author psigloch
 *
 */
public interface AgentJob extends Runnable {

	/**
	 * @return the identifier
	 */
	public String getJobIdentifier();

	/**
	 * @return start on default
	 */
	public boolean isDefaultStart();

	/**
	 * @return the default start timer
	 */
	public long defaultStartTimer();
	
	/**
	 * @return the default start timer unit
	 */
	public TimeUnit defaultStartTimerUnit();

}
