package io.anuke.arc.backends.lwjgl2;

import io.anuke.arc.Files.FileType;
import io.anuke.arc.Graphics.DisplayMode;
import io.anuke.arc.collection.Array;
import io.anuke.arc.graphics.Color;

public class Lwjgl2ApplicationConfiguration{
    /**
     * If true, OpenAL will not be used. This means {@link Application#getAudio()} returns null and the gdx-openal.jar and OpenAL
     * natives are not needed.
     */
    static public boolean disableAudio;

    /** number of bits per color channel **/
    public int r = 8, g = 8, b = 8, a = 8;
    /** number of bits for depth and stencil buffer **/
    public int depth = 16, stencil = 0;
    /** number of samples for MSAA **/
    public int samples = 0;
    /** width & height of application window **/
    public int width = 640, height = 480;
    /** x & y of application window, -1 for center **/
    public int x = -1, y = -1;
    /** fullscreen **/
    public boolean fullscreen = false;
    /** used to emulate screen densities **/
    public int overrideDensity = -1;
    /** whether to enable vsync, can be changed at runtime via {@link Graphics#setVSync(boolean)} **/
    public boolean vSyncEnabled = true;
    /** title of application **/
    public String title;
    /** whether to call System.exit() on tear-down. Needed for Webstarts on some versions of Mac OS X it seems **/
    public boolean forceExit = true;
    /** whether the window is resizable **/
    public boolean resizable = true;
    /** the maximum number of sources that can be played simultaneously */
    public int audioDeviceSimultaneousSources = 16;
    /** the audio device buffer size in samples **/
    public int audioDeviceBufferSize = 512;
    /** the audio device buffer count **/
    public int audioDeviceBufferCount = 9;
    public Color initialBackgroundColor = Color.BLACK;
    /** Target framerate when the window is in the foreground. The CPU sleeps as needed. Use 0 to never sleep. **/
    public int foregroundFPS = 60;
    /**
     * Target framerate when the window is not in the foreground. The CPU sleeps as needed. Use 0 to never sleep, -1 to not
     * render.
     **/
    public int backgroundFPS = 60;
    /** {@link LifecycleListener#pause()} and don't render when the window is minimized. **/
    public boolean pauseWhenMinimized = true;
    /**
     * {@link LifecycleListener#pause()} (but continue rendering) when the window is not minimized and not the foreground window.
     * To stop rendering when not the foreground window, use backgroundFPS -1.
     **/
    public boolean pauseWhenBackground = false;
    /**
     * Allows software OpenGL rendering if hardware acceleration was not available.
     * @see Lwjgl2Graphics#isSoftwareMode()
     */
    public boolean allowSoftwareMode = false;
    /** Callback used when trying to create a display, can handle failures, default value is null (disabled) */
    public Lwjgl2Graphics.SetDisplayModeCallback setDisplayModeCallback;
    /** enable HDPI mode on Mac OS X **/
    public boolean useHDPI = false;

    Array<String> iconPaths = new Array();
    Array<FileType> iconFileTypes = new Array();

    /**
     * Adds a window icon. Icons are tried in the order added, the first one that works is used. Typically three icons should be
     * provided: 128x128 (for Mac), 32x32 (for Windows and Linux), and 16x16 (for Windows).
     */
    public void addIcon(String path, FileType fileType){
        iconPaths.add(path);
        iconFileTypes.add(fileType);
    }

    /**
     * Sets the r, g, b and a bits per channel based on the given {@link DisplayMode} and sets the fullscreen flag to true.
     */
    public void setFromDisplayMode(DisplayMode mode){
        this.width = mode.width;
        this.height = mode.height;
        if(mode.bitsPerPixel == 16){
            this.r = 5;
            this.g = 6;
            this.b = 5;
            this.a = 0;
        }
        if(mode.bitsPerPixel == 24){
            this.r = 8;
            this.g = 8;
            this.b = 8;
            this.a = 0;
        }
        if(mode.bitsPerPixel == 32){
            this.r = 8;
            this.g = 8;
            this.b = 8;
            this.a = 8;
        }
        this.fullscreen = true;
    }

    protected static class LwjglApplicationConfigurationDisplayMode extends DisplayMode{
        protected LwjglApplicationConfigurationDisplayMode(int width, int height, int refreshRate, int bitsPerPixel){
            super(width, height, refreshRate, bitsPerPixel);
        }
    }
}
