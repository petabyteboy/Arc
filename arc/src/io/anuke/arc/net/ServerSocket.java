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

package io.anuke.arc.net;

import io.anuke.arc.Net.Protocol;
import io.anuke.arc.utils.Disposable;
import io.anuke.arc.utils.ArcRuntimeException;

/**
 * A server socket that accepts new incoming connections, returning {@link Socket} instances. The {@link #accept(SocketHints)}
 * method should preferably be called in a separate thread as it is blocking.
 * @author mzechner
 * @author noblemaster
 */
public interface ServerSocket extends Disposable{

    /** @return the Protocol used by this socket */
    Protocol getProtocol();

    /**
     * Accepts a new incoming connection from a client {@link Socket}. The given hints will be applied to the accepted socket.
     * Blocking, call on a separate thread.
     * @param hints additional {@link SocketHints} applied to the accepted {@link Socket}. Input null to use the default setting
     * provided by the system.
     * @return the accepted {@link Socket}
     * @throws ArcRuntimeException in case an error occurred
     */
    Socket accept(SocketHints hints);
}