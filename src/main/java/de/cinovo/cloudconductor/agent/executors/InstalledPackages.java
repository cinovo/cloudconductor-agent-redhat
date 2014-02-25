package de.cinovo.cloudconductor.agent.executors;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.executors.helper.AbstractExecutor;
import de.cinovo.cloudconductor.api.model.PackageVersion;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 */
public class InstalledPackages extends AbstractExecutor<List<PackageVersion>> {
	
	private static final String cmd = "rpm -qa --queryformat %{NAME};%{VERSION}-%{RELEASE}\\n";
	private static final String delimiter = ";";
	private List<PackageVersion> result = new ArrayList<>();
	
	
	@Override
	protected Process genProcess() throws IOException {
		return Runtime.getRuntime().exec(InstalledPackages.cmd);
	}
	
	@Override
	protected void analyzeStream(String[] dev, String[] error) throws ExecutionError {
		if (error.length > 0) {
			throw new ExecutionError("Error while collecting installed packages");
		}
		for (String str : dev) {
			String[] arr = str.split(InstalledPackages.delimiter);
			this.result.add(new PackageVersion(arr[0], arr[1], null));
		}
	}
	
	@Override
	public List<PackageVersion> getResult() {
		return this.result;
	}
}
