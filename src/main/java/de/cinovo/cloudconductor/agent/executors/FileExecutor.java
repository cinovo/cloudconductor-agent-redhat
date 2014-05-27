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
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.exceptions.TransformationErrorException;
import de.cinovo.cloudconductor.agent.helper.FileHelper;
import de.cinovo.cloudconductor.agent.helper.ServerCom;
import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.ConfigFile;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 */
public class FileExecutor implements IExecutor<Set<String>> {
	
	private Set<ConfigFile> files;
	private StringBuilder errors;
	private Set<String> restart;
	
	
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
		
		for (ConfigFile file : this.files) {
			File localFile = new File(file.getTargetPath());
			HashCode localFileHash = this.getChecksum(localFile);
			String serverFile;
			try {
				serverFile = ServerCom.getFileData(file);
			} catch (TransformationErrorException | CloudConductorException e) {
				continue;
			}
			HashCode serverFileHash = this.getChecksum(serverFile);
			
			boolean changeOccured = false;
			
			if (!serverFileHash.equals(localFileHash)) {
				try {
					Files.createParentDirs(localFile);
					Files.write(serverFile, localFile, Charset.forName("UTF-8"));
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
				if (!FileHelper.isFileOwner(localFile, file.getOwner(), file.getGroup())) {
					FileHelper.chown(localFile, file.getOwner(), file.getGroup());
					changeOccured = true;
				}
			} catch (IOException e) {
				this.errors.append("Failed to set user and/or group for file: " + localFile.getAbsolutePath());
				this.errors.append(System.lineSeparator());
			}
			
			// set file mode
			try {
				String fileMode = this.fileModeIntToString(file.getFileMode());
				if (!FileHelper.isFileMode(localFile, fileMode)) {
					FileHelper.chmod(localFile, fileMode);
					changeOccured = true;
				}
			} catch (IOException e) {
				this.errors.append("Failed to set chmod for file: " + localFile.getAbsolutePath());
				this.errors.append(System.lineSeparator());
			}
			
			// set services to restart
			if (file.isReloadable() && changeOccured) {
				this.restart.addAll(file.getDependentServices());
			}
		}
		
		if (!this.errors.toString().trim().isEmpty()) {
			throw new ExecutionError(this.errors.toString().trim());
		}
		return this;
	}
	
	private String fileModeIntToString(String mod) {
		char[] str;
		if (mod.length() > 3) {
			str = mod.substring(1, 3).toCharArray();
		} else {
			str = mod.toCharArray();
		}
		StringBuilder s = new StringBuilder();
		for (int k = 0; k < 3; k++) {
			char[] test = Integer.toBinaryString(Integer.parseInt(String.valueOf(str[k]))).toCharArray();
			if (test.length < 3) {
				test = new char[] {'0', test.length > 1 ? test[test.length - 2] : '0', test.length > 0 ? test[test.length - 1] : '0'};
			}
			for (int i = 0; i < 3; i++) {
				if (test[i] != '1') {
					s.append("-");
					continue;
				}
				switch (i) {
				case 0:
					s.append("r");
					break;
				case 1:
					s.append("w");
					break;
				case 2:
					s.append("x");
					break;
				}
			}
		}
		return s.toString();
	}
	
	@Override
	public boolean failed() {
		return !this.errors.toString().trim().isEmpty();
	}
	
	private HashCode getChecksum(String content) {
		return Hashing.md5().hashBytes(content.getBytes());
	}
	
	private HashCode getChecksum(File content) {
		try {
			return Files.hash(content, Hashing.md5());
		} catch (IOException e) {
			// should never happen, if it does-> leave checksum empty
			return null;
		}
	}
	
}
