package de.cinovo.cloudconductor.agent.jobs;

/*
 * #%L
 * Node Agent for cloudconductor framework
 * %%
 * Copyright (C) 2013 - 2014 Cinovo AG
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.collect.ArrayListMultimap;
import de.cinovo.cloudconductor.agent.helper.FileHelper;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.SSHKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 *
 * @author psigloch
 *
 */
public class AuthorizedKeysJob implements AgentJob {
	
	/** the job name, used by scheduler */
	public static final String JOB_NAME = "AUTHORIZED_KEYS";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizedKeysJob.class);
	
	
	@Override
	public void run() {
		Set<SSHKey> sshKeys;
		try {
			sshKeys = ServerCom.getSSHKeys();
		} catch (CloudConductorException e) {
			AuthorizedKeysJob.LOGGER.error("Couldn't retrieve ssh keys from server.", e);
			return;
		}
		
		if (!sshKeys.isEmpty()) {
			ArrayListMultimap<String, SSHKey> userKeyMap = ArrayListMultimap.create();
			for (SSHKey key : sshKeys) {
				userKeyMap.put(key.getUsername(), key);
			}
			
			// write file for each user
			for (String username : userKeyMap.keySet()) {
				try {
					FileHelper.writeAuthorizedKeysForUser(username, userKeyMap.get(username));
				} catch (IOException e) {
					AuthorizedKeysJob.LOGGER.error("Couldn't write authorized keys for user '" + username + "'", e);
					return;
				}
			}
		}
	}
	
	@Override
	public String getJobIdentifier() {
		return AuthorizedKeysJob.JOB_NAME;
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
