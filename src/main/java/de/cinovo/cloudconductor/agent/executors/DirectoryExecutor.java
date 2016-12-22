package de.cinovo.cloudconductor.agent.executors;

import de.cinovo.cloudconductor.agent.exceptions.ExecutionError;
import de.cinovo.cloudconductor.agent.helper.FileHelper;
import de.cinovo.cloudconductor.api.model.Directory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by janweisssieker on 22.12.16.
 */
public class DirectoryExecutor implements IExecutor<Set<String>> {

    private Set<Directory> directories;
    private StringBuilder errors;
    private Set<String> restart;

    public DirectoryExecutor(Set<Directory> directories){
        this.directories = directories;
        this.restart = new HashSet<>();
    }

    public Set<String> getResult() {
        return this.restart;
    }

    @Override
    public IExecutor<Set<String>> execute() throws ExecutionError {
        for(Directory dir : this.directories){
            Path path = Paths.get(dir.getTargetPath());
            //check if directory already exists
            File dirs = new File(dir.getTargetPath());
            if(Files.notExists(path)){
                //create new directories and check directory modes and owner on success
                if(dirs.mkdirs()){
                    this.checkDirPermOwner(dirs, dir.getFileMode(), dir.getOwner(), dir.getGroup());
                }
            } else {
                this.checkDirPermOwner(dirs, dir.getFileMode(), dir.getOwner(), dir.getGroup());
            }
        }
        return this;
    }

    private void checkDirPermOwner(File dirs, String fm, String owner, String group) {
        String fileMode = FileHelper.fileModeIntToString(fm);
        try {
            if (!FileHelper.isFileMode(dirs, fileMode)){
                FileHelper.chmod(dirs, fileMode);
            }
        }catch (IOException e){
            this.errors.append("could not check/change directory mode");
            this.errors.append(System.lineSeparator());
        }
        //check file owner
        try {
            if(!FileHelper.isFileOwner(dirs, owner, group)){
                FileHelper.chown(dirs, owner, group);
            }
        } catch (IOException e){
            this.errors.append("could not check/change owner / group");
            this.errors.append(System.lineSeparator());
        }
    }



    @Override
    public boolean failed() {
        return !this.errors.toString().trim().isEmpty();
    }
}
