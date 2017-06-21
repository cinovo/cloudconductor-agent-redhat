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

import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.Log4JLogChute;

import de.cinovo.cloudconductor.agent.helper.AgentVars;
import de.taimos.daemon.DaemonStarter;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 */
public final class Starter {
	
	/**
	 * @param args arguments
	 */
	public static void main(final String[] args) {
		Starter.assure(AgentVars.CLOUDCONDUCTOR_URL_PROP, "The CloudConductor url is not set.");
		Starter.assure(AgentVars.TEMPLATE_PROP, "No template was set.");
		Starter.applyPropertyFromEnv(AgentVars.AGENT_PROP);
		Starter.applyPropertyFromEnv(AgentVars.TOKEN_PROP);
		Starter.applyPropertyFromEnv(AgentVars.UUID_PROP);
		Starter.initVelocity();
		
		DaemonStarter.startDaemon(AgentVars.SERVICE_NAME, new NodeAgent());
	}
	
	private static void assure(String prop, String errorMsg) {
		if ((System.getProperty(prop) == null) || System.getProperty(prop).isEmpty()) {
			if ((System.getenv(prop) == null) || System.getenv(prop).isEmpty()) {
				System.err.println(errorMsg);
				System.exit(1);
			} else {
				System.setProperty(prop, System.getenv(prop));
			}
		}
	}
	
	private static void applyPropertyFromEnv(String prop) {
		if ((System.getProperty(prop) == null) || System.getProperty(prop).isEmpty()) {
			if ((System.getenv(prop) != null) && !(System.getenv(prop).isEmpty())) {
				System.setProperty(prop, System.getenv(prop));
			}
		}
	}
	
	private static void initVelocity() {
		try {
			// Use log4j
			Velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, Log4JLogChute.class.getCanonicalName());
			Velocity.setProperty("runtime.log.logsystem.log4j.logger", "org.apache.velocity");
			// Use UTF-8
			Velocity.setProperty("input.encoding", "UTF-8");
			Velocity.setProperty("output.encoding", "UTF-8");
			Velocity.init();
		} catch (Exception e) {
			System.err.println("Couldn't start up velocity.");
			System.exit(1);
		}
	}
	
	private Starter() {
		// prevent instantiation
	}
}
