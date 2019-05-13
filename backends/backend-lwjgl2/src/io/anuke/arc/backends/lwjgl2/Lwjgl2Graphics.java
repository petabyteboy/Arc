package io.anuke.arc.backends.lwjgl2;

import io.anuke.arc.*;
import io.anuke.arc.Graphics.Cursor.SystemCursor;
import io.anuke.arc.collection.Array;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.graphics.Pixmap.Blending;
import io.anuke.arc.graphics.Pixmap.Format;
import io.anuke.arc.graphics.glutils.GLVersion;
import io.anuke.arc.util.ArcRuntimeException;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;

/**
 * An implementation of the {@link Graphics} interface based on Lwjgl.
 * @author mzechner
 */
public class Lwjgl2Graphics extends Graphics{
    /** The suppored OpenGL extensions */
    static Array<String> extensions;
    static GLVersion glVersion;

    io.anuke.arc.graphics.GL20 gl20;
    long frameId = -1;
    float deltaTime = 0;
    long frameStart = 0;
    int frames = 0;
    int fps;
    long lastTime = System.nanoTime();
    boolean vsync = false;
    boolean resize = false;
    Lwjgl2ApplicationConfiguration config;
    BufferFormat bufferFormat = new BufferFormat(8, 8, 8, 8, 16, 8, 0, false);
    volatile boolean isContinuous = true;
    volatile boolean requestRendering = false;
    boolean softwareMode;

    Lwjgl2Graphics(Lwjgl2ApplicationConfiguration config){
        this.config = config;
    }

    public int getHeight(){
        return (int)(Display.getHeight() * Display.getPixelScaleFactor());
    }

    public int getWidth(){
        return (int)(Display.getWidth() * Display.getPixelScaleFactor());
    }

    @Override
    public int getBackBufferWidth(){
        return getWidth();
    }

    @Override
    public int getBackBufferHeight(){
        return getHeight();
    }

    @Override
    public io.anuke.arc.graphics.GL30 getGL30(){
        return null;
    }

    @Override
    public void setGL30(io.anuke.arc.graphics.GL30 gl30){

    }

    public long getFrameId(){
        return frameId;
    }

    public float getDeltaTime(){
        return deltaTime;
    }

    public float getRawDeltaTime(){
        return deltaTime;
    }

    public GLVersion getGLVersion(){
        return glVersion;
    }

    public boolean isGL20Available(){
        return gl20 != null;
    }

    public io.anuke.arc.graphics.GL20 getGL20(){
        return gl20;
    }

    @Override
    public void setGL20(io.anuke.arc.graphics.GL20 gl20){
        this.gl20 = gl20;
        Core.gl = gl20;
        Core.gl20 = gl20;
    }

    @Override
    public boolean isGL30Available(){
        return false;
    }

    public int getFramesPerSecond(){
        return fps;
    }

    void updateTime(){
        long time = System.nanoTime();
        deltaTime = (time - lastTime) / 1000000000.0f;
        lastTime = time;

        if(time - frameStart >= 1000000000){
            fps = frames;
            frames = 0;
            frameStart = time;
        }
        frames++;
    }

    void setupDisplay(){
        if(config.useHDPI){
            System.setProperty("org.lwjgl.opengl.Display.enableHighDPI", "true");
        }

        boolean displayCreated;

        if(!config.fullscreen){
            displayCreated = setWindowedMode(config.width, config.height);
        }else{
            DisplayMode bestMode = null;
            for(DisplayMode mode : getDisplayModes()){
                if(mode.width == config.width && mode.height == config.height){
                    if(bestMode == null || bestMode.refreshRate < this.getDisplayMode().refreshRate){
                        bestMode = mode;
                    }
                }
            }
            if(bestMode == null){
                bestMode = this.getDisplayMode();
            }
            displayCreated = setFullscreenMode(bestMode);
        }
        if(!displayCreated){
            if(config.setDisplayModeCallback != null){
                config = config.setDisplayModeCallback.onFailure(config);
                if(config != null){
                    displayCreated = setWindowedMode(config.width, config.height);
                }
            }
            if(!displayCreated){
                throw new ArcRuntimeException("Couldn't set display mode " + config.width + "x" + config.height + ", fullscreen: "
                + config.fullscreen);
            }
        }
        if(config.iconPaths.size > 0){
            ByteBuffer[] icons = new ByteBuffer[config.iconPaths.size];
            for(int i = 0, n = config.iconPaths.size; i < n; i++){
                Pixmap pixmap = new Pixmap(Core.files.getFileHandle(config.iconPaths.get(i), config.iconFileTypes.get(i)));
                if(pixmap.getFormat() != Format.RGBA8888){
                    Pixmap rgba = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Format.RGBA8888);
                    rgba.setBlending(Blending.None);
                    rgba.drawPixmap(pixmap, 0, 0);
                    pixmap.dispose();
                    pixmap = rgba;
                }
                icons[i] = ByteBuffer.allocateDirect(pixmap.getPixels().limit());
                icons[i].put(pixmap.getPixels()).flip();
                pixmap.dispose();
            }
            Display.setIcon(icons);
        }

        Display.setTitle(config.title);
        Display.setResizable(config.resizable);
        Display.setInitialBackground(config.initialBackgroundColor.r, config.initialBackgroundColor.g,
        config.initialBackgroundColor.b);

        Display.setLocation(config.x, config.y);
        createDisplayPixelFormat();
        initiateGL();
    }

    /**
     * Only needed when setupDisplay() is not called.
     */
    void initiateGL(){
        extractVersion();
        extractExtensions();
        initiateGLInstances();
    }

    private static void extractVersion(){
        String versionString = org.lwjgl.opengl.GL11.glGetString(GL11.GL_VERSION);
        String vendorString = org.lwjgl.opengl.GL11.glGetString(GL11.GL_VENDOR);
        String rendererString = org.lwjgl.opengl.GL11.glGetString(GL11.GL_RENDERER);
        glVersion = new GLVersion(Application.ApplicationType.Desktop, versionString, vendorString, rendererString);
    }

    private static void extractExtensions(){
        extensions = new Array<>();
        if(glVersion.isVersionEqualToOrHigher(3, 2)){
            int numExtensions = GL11.glGetInteger(GL30.GL_NUM_EXTENSIONS);
            for(int i = 0; i < numExtensions; ++i)
                extensions.add(org.lwjgl.opengl.GL30.glGetStringi(io.anuke.arc.graphics.GL20.GL_EXTENSIONS, i));
        }else{
            extensions.addAll(org.lwjgl.opengl.GL11.glGetString(io.anuke.arc.graphics.GL20.GL_EXTENSIONS).split(" "));
        }
    }

    /** @return whether the supported OpenGL (not ES) version is compatible with OpenGL ES 3.x. */
    private static boolean fullCompatibleWithGLES3(){
        // OpenGL ES 3.0 is compatible with OpenGL 4.3 core, see http://en.wikipedia.org/wiki/OpenGL_ES#OpenGL_ES_3.0
        return glVersion.isVersionEqualToOrHigher(4, 3);
    }

    /** @return whether the supported OpenGL (not ES) version is compatible with OpenGL ES 2.x. */
    private static boolean fullCompatibleWithGLES2(){
        // OpenGL ES 2.0 is compatible with OpenGL 4.1 core
        // see https://www.opengl.org/registry/specs/ARB/ES2_compatibility.txt
        return glVersion.isVersionEqualToOrHigher(4, 1) || extensions.contains("GL_ARB_ES2_compatibility", false);
    }

    private static boolean supportsFBO(){
        // FBO is in core since OpenGL 3.0, see https://www.opengl.org/wiki/Framebuffer_Object
        return glVersion.isVersionEqualToOrHigher(3, 0) || extensions.contains("GL_EXT_framebuffer_object", false)
        || extensions.contains("GL_ARB_framebuffer_object", false);
    }

    private void createDisplayPixelFormat(){
        try{

            Display.create(new PixelFormat(config.r + config.g + config.b, config.a, config.depth, config.stencil, config.samples));
            bufferFormat = new BufferFormat(config.r, config.g, config.b, config.a, config.depth, config.stencil, config.samples,
            false);
        }catch(Exception ex){
            Display.destroy();
            try{
                Thread.sleep(200);
            }catch(InterruptedException ignored){
            }
            try{
                Display.create(new PixelFormat(0, 16, 8));
                if(getDisplayMode().bitsPerPixel == 16){
                    bufferFormat = new BufferFormat(5, 6, 5, 0, 16, 8, 0, false);
                }
                if(getDisplayMode().bitsPerPixel == 24){
                    bufferFormat = new BufferFormat(8, 8, 8, 0, 16, 8, 0, false);
                }
                if(getDisplayMode().bitsPerPixel == 32){
                    bufferFormat = new BufferFormat(8, 8, 8, 8, 16, 8, 0, false);
                }
            }catch(Exception ex2){
                Display.destroy();
                try{
                    Thread.sleep(200);
                }catch(InterruptedException ignored){
                }
                try{
                    Display.create(new PixelFormat());
                }catch(Exception ex3){
                    if(!softwareMode && config.allowSoftwareMode){
                        softwareMode = true;
                        System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true");
                        createDisplayPixelFormat();
                        return;
                    }
                    throw new ArcRuntimeException("OpenGL is not supported by the video driver.", ex3);
                }
                if(getDisplayMode().bitsPerPixel == 16){
                    bufferFormat = new BufferFormat(5, 6, 5, 0, 8, 0, 0, false);
                }
                if(getDisplayMode().bitsPerPixel == 24){
                    bufferFormat = new BufferFormat(8, 8, 8, 0, 8, 0, 0, false);
                }
                if(getDisplayMode().bitsPerPixel == 32){
                    bufferFormat = new BufferFormat(8, 8, 8, 8, 8, 0, 0, false);
                }
            }
        }
    }

    public void initiateGLInstances(){
        gl20 = new Lwjgl2GL20();

        if(!glVersion.isVersionEqualToOrHigher(2, 0))
            throw new ArcRuntimeException("OpenGL 2.0 or higher with the FBO extension is required. OpenGL version: "
            + GL11.glGetString(GL11.GL_VERSION) + "\n" + glVersion.getDebugVersionString());

        if(!supportsFBO()){
            throw new ArcRuntimeException("OpenGL 2.0 or higher with the FBO extension is required. OpenGL version: "
            + GL11.glGetString(GL11.GL_VERSION) + ", FBO extension: false\n" + glVersion.getDebugVersionString());
        }

        Core.gl = gl20;
        Core.gl20 = gl20;
    }

    @Override
    public float getPpiX(){
        return java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
    }

    @Override
    public float getPpiY(){
        return java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
    }

    @Override
    public float getPpcX(){
        return (java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 2.54f);
    }

    @Override
    public float getPpcY(){
        return (java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 2.54f);
    }

    @Override
    public float getDensity(){
        if(config.overrideDensity != -1) return config.overrideDensity / 160f;
        return (java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 160f);
    }

    @Override
    public boolean supportsDisplayModeChange(){
        return true;
    }

    @Override
    public Monitor getPrimaryMonitor(){
        return new LwjglMonitor(0, 0, "Primary Monitor");
    }

    @Override
    public Monitor getMonitor(){
        return getPrimaryMonitor();
    }

    @Override
    public Monitor[] getMonitors(){
        return new Monitor[]{getPrimaryMonitor()};
    }

    @Override
    public DisplayMode[] getDisplayModes(Monitor monitor){
        return getDisplayModes();
    }

    @Override
    public DisplayMode getDisplayMode(Monitor monitor){
        return getDisplayMode();
    }

    @Override
    public boolean setFullscreenMode(DisplayMode displayMode){
        org.lwjgl.opengl.DisplayMode mode = ((LwjglDisplayMode)displayMode).mode;
        try{
            if(!mode.isFullscreenCapable()){
                Display.setDisplayMode(mode);
            }else{
                Display.setDisplayModeAndFullscreen(mode);
            }
            float scaleFactor = Display.getPixelScaleFactor();
            config.width = (int)(mode.getWidth() * scaleFactor);
            config.height = (int)(mode.getHeight() * scaleFactor);
            if(Core.gl != null) Core.gl.glViewport(0, 0, config.width, config.height);
            resize = true;
            return true;
        }catch(LWJGLException e){
            return false;
        }
    }

    /** Kindly stolen from http://lwjgl.org/wiki/index.php?title=LWJGL_Basics_5_(Fullscreen), not perfect but will do. */
    @Override
    public boolean setWindowedMode(int width, int height){
        if(getWidth() == width && getHeight() == height && !Display.isFullscreen()){
            return true;
        }

        try{
            org.lwjgl.opengl.DisplayMode targetDisplayMode = null;
            targetDisplayMode = new org.lwjgl.opengl.DisplayMode(width, height);

            boolean resizable = config.resizable;

            Display.setDisplayMode(targetDisplayMode);
            Display.setFullscreen(false);
            // Workaround for bug in LWJGL whereby resizable state is lost on DisplayMode change
            if(resizable == Display.isResizable()){
                Display.setResizable(!resizable);
            }
            Display.setResizable(resizable);

            float scaleFactor = Display.getPixelScaleFactor();
            config.width = (int)(targetDisplayMode.getWidth() * scaleFactor);
            config.height = (int)(targetDisplayMode.getHeight() * scaleFactor);
            if(Core.gl != null) Core.gl.glViewport(0, 0, config.width, config.height);
            resize = true;
            return true;
        }catch(LWJGLException e){
            return false;
        }
    }

    @Override
    public DisplayMode[] getDisplayModes(){
        try{
            org.lwjgl.opengl.DisplayMode[] availableDisplayModes = Display.getAvailableDisplayModes();
            DisplayMode[] modes = new DisplayMode[availableDisplayModes.length];

            int idx = 0;
            for(org.lwjgl.opengl.DisplayMode mode : availableDisplayModes){
                if(mode.isFullscreenCapable()){
                    modes[idx++] = new LwjglDisplayMode(mode.getWidth(), mode.getHeight(), mode.getFrequency(),
                    mode.getBitsPerPixel(), mode);
                }
            }

            return modes;
        }catch(LWJGLException e){
            throw new ArcRuntimeException("Couldn't fetch available display modes", e);
        }
    }

    @Override
    public DisplayMode getDisplayMode(){
        org.lwjgl.opengl.DisplayMode mode = Display.getDesktopDisplayMode();
        return new LwjglDisplayMode(mode.getWidth(), mode.getHeight(), mode.getFrequency(), mode.getBitsPerPixel(), mode);
    }

    @Override
    public void setTitle(String title){
        Display.setTitle(title);
    }

    /**
     * Display must be reconfigured via {@link #setWindowedMode(int, int)} for the changes to take
     * effect.
     */
    @Override
    public void setUndecorated(boolean undecorated){
        System.setProperty("org.lwjgl.opengl.Window.undecorated", undecorated ? "true" : "false");
    }

    /**
     * Display must be reconfigured via {@link #setWindowedMode(int, int)} for the changes to take
     * effect.
     */
    @Override
    public void setResizable(boolean resizable){
        this.config.resizable = resizable;
        Display.setResizable(resizable);
    }

    @Override
    public BufferFormat getBufferFormat(){
        return bufferFormat;
    }

    @Override
    public void setVSync(boolean vsync){
        this.vsync = vsync;
        Display.setVSyncEnabled(vsync);
    }

    @Override
    public boolean supportsExtension(String extension){
        return extensions.contains(extension, false);
    }

    @Override
    public void setContinuousRendering(boolean isContinuous){
        this.isContinuous = isContinuous;
    }

    @Override
    public boolean isContinuousRendering(){
        return isContinuous;
    }

    @Override
    public void requestRendering(){
        synchronized(this){
            requestRendering = true;
        }
    }

    public boolean shouldRender(){
        synchronized(this){
            boolean rq = requestRendering;
            requestRendering = false;
            return rq || isContinuous || Display.isDirty();
        }
    }

    @Override
    public boolean isFullscreen(){
        return Display.isFullscreen();
    }

    public boolean isSoftwareMode(){
        return softwareMode;
    }

    /** A callback used by LwjglApplication when trying to create the display */
    public interface SetDisplayModeCallback{
        /**
         * If the display creation fails, this method will be called. Suggested usage is to modify the passed configuration to use a
         * common width and height, and set fullscreen to false.
         * @return the configuration to be used for a second attempt at creating a display. A null value results in NOT attempting
         * to create the display a second time
         */
		Lwjgl2ApplicationConfiguration onFailure(Lwjgl2ApplicationConfiguration initialConfig);
    }

    @Override
    public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot){
        return new Lwjgl2Cursor(pixmap, xHotspot, yHotspot);
    }

    @Override
    public void setCursor(Cursor cursor){
        try{
            Mouse.setNativeCursor(((Lwjgl2Cursor)cursor).lwjglCursor);
        }catch(LWJGLException e){
            throw new ArcRuntimeException("Could not set cursor image.", e);
        }
    }

    @Override
    public void setSystemCursor(SystemCursor systemCursor){
        try{
            Mouse.setNativeCursor(null);
        }catch(LWJGLException e){
            throw new ArcRuntimeException("Couldn't set system cursor");
        }
    }

    private class LwjglDisplayMode extends DisplayMode{
        org.lwjgl.opengl.DisplayMode mode;

        public LwjglDisplayMode(int width, int height, int refreshRate, int bitsPerPixel, org.lwjgl.opengl.DisplayMode mode){
            super(width, height, refreshRate, bitsPerPixel);
            this.mode = mode;
        }
    }

    private class LwjglMonitor extends Monitor{
        protected LwjglMonitor(int virtualX, int virtualY, String name){
            super(virtualX, virtualY, name);
        }
    }
}
