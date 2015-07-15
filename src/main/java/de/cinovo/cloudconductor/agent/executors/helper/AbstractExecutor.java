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

import java.io.IOException;
import java.io.InputStream;

import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.executors.IExecutor;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * @param <T> the result type
 * 
 */
public abstract class AbstractExecutor<T> implements IExecutor<T> {
	
	protected int exitValue = -1;
	
	
	protected abstract Process genProcess() throws IOException;
	
	protected abstract void analyzeStream(String[] dev, String[] error) throws ExecutionError;
	
	/**
	 * @return the result of the execution
	 */
	@Override
	public abstract T getResult();
	
	/**
	 * @return the executor
	 * @throws ExecutionError if an error during execution occures
	 */
	@Override
	public IExecutor<T> execute() throws ExecutionError {
		Process p = null;
		try {
			p = this.genProcess();
		} catch (Exception e) {
			throw new ExecutionError("Error generating a process.", e);
		}
		if (p == null) {
			throw new ExecutionError("Error generating a process.");
		}
		StreamAnalyzer devAnalyzer = this.getAnalyzer(p.getInputStream());
		StreamAnalyzer errorAnalyzer = this.getAnalyzer(p.getErrorStream());
		devAnalyzer.start();
		errorAnalyzer.start();
		
		while (this.exitValue < 0) {
			try {
				this.exitValue = p.exitValue();
			} catch (IllegalThreadStateException e) {
				// sleep while waiting for process to finish
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// just ignore this one
				}
			}
		}
		this.analyzeStream(devAnalyzer.getValues(), errorAnalyzer.getValues());
		return this;
	}
	
	protected StreamAnalyzer getAnalyzer(InputStream stream) {
		return new DefaultStreamAnalyzer(stream);
	}
	
	/**
	 * @return true if exit value != 0
	 */
	@Override
	public boolean failed() {
		return this.exitValue != 0;
	}
	
}
