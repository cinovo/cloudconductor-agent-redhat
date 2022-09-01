package de.cinovo.cloudconductor.agent;

/*
 * #%L
 * Node Agent for cloudconductor framework
 * %%
 * Copyright (C) 2013 - 2014 Cinovo AG
 * %%
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * #L%
 */

import de.cinovo.cloudconductor.agent.helper.AgentVars;
import de.taimos.daemon.DaemonStarter;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 */
public final class Starter {
	
	private static final Logger logger = LoggerFactory.getLogger(Starter.class);
	
	/**
	 * @param args arguments
	 */
	public static void main(final String[] args) {
		Starter.initVelocity();
		DaemonStarter.startDaemon(AgentVars.SERVICE_NAME, new NodeAgent());
	}
	
	private static void initVelocity() {
		try {
			// Use UTF-8
			Velocity.setProperty("input.encoding", "UTF-8");
			Velocity.setProperty("output.encoding", "UTF-8");
			Velocity.init();
		} catch (Exception e) {
			Starter.logger.error("Couldn't start up velocity.");
			System.exit(1);
		}
	}
}
