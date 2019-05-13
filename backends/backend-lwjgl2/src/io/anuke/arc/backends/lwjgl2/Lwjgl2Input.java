package io.anuke.arc.backends.lwjgl2;

import io.anuke.arc.Core;
import io.anuke.arc.Input;
import io.anuke.arc.collection.IntSet;
import io.anuke.arc.input.*;
import io.anuke.arc.util.pooling.Pool;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of the {@link Input} interface hooking a LWJGL panel for input.
 * @author mzechner
 */
final public class Lwjgl2Input extends Input{
    static public float keyRepeatInitialTime = 0.4f;
    static public float keyRepeatTime = 0.1f;

    List<KeyEvent> keyEvents = new ArrayList<>();
    List<TouchEvent> touchEvents = new ArrayList<>();
    int mouseX, mouseY;
    int deltaX, deltaY;
    int pressedKeys = 0;
    boolean keyJustPressed = false;
    boolean[] justPressedKeys = new boolean[256];
    boolean[] justPressedButtons = new boolean[256];
    boolean justTouched = false;
    IntSet pressedButtons = new IntSet();
    char lastKeyCharPressed;
    float keyRepeatTimer;
    long currentEventTimeStamp;
    float deltaTime;
    long lastTime;

    Pool<KeyEvent> usedKeyEvents = new Pool<KeyEvent>(16, 1000){
        protected KeyEvent newObject(){
            return new KeyEvent();
        }
    };

    Pool<TouchEvent> usedTouchEvents = new Pool<TouchEvent>(16, 1000){
        protected TouchEvent newObject(){
            return new TouchEvent();
        }
    };

    public Lwjgl2Input(){
        Keyboard.enableRepeatEvents(false);
        Mouse.setClipMouseCoordinatesToWindow(false);
    }

    @Override
    public int mouseX(){
        return (int)(Mouse.getX() * Display.getPixelScaleFactor());
    }

    @Override
    public int mouseY(){
        return (int)(Mouse.getY() * Display.getPixelScaleFactor());
    }

    @Override
    public boolean isTouched(){
        return Mouse.isButtonDown(0) || Mouse.isButtonDown(1) || Mouse.isButtonDown(2);
    }

    public int mouseX(int pointer){
        if(pointer > 0)
            return 0;
        else
            return mouseX();
    }

    public int mouseY(int pointer){
        if(pointer > 0)
            return 0;
        else
            return mouseY();
    }

    public boolean isTouched(int pointer){
        if(pointer > 0)
            return false;
        else
            return isTouched();
    }

    @Override
    public float getPressure(){
        return getPressure(0);
    }

    @Override
    public float getPressure(int pointer){
        return isTouched(pointer) ? 1 : 0;
    }

    public boolean supportsMultitouch(){
        return false;
    }

    @Override
    public void setOnscreenKeyboardVisible(boolean visible){

    }

    void finishEvents(){
        for(InputDevice device : devices){
            device.update();
        }
    }

    void processEvents(){
        synchronized(this){
            InputProcessor processor = this.inputMultiplexer;
            int len = keyEvents.size();
            for(int i = 0; i < len; i++){
                KeyEvent e = keyEvents.get(i);
                currentEventTimeStamp = e.timeStamp;
                switch(e.type){
                    case KeyEvent.KEY_DOWN:
                        processor.keyDown(e.keyCode);
                        break;
                    case KeyEvent.KEY_UP:
                        processor.keyUp(e.keyCode);
                        break;
                    case KeyEvent.KEY_TYPED:
                        processor.keyTyped(e.keyChar);
                }
                usedKeyEvents.free(e);
            }

            len = touchEvents.size();
            for(int i = 0; i < len; i++){
                TouchEvent e = touchEvents.get(i);
                currentEventTimeStamp = e.timeStamp;
                switch(e.type){
                    case TouchEvent.TOUCH_DOWN:
                        processor.touchDown(e.x, e.y, e.pointer, e.button);
                        processor.keyDown(e.button);
                        break;
                    case TouchEvent.TOUCH_UP:
                        processor.touchUp(e.x, e.y, e.pointer, e.button);
                        processor.keyUp(e.button);
                        break;
                    case TouchEvent.TOUCH_DRAGGED:
                        processor.touchDragged(e.x, e.y, e.pointer);
                        break;
                    case TouchEvent.TOUCH_MOVED:
                        processor.mouseMoved(e.x, e.y);
                        break;
                    case TouchEvent.TOUCH_SCROLLED:
                        processor.scrolled(0f, e.scrollAmount);
                }
                usedTouchEvents.free(e);
            }


            keyEvents.clear();
            touchEvents.clear();
        }
    }

    public static KeyCode getGdxKeyCode(int lwjglKeyCode){
        switch(lwjglKeyCode){
            case Keyboard.KEY_LBRACKET:
                return KeyCode.LEFT_BRACKET;
            case Keyboard.KEY_RBRACKET:
                return KeyCode.RIGHT_BRACKET;
            case Keyboard.KEY_GRAVE:
                return KeyCode.GRAVE;
            case Keyboard.KEY_MULTIPLY:
                return KeyCode.STAR;
            case Keyboard.KEY_NUMLOCK:
                return KeyCode.NUM;
            case Keyboard.KEY_DECIMAL:
                return KeyCode.PERIOD;
            case Keyboard.KEY_DIVIDE:
                return KeyCode.SLASH;
            case Keyboard.KEY_LMETA:
                return KeyCode.SYM;
            case Keyboard.KEY_RMETA:
                return KeyCode.SYM;
            case Keyboard.KEY_NUMPADEQUALS:
                return KeyCode.EQUALS;
            case Keyboard.KEY_AT:
                return KeyCode.AT;
            case Keyboard.KEY_EQUALS:
                return KeyCode.EQUALS;
            case Keyboard.KEY_NUMPADCOMMA:
                return KeyCode.COMMA;
            case Keyboard.KEY_NUMPADENTER:
                return KeyCode.ENTER;
            case Keyboard.KEY_0:
                return KeyCode.NUM_0;
            case Keyboard.KEY_1:
                return KeyCode.NUM_1;
            case Keyboard.KEY_2:
                return KeyCode.NUM_2;
            case Keyboard.KEY_3:
                return KeyCode.NUM_3;
            case Keyboard.KEY_4:
                return KeyCode.NUM_4;
            case Keyboard.KEY_5:
                return KeyCode.NUM_5;
            case Keyboard.KEY_6:
                return KeyCode.NUM_6;
            case Keyboard.KEY_7:
                return KeyCode.NUM_7;
            case Keyboard.KEY_8:
                return KeyCode.NUM_8;
            case Keyboard.KEY_9:
                return KeyCode.NUM_9;
            case Keyboard.KEY_A:
                return KeyCode.A;
            case Keyboard.KEY_B:
                return KeyCode.B;
            case Keyboard.KEY_C:
                return KeyCode.C;
            case Keyboard.KEY_D:
                return KeyCode.D;
            case Keyboard.KEY_E:
                return KeyCode.E;
            case Keyboard.KEY_F:
                return KeyCode.F;
            case Keyboard.KEY_G:
                return KeyCode.G;
            case Keyboard.KEY_H:
                return KeyCode.H;
            case Keyboard.KEY_I:
                return KeyCode.I;
            case Keyboard.KEY_J:
                return KeyCode.J;
            case Keyboard.KEY_K:
                return KeyCode.K;
            case Keyboard.KEY_L:
                return KeyCode.L;
            case Keyboard.KEY_M:
                return KeyCode.M;
            case Keyboard.KEY_N:
                return KeyCode.N;
            case Keyboard.KEY_O:
                return KeyCode.O;
            case Keyboard.KEY_P:
                return KeyCode.P;
            case Keyboard.KEY_Q:
                return KeyCode.Q;
            case Keyboard.KEY_R:
                return KeyCode.R;
            case Keyboard.KEY_S:
                return KeyCode.S;
            case Keyboard.KEY_T:
                return KeyCode.T;
            case Keyboard.KEY_U:
                return KeyCode.U;
            case Keyboard.KEY_V:
                return KeyCode.V;
            case Keyboard.KEY_W:
                return KeyCode.W;
            case Keyboard.KEY_X:
                return KeyCode.X;
            case Keyboard.KEY_Y:
                return KeyCode.Y;
            case Keyboard.KEY_Z:
                return KeyCode.Z;
            case Keyboard.KEY_LMENU:
                return KeyCode.ALT_LEFT;
            case Keyboard.KEY_RMENU:
                return KeyCode.ALT_RIGHT;
            case Keyboard.KEY_BACKSLASH:
                return KeyCode.BACKSLASH;
            case Keyboard.KEY_COMMA:
                return KeyCode.COMMA;
            case Keyboard.KEY_DELETE:
                return KeyCode.FORWARD_DEL;
            case Keyboard.KEY_LEFT:
                return KeyCode.DPAD_LEFT;
            case Keyboard.KEY_RIGHT:
                return KeyCode.DPAD_RIGHT;
            case Keyboard.KEY_UP:
                return KeyCode.DPAD_UP;
            case Keyboard.KEY_DOWN:
                return KeyCode.DPAD_DOWN;
            case Keyboard.KEY_RETURN:
                return KeyCode.ENTER;
            case Keyboard.KEY_HOME:
                return KeyCode.HOME;
            case Keyboard.KEY_MINUS:
                return KeyCode.MINUS;
            case Keyboard.KEY_PERIOD:
                return KeyCode.PERIOD;
            case Keyboard.KEY_ADD:
                return KeyCode.PLUS;
            case Keyboard.KEY_SEMICOLON:
                return KeyCode.SEMICOLON;
            case Keyboard.KEY_LSHIFT:
                return KeyCode.SHIFT_LEFT;
            case Keyboard.KEY_RSHIFT:
                return KeyCode.SHIFT_RIGHT;
            case Keyboard.KEY_SLASH:
                return KeyCode.SLASH;
            case Keyboard.KEY_SPACE:
                return KeyCode.SPACE;
            case Keyboard.KEY_TAB:
                return KeyCode.TAB;
            case Keyboard.KEY_LCONTROL:
                return KeyCode.CONTROL_LEFT;
            case Keyboard.KEY_RCONTROL:
                return KeyCode.CONTROL_RIGHT;
            case Keyboard.KEY_NEXT:
                return KeyCode.PAGE_DOWN;
            case Keyboard.KEY_PRIOR:
                return KeyCode.PAGE_UP;
            case Keyboard.KEY_ESCAPE:
                return KeyCode.ESCAPE;
            case Keyboard.KEY_END:
                return KeyCode.END;
            case Keyboard.KEY_INSERT:
                return KeyCode.INSERT;
            case Keyboard.KEY_BACK:
                return KeyCode.DEL;
            case Keyboard.KEY_SUBTRACT:
                return KeyCode.MINUS;
            case Keyboard.KEY_APOSTROPHE:
                return KeyCode.APOSTROPHE;
            case Keyboard.KEY_F1:
                return KeyCode.F1;
            case Keyboard.KEY_F2:
                return KeyCode.F2;
            case Keyboard.KEY_F3:
                return KeyCode.F3;
            case Keyboard.KEY_F4:
                return KeyCode.F4;
            case Keyboard.KEY_F5:
                return KeyCode.F5;
            case Keyboard.KEY_F6:
                return KeyCode.F6;
            case Keyboard.KEY_F7:
                return KeyCode.F7;
            case Keyboard.KEY_F8:
                return KeyCode.F8;
            case Keyboard.KEY_F9:
                return KeyCode.F9;
            case Keyboard.KEY_F10:
                return KeyCode.F10;
            case Keyboard.KEY_F11:
                return KeyCode.F11;
            case Keyboard.KEY_F12:
                return KeyCode.F12;
            case Keyboard.KEY_COLON:
                return KeyCode.COLON;
            case Keyboard.KEY_NUMPAD0:
                return KeyCode.NUMPAD_0;
            case Keyboard.KEY_NUMPAD1:
                return KeyCode.NUMPAD_1;
            case Keyboard.KEY_NUMPAD2:
                return KeyCode.NUMPAD_2;
            case Keyboard.KEY_NUMPAD3:
                return KeyCode.NUMPAD_3;
            case Keyboard.KEY_NUMPAD4:
                return KeyCode.NUMPAD_4;
            case Keyboard.KEY_NUMPAD5:
                return KeyCode.NUMPAD_5;
            case Keyboard.KEY_NUMPAD6:
                return KeyCode.NUMPAD_6;
            case Keyboard.KEY_NUMPAD7:
                return KeyCode.NUMPAD_7;
            case Keyboard.KEY_NUMPAD8:
                return KeyCode.NUMPAD_8;
            case Keyboard.KEY_NUMPAD9:
                return KeyCode.NUMPAD_9;
            default:
                return KeyCode.UNKNOWN;
        }
    }

    public static int getLwjglKeyCode(KeyCode gdxKeyCode){
        switch(gdxKeyCode){
            case APOSTROPHE:
                return Keyboard.KEY_APOSTROPHE;
            case LEFT_BRACKET:
                return Keyboard.KEY_LBRACKET;
            case RIGHT_BRACKET:
                return Keyboard.KEY_RBRACKET;
            case GRAVE:
                return Keyboard.KEY_GRAVE;
            case STAR:
                return Keyboard.KEY_MULTIPLY;
            case NUM:
                return Keyboard.KEY_NUMLOCK;
            case AT:
                return Keyboard.KEY_AT;
            case EQUALS:
                return Keyboard.KEY_EQUALS;
            case SYM:
                return Keyboard.KEY_LMETA;
            case NUM_0:
                return Keyboard.KEY_0;
            case NUM_1:
                return Keyboard.KEY_1;
            case NUM_2:
                return Keyboard.KEY_2;
            case NUM_3:
                return Keyboard.KEY_3;
            case NUM_4:
                return Keyboard.KEY_4;
            case NUM_5:
                return Keyboard.KEY_5;
            case NUM_6:
                return Keyboard.KEY_6;
            case NUM_7:
                return Keyboard.KEY_7;
            case NUM_8:
                return Keyboard.KEY_8;
            case NUM_9:
                return Keyboard.KEY_9;
            case A:
                return Keyboard.KEY_A;
            case B:
                return Keyboard.KEY_B;
            case C:
                return Keyboard.KEY_C;
            case D:
                return Keyboard.KEY_D;
            case E:
                return Keyboard.KEY_E;
            case F:
                return Keyboard.KEY_F;
            case G:
                return Keyboard.KEY_G;
            case H:
                return Keyboard.KEY_H;
            case I:
                return Keyboard.KEY_I;
            case J:
                return Keyboard.KEY_J;
            case K:
                return Keyboard.KEY_K;
            case L:
                return Keyboard.KEY_L;
            case M:
                return Keyboard.KEY_M;
            case N:
                return Keyboard.KEY_N;
            case O:
                return Keyboard.KEY_O;
            case P:
                return Keyboard.KEY_P;
            case Q:
                return Keyboard.KEY_Q;
            case R:
                return Keyboard.KEY_R;
            case S:
                return Keyboard.KEY_S;
            case T:
                return Keyboard.KEY_T;
            case U:
                return Keyboard.KEY_U;
            case V:
                return Keyboard.KEY_V;
            case W:
                return Keyboard.KEY_W;
            case X:
                return Keyboard.KEY_X;
            case Y:
                return Keyboard.KEY_Y;
            case Z:
                return Keyboard.KEY_Z;
            case ALT_LEFT:
                return Keyboard.KEY_LMENU;
            case ALT_RIGHT:
                return Keyboard.KEY_RMENU;
            case BACKSLASH:
                return Keyboard.KEY_BACKSLASH;
            case COMMA:
                return Keyboard.KEY_COMMA;
            case FORWARD_DEL:
                return Keyboard.KEY_DELETE;
            case DPAD_LEFT:
                return Keyboard.KEY_LEFT;
            case DPAD_RIGHT:
                return Keyboard.KEY_RIGHT;
            case DPAD_UP:
                return Keyboard.KEY_UP;
            case DPAD_DOWN:
                return Keyboard.KEY_DOWN;
            case ENTER:
                return Keyboard.KEY_RETURN;
            case HOME:
                return Keyboard.KEY_HOME;
            case END:
                return Keyboard.KEY_END;
            case PAGE_DOWN:
                return Keyboard.KEY_NEXT;
            case PAGE_UP:
                return Keyboard.KEY_PRIOR;
            case INSERT:
                return Keyboard.KEY_INSERT;
            case MINUS:
                return Keyboard.KEY_MINUS;
            case PERIOD:
                return Keyboard.KEY_PERIOD;
            case PLUS:
                return Keyboard.KEY_ADD;
            case SEMICOLON:
                return Keyboard.KEY_SEMICOLON;
            case SHIFT_LEFT:
                return Keyboard.KEY_LSHIFT;
            case SHIFT_RIGHT:
                return Keyboard.KEY_RSHIFT;
            case SLASH:
                return Keyboard.KEY_SLASH;
            case SPACE:
                return Keyboard.KEY_SPACE;
            case TAB:
                return Keyboard.KEY_TAB;
            case DEL:
                return Keyboard.KEY_BACK;
            case CONTROL_LEFT:
                return Keyboard.KEY_LCONTROL;
            case CONTROL_RIGHT:
                return Keyboard.KEY_RCONTROL;
            case ESCAPE:
                return Keyboard.KEY_ESCAPE;
            case F1:
                return Keyboard.KEY_F1;
            case F2:
                return Keyboard.KEY_F2;
            case F3:
                return Keyboard.KEY_F3;
            case F4:
                return Keyboard.KEY_F4;
            case F5:
                return Keyboard.KEY_F5;
            case F6:
                return Keyboard.KEY_F6;
            case F7:
                return Keyboard.KEY_F7;
            case F8:
                return Keyboard.KEY_F8;
            case F9:
                return Keyboard.KEY_F9;
            case F10:
                return Keyboard.KEY_F10;
            case F11:
                return Keyboard.KEY_F11;
            case F12:
                return Keyboard.KEY_F12;
            case COLON:
                return Keyboard.KEY_COLON;
            case NUMPAD_0:
                return Keyboard.KEY_NUMPAD0;
            case NUMPAD_1:
                return Keyboard.KEY_NUMPAD1;
            case NUMPAD_2:
                return Keyboard.KEY_NUMPAD2;
            case NUMPAD_3:
                return Keyboard.KEY_NUMPAD3;
            case NUMPAD_4:
                return Keyboard.KEY_NUMPAD4;
            case NUMPAD_5:
                return Keyboard.KEY_NUMPAD5;
            case NUMPAD_6:
                return Keyboard.KEY_NUMPAD6;
            case NUMPAD_7:
                return Keyboard.KEY_NUMPAD7;
            case NUMPAD_8:
                return Keyboard.KEY_NUMPAD8;
            case NUMPAD_9:
                return Keyboard.KEY_NUMPAD9;
            default:
                return Keyboard.KEY_NONE;
        }
    }

    public void update(){
        updateTime();
        updateMouse();
        updateKeyboard();
    }

    private KeyCode toGdxButton(int button){
        if(button == 0) return KeyCode.MOUSE_LEFT;
        if(button == 1) return KeyCode.MOUSE_RIGHT;
        if(button == 2) return KeyCode.MOUSE_MIDDLE;
        if(button == 3) return KeyCode.MOUSE_BACK;
        if(button == 4) return KeyCode.MOUSE_FORWARD;
        return KeyCode.UNKNOWN;
    }

    void updateTime(){
        long thisTime = System.nanoTime();
        deltaTime = (thisTime - lastTime) / 1000000000.0f;
        lastTime = thisTime;
    }

    void updateMouse(){
        if(justTouched){
            justTouched = false;
            for(int i = 0; i < justPressedButtons.length; i++){
                justPressedButtons[i] = false;
            }
        }
        if(Mouse.isCreated()){
            int events = 0;
            while(Mouse.next()){
                events++;
                int x = (int)(Mouse.getEventX() * Display.getPixelScaleFactor());
                int y = (int)(Mouse.getEventY() * Display.getPixelScaleFactor());
                int button = Mouse.getEventButton();
                KeyCode gdxButton = toGdxButton(button);
                if(button != -1 && gdxButton == KeyCode.UNKNOWN) continue; // Ignore unknown button.

                TouchEvent event = usedTouchEvents.obtain();
                event.x = x;
                event.y = y;
                event.button = gdxButton;
                event.pointer = 0;
                event.timeStamp = Mouse.getEventNanoseconds();

                // could be drag, scroll or move
                if(button == -1){
                    if(Mouse.getEventDWheel() != 0){
                        event.type = TouchEvent.TOUCH_SCROLLED;
                        event.scrollAmount = (int)-Math.signum(Mouse.getEventDWheel());
                    }else if(pressedButtons.size > 0){
                        event.type = TouchEvent.TOUCH_DRAGGED;
                    }else{
                        event.type = TouchEvent.TOUCH_MOVED;
                    }
                }else{
                    // nope, it's a down or up event.
                    if(Mouse.getEventButtonState()){
                        event.type = TouchEvent.TOUCH_DOWN;
                        pressedButtons.add(event.button.ordinal());
                        justPressedButtons[event.button.ordinal()] = true;
                        justTouched = true;
                    }else{
                        event.type = TouchEvent.TOUCH_UP;
                        pressedButtons.remove(event.button.ordinal());
                    }
                }

                touchEvents.add(event);
                mouseX = event.x;
                mouseY = event.y;
                deltaX = (int)(Mouse.getEventDX() * Display.getPixelScaleFactor());
                deltaY = (int)(Mouse.getEventDY() * Display.getPixelScaleFactor());
            }

            if(events == 0){
                deltaX = 0;
                deltaY = 0;
            }else{
                Core.graphics.requestRendering();
            }
        }
    }

    void updateKeyboard(){
        if(keyJustPressed){
            keyJustPressed = false;
            for(int i = 0; i < justPressedKeys.length; i++){
                justPressedKeys[i] = false;
            }
        }
        if(lastKeyCharPressed != 0){
            keyRepeatTimer -= deltaTime;
            if(keyRepeatTimer < 0){
                keyRepeatTimer = keyRepeatTime;

                KeyEvent event = usedKeyEvents.obtain();
                event.keyCode = KeyCode.UNKNOWN;
                event.keyChar = lastKeyCharPressed;
                event.type = KeyEvent.KEY_TYPED;
                event.timeStamp = System.nanoTime(); // FIXME this should use the repeat time plus the timestamp of the original
                keyEvents.add(event);
                Core.graphics.requestRendering();
            }
        }

        if(Keyboard.isCreated()){
            while(Keyboard.next()){
                KeyCode keyCode = getGdxKeyCode(Keyboard.getEventKey());
                char keyChar = Keyboard.getEventCharacter();
                if(Keyboard.getEventKeyState() || (keyCode == KeyCode.UNKNOWN && keyChar != 0 && Character.isDefined(keyChar))){
                    long timeStamp = Keyboard.getEventNanoseconds();

                    switch(keyCode){
                        case DEL:
                            keyChar = 8;
                            break;
                        case FORWARD_DEL:
                            keyChar = 127;
                            break;
                    }

                    if(keyCode != KeyCode.UNKNOWN){
                        KeyEvent event = usedKeyEvents.obtain();
                        event.keyCode = keyCode;
                        event.keyChar = 0;
                        event.type = KeyEvent.KEY_DOWN;
                        event.timeStamp = timeStamp;
                        keyEvents.add(event);

                        pressedKeys++;
                        keyJustPressed = true;
                        justPressedKeys[keyCode.ordinal()] = true;
                        lastKeyCharPressed = keyChar;
                        keyRepeatTimer = keyRepeatInitialTime;
                    }

                    KeyEvent event = usedKeyEvents.obtain();
                    event.keyCode = KeyCode.UNKNOWN;
                    event.keyChar = keyChar;
                    event.type = KeyEvent.KEY_TYPED;
                    event.timeStamp = timeStamp;
                    keyEvents.add(event);
                }else{
                    KeyEvent event = usedKeyEvents.obtain();
                    event.keyCode = keyCode;
                    event.keyChar = 0;
                    event.type = KeyEvent.KEY_UP;
                    event.timeStamp = Keyboard.getEventNanoseconds();
                    keyEvents.add(event);

                    pressedKeys--;
                    lastKeyCharPressed = 0;
                }
                Core.graphics.requestRendering();
            }
        }
    }

    @Override
    public void vibrate(int milliseconds){
    }

    @Override
    public boolean justTouched(){
        return justTouched;
    }

    @Override
    public void vibrate(long[] pattern, int repeat){
    }

    @Override
    public void cancelVibrate(){
    }

    @Override
    public boolean isPeripheralAvailable(Peripheral peripheral){
		return peripheral == Peripheral.HardwareKeyboard;
	}

    @Override
    public int getRotation(){
        return 0;
    }

    @Override
    public Orientation getNativeOrientation(){
        return Orientation.Landscape;
    }

    @Override
    public void setCursorCatched(boolean catched){
        Mouse.setGrabbed(catched);
    }

    @Override
    public boolean isCursorCatched(){
        return Mouse.isGrabbed();
    }

    @Override
    public int deltaX(){
        return deltaX;
    }

    @Override
    public int deltaX(int pointer){
        if(pointer == 0)
            return deltaX;
        else
            return 0;
    }

    @Override
    public int deltaY(){
        return -deltaY;
    }

    @Override
    public int deltaY(int pointer){
        if(pointer == 0)
            return -deltaY;
        else
            return 0;
    }

    @Override
    public void setCursorPosition(int x, int y){
        Mouse.setCursorPosition(x, y);
    }

    @Override
    public long getCurrentEventTime(){
        return currentEventTimeStamp;
    }

    class KeyEvent{
        static final int KEY_DOWN = 0;
        static final int KEY_UP = 1;
        static final int KEY_TYPED = 2;

        long timeStamp;
        int type;
        KeyCode keyCode;
        char keyChar;
    }

    class TouchEvent{
        static final int TOUCH_DOWN = 0;
        static final int TOUCH_UP = 1;
        static final int TOUCH_DRAGGED = 2;
        static final int TOUCH_SCROLLED = 3;
        static final int TOUCH_MOVED = 4;

        long timeStamp;
        int type;
        int x;
        int y;
        int scrollAmount;
        KeyCode button;
        int pointer;
    }

}
