package de.cinovo.cloudconductor.agent.jobs.handler;

import de.cinovo.cloudconductor.agent.AgentState;
import de.cinovo.cloudconductor.agent.jobs.AgentJob;
import de.cinovo.cloudconductor.agent.jobs.AuthorizedKeysJob;
import de.cinovo.cloudconductor.agent.jobs.DefaultJob;
import de.cinovo.cloudconductor.agent.jobs.FilesJob;
import de.cinovo.cloudconductor.agent.jobs.HeartBeatJob;
import de.cinovo.cloudconductor.agent.tasks.SchedulerService;
import de.cinovo.cloudconductor.api.model.AgentOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright 2014 Cinovo AG<br>
 * <br>
 *
 * @author psigloch
 */
public class OptionHandler {

	/**
	 * existing jobs
	 */
	@SuppressWarnings("unchecked")
	public static final Class<AgentJob>[] jobRegistry = new Class[]{DefaultJob.class, AuthorizedKeysJob.class, FilesJob.class, HeartBeatJob.class};
	private static final Logger LOGGER = LoggerFactory.getLogger(OptionHandler.class);
	private AgentOption newOptions;


	/**
	 * @param newOptions the new options to use
	 */
	public OptionHandler(AgentOption newOptions) {
		this.newOptions = newOptions;
	}

	/**
	 */
	public void run() {
		OptionHandler.LOGGER.debug("Starting OptionHandler");
		AgentOption oldOptions = AgentState.getOptions();
		AgentState.setOptions(this.newOptions);

		// option timer
		if((oldOptions == null) || (this.newOptions.getAliveTimer() != oldOptions.getAliveTimer()) || //
				(this.newOptions.getAliveTimerUnit() != oldOptions.getAliveTimerUnit())) {
			OptionHandler.LOGGER.debug("Reseting OPTIONTIMER TO " + this.newOptions.getAliveTimer() + ":" + this.newOptions.getAliveTimerUnit());
			SchedulerService.instance.resetTask(HeartBeatJob.JOB_NAME, this.newOptions.getAliveTimer(), this.newOptions.getAliveTimerUnit());
		}

		// SSH KEYS
		switch(this.newOptions.getDoSshKeys()) {
			case OFF:
				OptionHandler.LOGGER.debug("OptionHandler: STOP SHH KEY");
				SchedulerService.instance.stop(AuthorizedKeysJob.JOB_NAME);
				break;
			case ONCE:
				OptionHandler.LOGGER.debug("OptionHandler: ONCE SHH KEY");
				SchedulerService.instance.stop(AuthorizedKeysJob.JOB_NAME);
				SchedulerService.instance.executeOnce(AuthorizedKeysJob.JOB_NAME);
				break;
			case REPEAT:
				if((oldOptions == null) || (this.newOptions.getSshKeysTimer() != oldOptions.getSshKeysTimer()) || (this.newOptions.getSshKeysTimerUnit() != oldOptions.getSshKeysTimerUnit())) {
					OptionHandler.LOGGER.debug("OptionHandler: REPEAT SHH KEY");
					SchedulerService.instance.resetTask(AuthorizedKeysJob.JOB_NAME, this.newOptions.getSshKeysTimer(), this.newOptions.getSshKeysTimerUnit());
				}else if(!SchedulerService.instance.taskRunning(AuthorizedKeysJob.JOB_NAME)){
					OptionHandler.LOGGER.debug("OptionHandler: STARTING TASK SHH KEY BECAUSE NOT RUNNING");
					SchedulerService.instance.resetTask(AuthorizedKeysJob.JOB_NAME, this.newOptions.getSshKeysTimer(), this.newOptions.getSshKeysTimerUnit());
				}
				break;
		}

		// FILE MANAGEMENT
		switch(this.newOptions.getDoFileManagement()) {
			case OFF:
				OptionHandler.LOGGER.debug("OptionHandler: STOP FILE MANAGEMENT");
				SchedulerService.instance.stop(FilesJob.JOB_NAME);
				break;
			case ONCE:
				OptionHandler.LOGGER.debug("OptionHandler: ONCE FILE MANAGEMENT");
				SchedulerService.instance.stop(FilesJob.JOB_NAME);
				SchedulerService.instance.executeOnce(FilesJob.JOB_NAME);
				break;
			case REPEAT:
				if((oldOptions == null) || (this.newOptions.getFileManagementTimer() != oldOptions.getFileManagementTimer()) || (this.newOptions.getFileManagementTimerUnit() != oldOptions.getFileManagementTimerUnit())) {
					OptionHandler.LOGGER.debug("OptionHandler: REPEAT FILE MANAGEMENT");
				}else if(!SchedulerService.instance.taskRunning(FilesJob.JOB_NAME)){
					OptionHandler.LOGGER.debug("OptionHandler: STARTING TASK FILE MANAGEMENT BECAUSE NOT RUNNING");
					SchedulerService.instance.resetTask(FilesJob.JOB_NAME, this.newOptions.getFileManagementTimer(), this.newOptions.getFileManagementTimerUnit());
				}
				break;
		}

		// PACKAGE MANAGEMENT
		switch(this.newOptions.getDoPackageManagement()) {
			case OFF:
				OptionHandler.LOGGER.debug("OptionHandler: STOP PKG MANAGEMENT");
				SchedulerService.instance.stop(DefaultJob.JOB_NAME);
				break;
			case ONCE:
				OptionHandler.LOGGER.debug("OptionHandler: ONCE PKG MANAGEMENT");
				SchedulerService.instance.stop(DefaultJob.JOB_NAME);
				SchedulerService.instance.executeOnce(DefaultJob.JOB_NAME);
				break;
			case REPEAT:
				if((oldOptions == null) || (this.newOptions.getPackageManagementTimer() != oldOptions.getPackageManagementTimer()) || (this.newOptions.getPackageManagementTimerUnit() != oldOptions.getPackageManagementTimerUnit())) {
					OptionHandler.LOGGER.debug("OptionHandler: REPEAT PKG MANAGEMENT");
					SchedulerService.instance.resetTask(DefaultJob.JOB_NAME, this.newOptions.getPackageManagementTimer(), this.newOptions.getPackageManagementTimerUnit());
				}else if(!SchedulerService.instance.taskRunning(DefaultJob.JOB_NAME)){
					OptionHandler.LOGGER.debug("OptionHandler: STARTING TASK PKG MANAGEMENT BECAUSE NOT RUNNING");
					SchedulerService.instance.resetTask(DefaultJob.JOB_NAME, this.newOptions.getPackageManagementTimer(), this.newOptions.getPackageManagementTimerUnit());
				}
				break;
		}
		OptionHandler.LOGGER.debug("Finished OptionHandler");

	}
}
