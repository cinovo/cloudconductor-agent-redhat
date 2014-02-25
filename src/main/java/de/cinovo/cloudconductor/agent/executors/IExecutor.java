package de.cinovo.cloudconductor.agent.executors;

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


import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 * @param <T> the result
 */
public interface IExecutor<T> {
	
	/**
	 * @return the result of the execution
	 */
	public abstract T getResult();
	
	/**
	 * @return the executor
	 * @throws ExecutionError if an error during execution occurs
	 */
	public abstract IExecutor<T> execute() throws ExecutionError;
	
	/**
	 * @return true if exit value != 0
	 */
	public abstract boolean failed();
	
}
