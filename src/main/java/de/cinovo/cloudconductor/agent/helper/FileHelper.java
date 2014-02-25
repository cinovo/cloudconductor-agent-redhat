package de.cinovo.cloudconductor.agent.helper;

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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Collection;
import java.util.Set;

import com.google.common.io.Files;

import de.cinovo.cloudconductor.api.lib.exceptions.CloudConductorException;
import de.cinovo.cloudconductor.api.model.SSHKey;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 */
public class FileHelper {
	
	private FileHelper() {
		// prevent initialization
	}
	
	/**
	 * @throws CloudConductorException thrown if yum url retrieval fails
	 * @throws IOException thrown if file couln't be generated
	 */
	public static void writeYumRepo() throws CloudConductorException, IOException {
		String yumName = System.getProperty(AgentVars.YUM_NAME_PROP);
		String fileName = AgentVars.YUM_REPO_FOLDER + yumName + AgentVars.YUM_REPO_ENDING;
		try (FileWriter writer = new FileWriter(new File(fileName))) {
			writer.append("[");
			writer.append(yumName);
			writer.append("]");
			writer.append(System.lineSeparator());
			writer.append("name=" + yumName + " deploy repository");
			writer.append(System.lineSeparator());
			writer.append("failovermethod=priority");
			writer.append(System.lineSeparator());
			writer.append("baseurl=");
			writer.append(ServerCom.getYumPath());
			writer.append(System.lineSeparator());
			writer.append("enabled=0");
			writer.append(System.lineSeparator());
			writer.append("metadata_expire=1h");
			writer.append(System.lineSeparator());
			writer.append("gpgcheck=0");
			writer.append(System.lineSeparator());
			writer.flush();
			writer.close();
		}
	}
	
	/**
	 * @param localFile the local file to use chown on
	 * @param owner the file owner to set
	 * @param group the file group to set
	 * @throws IOException if chown couldn't be edited
	 */
	public static void chown(File localFile, String owner, String group) throws IOException {
		Path localFilePath = Paths.get(localFile.getAbsolutePath());
		
		UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
		PosixFileAttributeView view = java.nio.file.Files.getFileAttributeView(localFilePath, PosixFileAttributeView.class);
		UserPrincipal fileOwner = lookupService.lookupPrincipalByName(owner);
		GroupPrincipal fileGroup = lookupService.lookupPrincipalByGroupName(group);
		view.setOwner(fileOwner);
		view.setGroup(fileGroup);
	}
	
	/**
	 * @param localFile the local file to use chmod on
	 * @param fileMode the filemode to set in PosixFilePermissions string type -> 764 = rwxrw-r--
	 * @throws IOException if chmod couldn't be edited
	 */
	public static void chmod(File localFile, String fileMode) throws IOException {
		Path localFilePath = Paths.get(localFile.getAbsolutePath());
		Set<PosixFilePermission> fileModeSet = PosixFilePermissions.fromString(fileMode);
		PosixFileAttributeView view = java.nio.file.Files.getFileAttributeView(localFilePath, PosixFileAttributeView.class);
		view.setPermissions(fileModeSet);
	}
	
	
	private static final String akFolder = "/root/.ssh/";
	private static final String akFile = "authorized_keys";
	private static final String akOwner = "root";
	private static final String akGroup = "group";
	private static final String akChmodFolder = "rwx------";
	private static final String akChmodFile = "rw-------";
	
	
	/**
	 * writes the authorized key file for root
	 * 
	 * @param keys the keys to write to the authorized keys file
	 * @throws IOException if something during write operation fails
	 */
	public static void writeRootAuthorizedKeys(Collection<SSHKey> keys) throws IOException {
		File folder = new File(FileHelper.akFolder);
		if (!folder.exists()) {
			folder.mkdirs();
			FileHelper.chown(folder, FileHelper.akOwner, FileHelper.akGroup);
			FileHelper.chmod(folder, FileHelper.akChmodFolder);
		}
		
		File file = new File(FileHelper.akFolder + FileHelper.akFile);
		if (!file.exists()) {
			Files.touch(file);
			FileHelper.chown(folder, FileHelper.akOwner, FileHelper.akGroup);
			FileHelper.chmod(folder, FileHelper.akChmodFile);
		}
		try (FileWriter writer = new FileWriter(file)) {
			for (SSHKey str : keys) {
				writer.append(str.getKey());
				writer.append(System.lineSeparator());
			}
			writer.flush();
		}
	}
}
