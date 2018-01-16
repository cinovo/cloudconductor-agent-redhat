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

import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.executors.helper.AbstractExecutor;
import de.cinovo.cloudconductor.api.model.PackageVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 *
 * @author psigloch
 */
public class InstalledPackages extends AbstractExecutor<List<PackageVersion>> {
	private static final String cmdYum = "yum list installed";
	private static final String delimiter = ";";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private List<PackageVersion> result = new ArrayList<>();


	@Override
	protected Process genProcess() throws IOException {
		return Runtime.getRuntime().exec(InstalledPackages.cmdYum);
	}

	@Override
	protected void analyzeStream(String[] dev, String[] error) throws ExecutionError {
		if(error.length > 0) {
			throw new ExecutionError("Error while collecting installed packages");
		}
		this.logger.debug("Found installed packages: ");
		for(String str : dev) {
			str = str.replaceAll("\\s+", delimiter);
			String[] arr = str.split(InstalledPackages.delimiter);
			String pkg = arr[0].split("\\.")[0];
			String version = arr[1].split(":")[arr[1].split(":").length - 1];
			String repo = arr[2].replace("@", "");
			PackageVersion packageVersion = new PackageVersion(pkg, version, null);
			Set<String> repos = new HashSet<>();
			repos.add(repo);
			packageVersion.setRepos(repos);
			this.logger.debug(pkg + " - " + version + " - " + repo);
			this.result.add(packageVersion);
		}
	}

	@Override
	public List<PackageVersion> getResult() {
		return this.result;
	}
}
