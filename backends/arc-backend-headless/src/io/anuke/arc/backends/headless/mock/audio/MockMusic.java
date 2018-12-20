/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package io.anuke.arc.backends.headless.mock.audio;

import io.anuke.arc.audio.Music;

/**
 * The headless backend does its best to mock elements. This is intended to make code-sharing between
 * server and client as simple as possible.
 */
public class MockMusic implements Music{
    @Override
    public void play(){

    }

    @Override
    public void pause(){

    }

    @Override
    public void stop(){

    }

    @Override
    public boolean isPlaying(){
        return false;
    }

    @Override
    public boolean isLooping(){
        return false;
    }

    @Override
    public void setLooping(boolean isLooping){

    }

    @Override
    public float getVolume(){
        return 0;
    }

    @Override
    public void setVolume(float volume){

    }

    @Override
    public void setPan(float pan, float volume){

    }

    @Override
    public float getPosition(){
        return 0;
    }

    @Override
    public void setPosition(float position){

    }

    @Override
    public void dispose(){

    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener){

    }
}