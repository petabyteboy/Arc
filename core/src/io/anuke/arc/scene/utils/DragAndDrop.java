package io.anuke.arc.scene.utils;

import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.collection.ObjectMap.Entry;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.Scene;
import io.anuke.arc.scene.event.DragListener;
import io.anuke.arc.scene.event.InputEvent;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.ScrollPane;

import static io.anuke.arc.Core.scene;

/**
 * Manages drag and drop operations through registered drag sources and drop targets.
 * @author Nathan Sweet
 */
public class DragAndDrop{
    static final Vector2 tmpVector = new Vector2();

    Payload payload;
    Element dragActor;
    Target target;
    boolean isValidTarget;
    Array<Target> targets = new Array<>();
    ObjectMap<Source, DragListener> sourceListeners = new ObjectMap<>();
    float dragActorX = 0, dragActorY = 0;
    float touchOffsetX, touchOffsetY;
    long dragStartTime;
    int dragTime = 250;
    int activePointer = -1;
    boolean cancelTouchFocus = true;
    boolean keepWithinStage = true;
    private float tapSquareSize = 8;
    private int button;

    public void addSource(final Source source){
        io.anuke.arc.scene.event.DragListener listener = new io.anuke.arc.scene.event.DragListener(){
            public void dragStart(InputEvent event, float x, float y, int pointer){
                if(activePointer != -1){
                    event.stop();
                    return;
                }

                activePointer = pointer;

                dragStartTime = System.currentTimeMillis();
                payload = source.dragStart(event, getTouchDownX(), getTouchDownY(), pointer);
                event.stop();

                if(cancelTouchFocus && payload != null)
                    source.getActor().getScene().cancelTouchFocusExcept(this, source.getActor());
            }

            public void drag(InputEvent event, float x, float y, int pointer){
                if(payload == null) return;
                if(pointer != activePointer) return;

                Touchable dragActorTouchable = null;
                if(dragActor != null){
                    dragActorTouchable = dragActor.getTouchable();
                    dragActor.touchable(Touchable.disabled);
                }

                // Find target.
                Target newTarget = null;
                isValidTarget = false;
                float stageX = event.stageX + touchOffsetX, stageY = event.stageY + touchOffsetY;
                Element hit = scene.hit(stageX, stageY, true); // Prefer touchable actors.
                if(hit == null) hit = scene.hit(stageX, stageY, false);
                if(hit != null){
                    for(int i = 0, n = targets.size; i < n; i++){
                        Target target = targets.get(i);
                        if(!target.actor.isAscendantOf(hit)) continue;
                        newTarget = target;
                        target.actor.stageToLocalCoordinates(tmpVector.set(stageX, stageY));
                        break;
                    }
                }
                // If over a new target, notify the former target that it's being left behind.
                if(newTarget != target){
                    if(target != null) target.reset(source, payload);
                    target = newTarget;
                }
                // Notify new target of drag.
                if(newTarget != null)
                    isValidTarget = newTarget.drag(source, payload, tmpVector.x, tmpVector.y, pointer);

                if(dragActor != null) dragActor.touchable(dragActorTouchable);

                // Add/remove and position the drag actor.
                Element actor = null;
                if(target != null) actor = isValidTarget ? payload.validDragActor : payload.invalidDragActor;
                if(actor == null) actor = payload.dragActor;
                if(actor == null) return;
                if(dragActor != actor){
                    if(dragActor != null) dragActor.remove();
                    dragActor = actor;
                    scene.add(actor);
                }
                float actorX = event.stageX - actor.getWidth() + dragActorX;
                float actorY = event.stageY + dragActorY;
                if(keepWithinStage){
                    if(actorX < 0) actorX = 0;
                    if(actorY < 0) actorY = 0;
                    if(actorX + actor.getWidth() > scene.getWidth()) actorX = scene.getWidth() - actor.getWidth();
                    if(actorY + actor.getHeight() > scene.getHeight()) actorY = scene.getHeight() - actor.getHeight();
                }
                actor.setPosition(actorX, actorY);
            }

            public void dragStop(InputEvent event, float x, float y, int pointer){
                if(pointer != activePointer) return;
                activePointer = -1;
                if(payload == null) return;

                if(System.currentTimeMillis() - dragStartTime < dragTime) isValidTarget = false;
                if(dragActor != null) dragActor.remove();
                if(isValidTarget){
                    float stageX = event.stageX + touchOffsetX, stageY = event.stageY + touchOffsetY;
                    target.actor.stageToLocalCoordinates(tmpVector.set(stageX, stageY));
                    target.drop(source, payload, tmpVector.x, tmpVector.y, pointer);
                }
                source.dragStop(event, x, y, pointer, payload, isValidTarget ? target : null);
                if(target != null) target.reset(source, payload);
                payload = null;
                target = null;
                isValidTarget = false;
                dragActor = null;
            }
        };
        listener.setTapSquareSize(tapSquareSize);
        listener.setButton(button);
        source.actor.addCaptureListener(listener);
        sourceListeners.put(source, listener);
    }

    public void removeSource(Source source){
        io.anuke.arc.scene.event.DragListener dragListener = sourceListeners.remove(source);
        source.actor.removeCaptureListener(dragListener);
    }

    public void addTarget(Target target){
        targets.add(target);
    }

    public void removeTarget(Target target){
        targets.removeValue(target, true);
    }

    /** Removes all targets and sources. */
    public void clear(){
        targets.clear();
        for(Entry<Source, DragListener> entry : sourceListeners.entries())
            entry.key.actor.removeCaptureListener(entry.value);
        sourceListeners.clear();
    }

    /** Sets the distance a touch must travel before being considered a drag. */
    public void setTapSquareSize(float halfTapSquareSize){
        tapSquareSize = halfTapSquareSize;
    }

    /** Sets the button to listen for, all other buttons are ignored. */
    public void setButton(int button){
        this.button = button;
    }

    public void setDragActorPosition(float dragActorX, float dragActorY){
        this.dragActorX = dragActorX;
        this.dragActorY = dragActorY;
    }

    /**
     * Sets an offset in stage coordinates from the touch position which is used to determine the drop location. Default is
     * 0,0.
     */
    public void setTouchOffset(float touchOffsetX, float touchOffsetY){
        this.touchOffsetX = touchOffsetX;
        this.touchOffsetY = touchOffsetY;
    }

    public boolean isDragging(){
        return payload != null;
    }

    /** Returns the current drag actor, or null. */
    public Element getDragActor(){
        return dragActor;
    }

    /**
     * Time in milliseconds that a drag must take before a drop will be considered valid. This ignores an accidental drag and drop
     * that was meant to be a click. Default is 250.
     */
    public void setDragTime(int dragMillis){
        this.dragTime = dragMillis;
    }

    /**
     * When true (default), the {@link Scene#cancelTouchFocus()} touch focus} is cancelled if
     * {@link Source#dragStart(InputEvent, float, float, int) dragStart} returns non-null. This ensures the DragAndDrop is the only
     * touch focus listener, eg when the source is inside a {@link ScrollPane} with flick scroll enabled.
     */
    public void setCancelTouchFocus(boolean cancelTouchFocus){
        this.cancelTouchFocus = cancelTouchFocus;
    }

    public void setKeepWithinStage(boolean keepWithinStage){
        this.keepWithinStage = keepWithinStage;
    }

    /**
     * A source where a payload can be dragged from.
     * @author Nathan Sweet
     */
    static abstract public class Source{
        final Element actor;

        public Source(Element actor){
            if(actor == null) throw new IllegalArgumentException("actor cannot be null.");
            this.actor = actor;
        }

        /**
         * Called when a drag is started on the source. The coordinates are in the source's local coordinate system.
         * @return If null the drag will not affect any targets.
         */
        abstract public Payload dragStart(InputEvent event, float x, float y, int pointer);

        /**
         * Called when a drag for the source is stopped. The coordinates are in the source's local coordinate system.
         * @param payload null if dragStart returned null.
         * @param target null if not dropped on a valid target.
         */
        public void dragStop(InputEvent event, float x, float y, int pointer, Payload payload, Target target){
        }

        public Element getActor(){
            return actor;
        }
    }

    /**
     * A target where a payload can be dropped to.
     * @author Nathan Sweet
     */
    static abstract public class Target{
        final Element actor;

        public Target(Element actor){
            if(actor == null) throw new IllegalArgumentException("actor cannot be null.");
            this.actor = actor;
            Scene stage = actor.getScene();
            if(stage != null && actor == stage.root)
                throw new IllegalArgumentException("The stage root cannot be a drag and drop target.");
        }

        /**
         * Called when the payload is dragged over the target. The coordinates are in the target's local coordinate system.
         * @return true if this is a valid target for the payload.
         */
        abstract public boolean drag(Source source, Payload payload, float x, float y, int pointer);

        /** Called when the payload is no longer over the target, whether because the touch was moved or a drop occurred. */
        public void reset(Source source, Payload payload){
        }

        /** Called when the payload is dropped on the target. The coordinates are in the target's local coordinate system. */
        abstract public void drop(Source source, Payload payload, float x, float y, int pointer);

        public Element getActor(){
            return actor;
        }
    }

    /**
     * The payload of a drag and drop operation. Actors can be optionally provided to follow the cursor and change when over a
     * target. Such Actors will be added and removed from the stage automatically during the drag operation. Care should be taken
     * when using the source Actor as a payload drag actor.
     */
    static public class Payload{
        Element dragActor, validDragActor, invalidDragActor;
        Object object;

        public Element getDragActor(){
            return dragActor;
        }

        public void setDragActor(Element dragActor){
            this.dragActor = dragActor;
        }

        public Element getValidDragActor(){
            return validDragActor;
        }

        public void setValidDragActor(Element validDragActor){
            this.validDragActor = validDragActor;
        }

        public Element getInvalidDragActor(){
            return invalidDragActor;
        }

        public void setInvalidDragActor(Element invalidDragActor){
            this.invalidDragActor = invalidDragActor;
        }

        public Object getObject(){
            return object;
        }

        public void setObject(Object object){
            this.object = object;
        }
    }
}