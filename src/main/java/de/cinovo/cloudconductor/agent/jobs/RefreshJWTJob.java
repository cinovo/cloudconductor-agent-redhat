package de.cinovo.cloudconductor.agent.jobs;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import de.cinovo.cloudconductor.agent.AgentState;
import de.cinovo.cloudconductor.agent.helper.JWTHelper;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.agent.tasks.SchedulerService;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;

/**
 * 
 * Copyright 2017 Cinovo AG<br>
 * <br>
 * 
 * @author mweise
 *
 */
public class RefreshJWTJob implements AgentJob {
	
	/**
	 * the job name, used by the scheduler
	 */
	public static final String JOB_NAME = "REFRESH_JWT_JOB";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RefreshJWTJob.class);
	
	
	@Override
	public void run() {
		RefreshJWTJob.LOGGER.debug("Start RefreshJWTJob");
		
		String newJWT = null;
		long period = 1000 * 60;
		try {
			newJWT = ServerCom.getJWT();
			
			if (newJWT != null) {
				JWTClaimsSet claimsSet = SignedJWT.parse(newJWT).getJWTClaimsSet();
				AgentState.info().setJWT(newJWT);
				
				period = JWTHelper.calcNextRefreshInMillis(claimsSet);
				RefreshJWTJob.LOGGER.info("Authentication successful!");
			} else {
				RefreshJWTJob.LOGGER.error("Authentication failed: Missing JWT!");
				throw new CloudConductorException("Missing JWT!");
			}
		} catch (CloudConductorException e) {
			RefreshJWTJob.LOGGER.error("Error refreshing JWT: ", e);
		} catch (ParseException e) {
			RefreshJWTJob.LOGGER.error("Error parsing new JWT '" + newJWT + "': ", e);
		} finally {
			SchedulerService.instance.executeOnce(new RefreshJWTJob(), period, TimeUnit.MILLISECONDS);
			RefreshJWTJob.LOGGER.info("Scheduled next refresh of JWT in " + period + " ms");
			
			RefreshJWTJob.LOGGER.debug("Finished RefreshJWTJob");
		}
	}
	
	@Override
	public String getJobIdentifier() {
		return RefreshJWTJob.JOB_NAME;
	}
	
	@Override
	public boolean isDefaultStart() {
		return false;
	}
	
	@Override
	public long defaultStartTimer() {
		return 30;
	}
	
	@Override
	public TimeUnit defaultStartTimerUnit() {
		return TimeUnit.MINUTES;
	}
	
}
