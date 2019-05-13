package io.anuke.arc.backends.lwjgl2;

import io.anuke.arc.util.*;

import java.io.File;

import static io.anuke.arc.util.OS.*;

public final class Lwjgl2NativesLoader{
    static public boolean load = true;

    static{
        System.setProperty("org.lwjgl.input.Mouse.allowNegativeMouseCoords", "true");
    }

    /** Extracts the LWJGL native libraries from the classpath and sets the "org.lwjgl.librarypath" system property. */
    static public void load(){
        ArcNativesLoader.load();
        if(ArcNativesLoader.disableNativesLoading) return;
        if(!load) return;

        SharedLibraryLoader loader = new SharedLibraryLoader();
        File nativesDir = null;
        try{
            if(isWindows){
                nativesDir = loader.extractFile(is64Bit ? "lwjgl64.dll" : "lwjgl.dll", null).getParentFile();
                if(!Lwjgl2ApplicationConfiguration.disableAudio)
                    loader.extractFileTo(is64Bit ? "OpenAL64.dll" : "OpenAL32.dll", nativesDir);
            }else if(isMac){
                nativesDir = loader.extractFile("liblwjgl.dylib", null).getParentFile();
                if(!Lwjgl2ApplicationConfiguration.disableAudio) loader.extractFileTo("openal.dylib", nativesDir);
            }else if(isLinux){
                nativesDir = loader.extractFile(is64Bit ? "liblwjgl64.so" : "liblwjgl.so", null).getParentFile();
                if(!Lwjgl2ApplicationConfiguration.disableAudio)
                    loader.extractFileTo(is64Bit ? "libopenal64.so" : "libopenal.so", nativesDir);
            }
        }catch(Throwable ex){
            throw new ArcRuntimeException("Unable to extract LWJGL natives.", ex);
        }
        System.setProperty("org.lwjgl.librarypath", nativesDir.getAbsolutePath());
        load = false;
    }
}
