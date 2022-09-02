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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.google.common.hash.HashCode;
import com.google.common.io.Files;
import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.exceptions.TransformationErrorException;
import de.cinovo.cloudconductor.agent.helper.FileHelper;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.ConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 */
public class FileExecutor implements IExecutor<Set<String>> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FileExecutor.class);
	
	private final Set<ConfigFile> files;
	private StringBuilder errors;
	private final Set<String> restart;
	
	
	/**
	 * @param files the files to handle
	 */
	public FileExecutor(Set<ConfigFile> files) {
		this.files = files;
		this.restart = new HashSet<>();
	}
	
	@Override
	public Set<String> getResult() {
		return this.restart;
	}
	
	@Override
	public IExecutor<Set<String>> execute() throws ExecutionError {
		this.errors = new StringBuilder();
		
		for (ConfigFile configFile : this.files) {
			boolean changeOccured = false;
			
			if (configFile.isDirectory()) {
				File dirs = new File(configFile.getTargetPath());
				String dirMode = configFile.getFileMode();
				
				if (!dirs.exists()) {
					// create missing directories
					if (dirs.mkdirs()) {
						changeOccured = true;
						this.checkDirPermOwner(dirs, dirMode, configFile.getOwner(), configFile.getGroup());
					}
				} else {
					changeOccured = this.checkDirPermOwner(dirs, dirMode, configFile.getOwner(), configFile.getGroup());
				}
			} else {
				
				// in case config file is a file
				File localFile = new File(configFile.getTargetPath());
				HashCode localFileHash = FileHelper.getChecksum(localFile);
				
				String serverFile;
				try {
					serverFile = ServerCom.getFileData(configFile);
				} catch (TransformationErrorException | CloudConductorException e) {
					FileExecutor.LOGGER.error("Error getting file data for config file '" + configFile.getName() + "': ", e);
					continue;
				}
				HashCode serverFileHash = FileHelper.getChecksum(serverFile);
				
				if (!serverFileHash.equals(localFileHash)) {
					try {
						FileExecutor.LOGGER.debug("Update config file '{}'...", configFile.getName());
						Files.createParentDirs(localFile);
						Files.write(serverFile, localFile, StandardCharsets.UTF_8);
						changeOccured = true;
					} catch (IOException e) {
						// add error to exception list
						this.errors.append("Failed to write file: " + localFile.getAbsolutePath());
						this.errors.append(System.lineSeparator());
						// just skip this file
						continue;
					}
				}
				
				// set file owner and group
				try {
					if (!FileHelper.isFileOwner(localFile, configFile.getOwner(), configFile.getGroup())) {
						FileHelper.chown(localFile, configFile.getOwner(), configFile.getGroup());
						changeOccured = true;
					}
				} catch (IOException e) {
					this.errors.append("Failed to set user and/or group for file: " + localFile.getAbsolutePath());
					this.errors.append(System.lineSeparator());
				}
				
				// set file mode
				try {
					String fileMode = FileHelper.fileModeIntToString(ServerCom.getFileMode(configFile.getName()));
					if (!FileHelper.isFileMode(localFile, fileMode)) {
						FileHelper.chmod(localFile, fileMode);
						changeOccured = true;
					}
				} catch (IOException e) {
					this.errors.append("Failed to set chmod for file: " + localFile.getAbsolutePath());
					this.errors.append(System.lineSeparator());
				} catch (CloudConductorException e) {
					this.errors.append(e.getMessage());
					this.errors.append(System.lineSeparator());
				}
			}
			
			// set services to restart
			if (configFile.isReloadable() && changeOccured) {
				Set<String> servicesToRestart = configFile.getDependentServices();
				this.restart.addAll(servicesToRestart);
				FileExecutor.LOGGER.debug("Config file changed, {} services have to be restarted!", servicesToRestart.size());
			}
		}
		
		if (!this.errors.toString().trim().isEmpty()) {
			throw new ExecutionError(this.errors.toString().trim());
		}
		return this;
	}
	
	@Override
	public boolean failed() {
		return !this.errors.toString().trim().isEmpty();
	}
	
	private boolean checkDirPermOwner(File dirs, String fm, String owner, String group) {
		boolean changeOccured = false;
		
		String fileMode = FileHelper.fileModeIntToString(fm);
		try {
			if (!FileHelper.isFileMode(dirs, fileMode)) {
				FileHelper.chmod(dirs, fileMode);
				changeOccured = true;
			}
		} catch (IOException e) {
			this.errors.append("could not check/change directory mode");
			this.errors.append(System.lineSeparator());
		}
		
		// check file owner
		try {
			if (!FileHelper.isFileOwner(dirs, owner, group)) {
				FileHelper.chown(dirs, owner, group);
				changeOccured = true;
			}
		} catch (IOException e) {
			this.errors.append("could not check/change owner / group");
			this.errors.append(System.lineSeparator());
		}
		
		return changeOccured;
	}
	
}
