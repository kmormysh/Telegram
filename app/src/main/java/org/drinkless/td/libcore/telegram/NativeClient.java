/*
 * This file is part of TD.
 *
 * TD is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * TD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with TD.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2014-2015 Arseny Smirnov
 *           2014-2015 Aliaksei Levin
 */

package org.drinkless.td.libcore.telegram;

import android.util.Log;

/**
 * This class is used internally by Client to send requests to TDLib.
 */
final class NativeClient {
    static {
        // TODO: move System.loadLibrary("tdjni") in proper place.
        try {
            System.loadLibrary("tdjni");
        } catch (UnsatisfiedLinkError e) {
            Log.w("DLTD", "Can't find tdjni", e);
        }
    }

    public static native long createClient();

    public static native void destroyClient(long clientId);

    public static native void clientInit(long clientId, String dir);

    public static native int clientRun(long clientId, long[] eventIds, TdApi.TLObject[] events, int changesCount, double timeout);

    public static native void clientWakeUp(long clientId);

    public static native void clientClear(long clientId);

    //Just for testing
    public static native TdApi.TLObject pingPong(TdApi.TLObject object);

    public static native void ping(TdApi.TLObject object);
}
