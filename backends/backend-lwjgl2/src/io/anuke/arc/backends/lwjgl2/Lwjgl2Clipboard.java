package io.anuke.arc.backends.lwjgl2;

import io.anuke.arc.util.Clipboard;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.util.List;

/**
 * Clipboard implementation for desktop that uses the system clipboard via the default AWT {@link Toolkit}.
 * @author mzechner
 */
public class Lwjgl2Clipboard implements Clipboard, ClipboardOwner{
    @Override
    public String getContents(){
        try{
            java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);
            if(contents != null){
                if(contents.isDataFlavorSupported(DataFlavor.stringFlavor)){
                    try{
                        return (String)contents.getTransferData(DataFlavor.stringFlavor);
                    }catch(Exception ex){
                    }
                }
                if(contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){
                    try{
                        List<File> files = (List)contents.getTransferData(DataFlavor.javaFileListFlavor);
                        StringBuilder buffer = new StringBuilder(128);
                        for(int i = 0, n = files.size(); i < n; i++){
                            if(buffer.length() > 0) buffer.append('\n');
                            buffer.append(files.get(i).toString());
                        }
                        return buffer.toString();
                    }catch(RuntimeException ex){
                    }
                }
            }
        }catch(Exception ignored){ // Ignore JDK crashes sorting data flavors.
        }
        return "";
    }

    @Override
    public void setContents(String content){
        try{
            StringSelection stringSelection = new StringSelection(content);
            java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);
        }catch(Exception ignored){ // Ignore JDK crashes sorting data flavors.
        }
    }

    @Override
    public void lostOwnership(java.awt.datatransfer.Clipboard arg0, Transferable arg1){
    }
}
