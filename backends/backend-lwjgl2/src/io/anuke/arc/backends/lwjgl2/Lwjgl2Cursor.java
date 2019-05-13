package io.anuke.arc.backends.lwjgl2;

import io.anuke.arc.Graphics.Cursor;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.util.ArcRuntimeException;
import org.lwjgl.LWJGLException;

import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class Lwjgl2Cursor implements Cursor{
    org.lwjgl.input.Cursor lwjglCursor;

    public Lwjgl2Cursor(Pixmap pixmap, int xHotspot, int yHotspot){
        try{
            if(pixmap == null){
                lwjglCursor = null;
                return;
            }

            if(pixmap.getFormat() != Pixmap.Format.RGBA8888){
                throw new ArcRuntimeException("Cursor image pixmap is not in RGBA8888 format.");
            }

            if((pixmap.getWidth() & (pixmap.getWidth() - 1)) != 0){
                throw new ArcRuntimeException("Cursor image pixmap width of " + pixmap.getWidth()
                + " is not a power-of-two greater than zero.");
            }

            if((pixmap.getHeight() & (pixmap.getHeight() - 1)) != 0){
                throw new ArcRuntimeException("Cursor image pixmap height of " + pixmap.getHeight()
                + " is not a power-of-two greater than zero.");
            }

            if(xHotspot < 0 || xHotspot >= pixmap.getWidth()){
                throw new ArcRuntimeException("xHotspot coordinate of " + xHotspot + " is not within image width bounds: [0, "
                + pixmap.getWidth() + ").");
            }

            if(yHotspot < 0 || yHotspot >= pixmap.getHeight()){
                throw new ArcRuntimeException("yHotspot coordinate of " + yHotspot + " is not within image height bounds: [0, "
                + pixmap.getHeight() + ").");
            }

            // Convert from RGBA8888 to ARGB8888 and flip vertically
            IntBuffer pixelBuffer = pixmap.getPixels().asIntBuffer();
            int[] pixelsRGBA = new int[pixelBuffer.capacity()];
            pixelBuffer.get(pixelsRGBA);
            int[] pixelsARGBflipped = new int[pixelBuffer.capacity()];
            int pixel;
            if(pixelBuffer.order() == ByteOrder.BIG_ENDIAN){
                for(int y = 0; y < pixmap.getHeight(); ++y){
                    for(int x = 0; x < pixmap.getWidth(); ++x){
                        pixel = pixelsRGBA[x + (y * pixmap.getWidth())];
                        pixelsARGBflipped[x + ((pixmap.getHeight() - 1 - y) * pixmap.getWidth())] = ((pixel >> 8) & 0x00FFFFFF)
                        | ((pixel << 24) & 0xFF000000);
                    }
                }
            }else{
                for(int y = 0; y < pixmap.getHeight(); ++y){
                    for(int x = 0; x < pixmap.getWidth(); ++x){
                        pixel = pixelsRGBA[x + (y * pixmap.getWidth())];
                        pixelsARGBflipped[x + ((pixmap.getHeight() - 1 - y) * pixmap.getWidth())] = ((pixel & 0xFF) << 16)
                        | ((pixel & 0xFF0000) >> 16) | (pixel & 0xFF00FF00);
                    }
                }
            }

            lwjglCursor = new org.lwjgl.input.Cursor(pixmap.getWidth(), pixmap.getHeight(), xHotspot, pixmap.getHeight() - yHotspot
            - 1, 1, IntBuffer.wrap(pixelsARGBflipped), null);
        }catch(LWJGLException e){
            throw new ArcRuntimeException("Could not create cursor image.", e);
        }
    }

    @Override
    public void dispose(){
    }
}
