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

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import de.cinovo.cloudconductor.api.model.Repo;
import de.cinovo.cloudconductor.api.model.RepoMirror;
import de.cinovo.cloudconductor.api.model.SSHKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author psigloch
 * 
 */
public class FileHelper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class);
	
	
	private FileHelper() {
		// prevent initialization
	}
	
	/**
	 * @param repoToWrite the repository for which a yum repo file should be written
	 * @throws IOException thrown if file couln't be generated
	 * @return the file written
	 */
	public static File writeYumRepo(Repo repoToWrite) throws IOException {
		String yumName = repoToWrite.getName();
		
		String baseurl = null;
		Long mirrorIndex = repoToWrite.getPrimaryMirror();
		for (RepoMirror mirror : repoToWrite.getMirrors()) {
			if (mirror.getId().equals(mirrorIndex)) {
				baseurl = mirror.getPath();
				break;
			}
		}
		FileHelper.LOGGER.debug("Found baseurl '" + baseurl + "'");
		
		String fileName = AgentVars.YUM_REPO_FOLDER + AgentVars.YUM_REPO_PREFIX + yumName + AgentVars.YUM_REPO_ENDING;
		
		StringBuilder repoStr = new StringBuilder();
		repoStr.append("[");
		repoStr.append(yumName);
		repoStr.append("]");
		repoStr.append(System.lineSeparator());
		
		repoStr.append("name=" + yumName + " deploy repository");
		repoStr.append(System.lineSeparator());
		repoStr.append("baseurl=");
		repoStr.append(baseurl);
		repoStr.append(System.lineSeparator());
		repoStr.append("enabled=0");
		repoStr.append(System.lineSeparator());
		repoStr.append("metadata_expire=1h");
		repoStr.append(System.lineSeparator());
		repoStr.append("gpgcheck=0");
		repoStr.append(System.lineSeparator());
		
		// check whether file must be written
		File yumRepoFile = new File(fileName);
		if (yumRepoFile.exists()) {
			HashCode checksumFile = FileHelper.getChecksum(yumRepoFile);
			HashCode checksumString = FileHelper.getChecksum(repoStr.toString());
			
			if (checksumFile.equals(checksumString)) {
				FileHelper.LOGGER.debug("No changes for repo file '" + fileName + "'.");
				return yumRepoFile;
			}
		}
		
		FileHelper.LOGGER.info("Write yum repo file '{}'", fileName);
		return FileHelper.writeFileAndReturn(fileName, repoStr.toString());
	}
	
	/**
	 * @param filePath the path of the file to write
	 * @param content the string content to write
	 * @throws IOException thrown if file could not be generated
	 */
	public static void writeFile(String filePath, String content) throws IOException {
		FileHelper.LOGGER.debug("Start to write into file '" + filePath + "'...");
		try (FileWriter writer = new FileWriter(new File(filePath))) {
			writer.append(content);
			writer.flush();
			writer.close();
		}
	}
	
	/**
	 * @param filePath the path of the file to write
	 * @param content the string content to write
	 * @throws IOException thrown if file could not be generated
	 * @return the file written
	 */
	private static File writeFileAndReturn(String filePath, String content) throws IOException {
		File file = new File(filePath);
		try (FileWriter writer = new FileWriter(file)) {
			writer.append(content);
			writer.flush();
		}
		return file;
	}
	
	/**
	 * @param localFile the local file
	 * @return the file attribute view
	 */
	public static PosixFileAttributeView getFileAttributes(File localFile) {
		Path localFilePath = Paths.get(localFile.getAbsolutePath());
		return java.nio.file.Files.getFileAttributeView(localFilePath, PosixFileAttributeView.class);
	}
	
	/**
	 * Check if a file has a specific file mode or not
	 * 
	 * @param file the file to check
	 * @param fileMode the file mode to check
	 * @return file mode is equal or not
	 * @throws IOException on error
	 */
	public static boolean isFileMode(File file, String fileMode) throws IOException {
		PosixFileAttributeView view = FileHelper.getFileAttributes(file);
		Set<PosixFilePermission> fileModeSet = PosixFilePermissions.fromString(fileMode);
		if (view.readAttributes().permissions().equals(fileModeSet)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Checks whether a files owner and group are correct
	 * 
	 * @param file the file to check
	 * @param owner the owner
	 * @param group the group
	 * @return owner and group of file are correct or not
	 * @throws IOException on error
	 */
	public static boolean isFileOwner(File file, String owner, String group) throws IOException {
		PosixFileAttributeView view = FileHelper.getFileAttributes(file);
		if (!view.readAttributes().owner().getName().equals(owner)) {
			return false;
		}
		if (!view.readAttributes().group().getName().equals(group)) {
			return false;
		}
		return true;
	}
	
	/**
	 * @param localFile the local file to use chown on
	 * @param owner the file owner to set
	 * @param group the file group to set
	 * @throws IOException if chown couldn't be edited
	 */
	public static void chown(File localFile, String owner, String group) throws IOException {
		PosixFileAttributeView view = FileHelper.getFileAttributes(localFile);
		UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
		UserPrincipal fileOwner = lookupService.lookupPrincipalByName(owner);
		GroupPrincipal fileGroup = lookupService.lookupPrincipalByGroupName(group);
		view.setOwner(fileOwner);
		view.setGroup(fileGroup);
	}
	
	/**
	 * @param localFile the local file to use chmod on
	 * @param fileMode the filemode to set in PosixFilePermissions string type - 764 = rwxrw-r--
	 * @throws IOException if chmod couldn't be edited
	 */
	public static void chmod(File localFile, String fileMode) throws IOException {
		PosixFileAttributeView view = FileHelper.getFileAttributes(localFile);
		Set<PosixFilePermission> fileModeSet = PosixFilePermissions.fromString(fileMode);
		view.setPermissions(fileModeSet);
	}
	
	
	private static final String akFile = "authorized_keys";
	private static final String akChmodFolder = "rwx------";
	private static final String akChmodFile = "rw-r--r--";
	
	private static final String ROOT_USERDIR = "/root/";
	
	
	/**
	 * Write the 'authorized_keys' file for a given user.
	 * 
	 * @param username the name of the user
	 * @param keys collection of SSH keys to write into the file
	 * @throws IOException if crating or writing the file or its directories fails
	 */
	public static void writeAuthorizedKeysForUser(String username, Collection<SSHKey> keys) throws IOException {
		String folderPath = FileHelper.getUserDirectory(username) + "/.ssh/";
		
		File folder = new File(folderPath);
		if (!folder.exists()) {
			folder.mkdirs();
			FileHelper.chown(folder, username, username);
			FileHelper.chmod(folder, FileHelper.akChmodFolder);
		}
		
		File file = new File(folderPath + FileHelper.akFile);
		if (!file.exists()) {
			Files.touch(file);
			FileHelper.chown(file, username, username);
			FileHelper.chmod(file, FileHelper.akChmodFile);
		}
		
		StringBuilder keyStr = new StringBuilder();
		for (SSHKey key : keys) {
			keyStr.append(key.getKey());
			keyStr.append(" ");
			keyStr.append(username);
			keyStr.append(System.lineSeparator());
		}
		
		HashCode checksumStr = FileHelper.getChecksum(keyStr.toString());
		HashCode checksumFile = FileHelper.getChecksum(file);
		
		if (!checksumStr.equals(checksumFile)) {
			FileHelper.writeFile(file.getAbsolutePath(), keyStr.toString());
		} else {
			FileHelper.LOGGER.debug("No changes for authorized_keys of user '" + username + "'.");
		}
	}
	
	private static String getUserDirectory(String username) {
		if (username.equals("root")) {
			return FileHelper.ROOT_USERDIR;
		}
		return "/home/" + username;
	}
	
	/**
	 * @param mod the int value of the file mode
	 * @return the string representation of the file mode
	 */
	public static String fileModeIntToString(String mod) {
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
	
	/**
	 * 
	 * @param content the content for which to compute the HashCode
	 * @return HashCode of the given string content
	 */
	public static HashCode getChecksum(String content) {
		return Hashing.md5().hashBytes(content.getBytes());
	}
	
	/**
	 * 
	 * @param content the file for which the checksum should be computed
	 * @return HashCode or null if an exception occurred reading the file
	 */
	public static HashCode getChecksum(File content) {
		try {
			return Files.hash(content, Hashing.md5());
		} catch (IOException e) {
			FileHelper.LOGGER.error("Error computing hash: ", e);
			return null;
		}
	}
}
