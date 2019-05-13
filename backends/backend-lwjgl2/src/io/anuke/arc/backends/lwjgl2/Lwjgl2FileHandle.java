package io.anuke.arc.backends.lwjgl2;

import io.anuke.arc.Files.FileType;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.util.ArcRuntimeException;

import java.io.File;

/**
 * @author mzechner
 * @author Nathan Sweet
 */
public final class Lwjgl2FileHandle extends FileHandle{
    public Lwjgl2FileHandle(String fileName, FileType type){
        super(fileName, type);
    }

    public Lwjgl2FileHandle(File file, FileType type){
        super(file, type);
    }

    public FileHandle child(String name){
        if(file.getPath().length() == 0) return new Lwjgl2FileHandle(new File(name), type);
        return new Lwjgl2FileHandle(new File(file, name), type);
    }

    public FileHandle sibling(String name){
        if(file.getPath().length() == 0) throw new ArcRuntimeException("Cannot get the sibling of the root.");
        return new Lwjgl2FileHandle(new File(file.getParent(), name), type);
    }

    public FileHandle parent(){
        File parent = file.getParentFile();
        if(parent == null){
            if(type == FileType.Absolute)
                parent = new File("/");
            else
                parent = new File("");
        }
        return new Lwjgl2FileHandle(parent, type);
    }

    public File file(){
        if(type == FileType.External) return new File(Lwjgl2Files.externalPath, file.getPath());
        if(type == FileType.Local) return new File(Lwjgl2Files.localPath, file.getPath());
        return file;
    }
}
