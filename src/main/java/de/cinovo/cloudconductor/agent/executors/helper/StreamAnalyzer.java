package de.cinovo.cloudconductor.agent.executors.helper;

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


import java.io.InputStream;
import java.util.Scanner;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 */
public abstract class StreamAnalyzer extends Thread {
	
	private InputStream inputStream;
	
	
	protected abstract void handleLine(String line);
	
	protected abstract String[] getValues();
	
	/**
	 * @param stream the stream to read
	 */
	public StreamAnalyzer(InputStream stream) {
		this.setDaemon(true);
		this.inputStream = stream;
	}
	
	@Override
	public void run() {
		try (Scanner sc = new Scanner(this.inputStream)) {
			while (sc.hasNextLine()) {
				this.handleLine(sc.nextLine());
			}
		}
	}
}
