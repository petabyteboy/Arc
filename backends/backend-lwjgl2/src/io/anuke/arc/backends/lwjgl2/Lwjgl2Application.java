package io.anuke.arc.backends.lwjgl2;

import io.anuke.arc.*;
import io.anuke.arc.backends.lwjgl2.audio.OpenALAudio;
import io.anuke.arc.collection.*;
import io.anuke.arc.util.*;
import org.lwjgl.opengl.Display;

import java.util.prefs.Preferences;

/** An OpenGL surface fullscreen or in a lightweight window. */
public class Lwjgl2Application implements Application{
    protected final Lwjgl2Graphics graphics;
    protected OpenALAudio audio;
    protected final Lwjgl2Files files;
    protected final Lwjgl2Input input;
    protected final Lwjgl2Net net;
    protected final SnapshotArray<ApplicationListener> listeners = new SnapshotArray<>(ApplicationListener.class);
    protected Thread mainLoopThread;
    protected boolean running = true;
    protected final Array<Runnable> runnables = new Array<>();
    protected final Array<Runnable> executedRunnables = new Array<>();

    public Lwjgl2Application(ApplicationListener listener, String title, int width, int height){
        this(listener, createConfig(title, width, height));
    }

    public Lwjgl2Application(ApplicationListener listener){
        this(listener, null, 640, 480);
    }

    public Lwjgl2Application(ApplicationListener listener, Lwjgl2ApplicationConfiguration config){
        this(listener, config, new Lwjgl2Graphics(config));
    }

    public Lwjgl2Application(ApplicationListener listener, Lwjgl2ApplicationConfiguration config, Lwjgl2Graphics graphics){
        Lwjgl2NativesLoader.load();

        if(config.title == null) config.title = listener.getClass().getSimpleName();
        this.graphics = graphics;
        if(!Lwjgl2ApplicationConfiguration.disableAudio){
            try{
                audio = new OpenALAudio(config.audioDeviceSimultaneousSources, config.audioDeviceBufferCount,
                config.audioDeviceBufferSize);
            }catch(Throwable t){
                Log.err("LwjglApplication: Couldn't initialize audio, disabling audio", t);
                Lwjgl2ApplicationConfiguration.disableAudio = true;
            }
        }
        files = new Lwjgl2Files();
        input = new Lwjgl2Input();
        net = new Lwjgl2Net();
        listeners.add(listener);

        Core.app = this;
        Core.graphics = graphics;
        Core.audio = audio;
        Core.files = files;
        Core.input = input;
        Core.net = net;
        Core.settings = new Settings();
        initialize();
    }

    private static Lwjgl2ApplicationConfiguration createConfig(String title, int width, int height){
        Lwjgl2ApplicationConfiguration config = new Lwjgl2ApplicationConfiguration();
        config.title = title;
        config.width = width;
        config.height = height;
        config.vSyncEnabled = true;
        return config;
    }

    private void initialize(){
        mainLoopThread = new Thread("LWJGL Application"){
            @Override
            public void run(){
                graphics.setVSync(graphics.config.vSyncEnabled);
                try{
                    Lwjgl2Application.this.mainLoop();
                }catch(Throwable t){
                    if(audio != null) audio.dispose();
                    Core.input.setCursorCatched(false);
                    if(t instanceof RuntimeException)
                        throw (RuntimeException)t;
                    else
                        throw new ArcRuntimeException(t);
                }
            }
        };
        mainLoopThread.start();
    }

    protected void mainLoop(){
        graphics.setupDisplay();

        synchronized(listeners){
            ApplicationListener[] arr = listeners.begin();
            for(int i = 0, n = listeners.size; i < n; ++i)
                arr[i].init();
            listeners.end();
        }
        graphics.resize = true;
        graphics.lastTime = System.nanoTime();
        boolean wasPaused = false;
        while(running){
            Display.processMessages();
            if(Display.isCloseRequested()) exit();

            boolean isMinimized = graphics.config.pauseWhenMinimized && !Display.isVisible();
            boolean isBackground = !Display.isActive();
            boolean paused = isMinimized || (isBackground && graphics.config.pauseWhenBackground);
            if(!wasPaused && paused){ // just been minimized
                wasPaused = true;
                synchronized(listeners){
                    ApplicationListener[] arr = listeners.begin();
                    for(int i = 0, n = listeners.size; i < n; ++i)
                        arr[i].pause();
                    listeners.end();
                }
            }
            if(wasPaused && !paused){ // just been restore from being minimized
                wasPaused = false;
                synchronized(listeners){
                    ApplicationListener[] arr = listeners.begin();
                    for(int i = 0, n = listeners.size; i < n; ++i)
                        arr[i].resume();
                    listeners.end();
                }
            }

            boolean shouldRender = false;
            graphics.config.x = Display.getX();
            graphics.config.y = Display.getY();
            if(graphics.resize || Display.wasResized()
            || (int)(Display.getWidth() * Display.getPixelScaleFactor()) != graphics.config.width
            || (int)(Display.getHeight() * Display.getPixelScaleFactor()) != graphics.config.height){
                graphics.resize = false;
                graphics.config.width = (int)(Display.getWidth() * Display.getPixelScaleFactor());
                graphics.config.height = (int)(Display.getHeight() * Display.getPixelScaleFactor());
                Core.gl.glViewport(0, 0, graphics.config.width, graphics.config.height);
                synchronized(listeners){
                    ApplicationListener[] arr = listeners.begin();
                    for(int i = 0, n = listeners.size; i < n; ++i)
                        arr[i].resize(graphics.config.width, graphics.config.height);
                    listeners.end();
                }
                shouldRender = true;
            }


            if(executeRunnables()) shouldRender = true;

            // If one of the runnables set running to false, for example after an exit().
            if(!running) break;

            input.update();
            if(graphics.shouldRender()) shouldRender = true;
            input.processEvents();
            if(audio != null) audio.update();

            if(isMinimized)
                shouldRender = false;
            else if(isBackground && graphics.config.backgroundFPS == -1) //
                shouldRender = false;

            int frameRate = isBackground ? graphics.config.backgroundFPS : graphics.config.foregroundFPS;
            if(shouldRender){
                graphics.updateTime();
                graphics.frameId++;
                for(ApplicationListener list : listeners){
                    list.update();
                }
                Display.update(false);
            }else{
                // Sleeps to avoid wasting CPU in an empty loop.
                if(frameRate == -1) frameRate = 10;
                if(frameRate == 0) frameRate = graphics.config.backgroundFPS;
                if(frameRate == 0) frameRate = 30;
            }
            if(frameRate > 0) Display.sync(frameRate);
            input.finishEvents();
        }

        synchronized(listeners){
            ApplicationListener[] arr = listeners.begin();
            for(int i = 0, n = listeners.size; i < n; ++i){
                arr[i].pause();
                arr[i].dispose();
            }
            listeners.end();
        }
        Display.destroy();
        if(audio != null) audio.dispose();
        if(graphics.config.forceExit) System.exit(-1);
    }

    public boolean executeRunnables(){
        synchronized(runnables){
            for(int i = runnables.size - 1; i >= 0; i--)
                executedRunnables.add(runnables.get(i));
            runnables.clear();
        }
        if(executedRunnables.size == 0) return false;
        do
            executedRunnables.pop().run();
        while(executedRunnables.size > 0);
        return true;
    }

    @Override
    public Array<ApplicationListener> getListeners(){
        return listeners;
    }

    @Override
    public void post(Runnable runnable){
        synchronized(runnables){
            runnables.add(runnable);
        }
    }

    @Override
    public ApplicationType getType(){
        return ApplicationType.Desktop;
    }

    @Override
    public int getVersion(){
        return 0;
    }

    public void stop(){
        running = false;
        try{
            mainLoopThread.join();
        }catch(Exception ex){
        }
    }

    @Override
    public long getJavaHeap(){
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    @Override
    public long getNativeHeap(){
        return getJavaHeap();
    }

    ObjectMap<String, Preferences> preferences = new ObjectMap<String, Preferences>();

    @Override
    public Clipboard getClipboard(){
        return new Lwjgl2Clipboard();
    }

    @Override
    public void exit(){
        post(() -> running = false);
    }
}
