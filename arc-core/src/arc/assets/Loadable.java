package arc.assets;

import arc.struct.*;

public interface Loadable{
    default void loadAsync(){

    }

    default void loadSync(){

    }

    default String getName(){
        return getClass().getSimpleName();
    }

    default Array<AssetDescriptor> getDependencies(){
        return null;
    }
}
