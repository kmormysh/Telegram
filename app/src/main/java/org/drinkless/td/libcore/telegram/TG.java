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

/**
 * This class is used for managing singleton-instance of class Client.
 */
public final class TG {
    private static volatile Client.ResultHandler updatesHandler;
    private static volatile Client instance;
    private static volatile String dir;

    /**
     * Sets handler which will be invoked for every incoming update from TDLib of type TdApi.Update.
     * Must be called before getClientInstance().
     *
     * @param updatesHandler Handler to be invoked on updates.
     */
    public static void setUpdatesHandler(Client.ResultHandler updatesHandler) {
        synchronized (TG.class) {
            TG.updatesHandler = updatesHandler;
            if (instance != null) {
                instance.setUpdatesHandler(TG.updatesHandler);
            }
        }
    }

    /**
     * Sets directory for storing persistent data of TDLib.
     * Must be called before getClientInstance().
     *
     * @param dir Directory to store persistent data.
     */
    public static void setDir(String dir) {
        synchronized (TG.class) {
            TG.dir = dir;
        }
    }

    /**
     * This function stops and destroys Client.
     * No queries are possible after this call, but completely new instance
     * of a Client with different settings can be obtained through getClientInstance()
     */
    public static void stopClient() {
        if (instance != null) {
            synchronized (TG.class) {
                if (instance != null) {
                    Client local = instance;
                    instance = null;
                    updatesHandler = null;
                    local.stop();
                }
            }
        }
    }

    /**
     * This function returns singleton object of class Client which can be used for querying TDLib.
     * setUpdatesHandler() and setDir() must be called before this function.
     */
    public static Client getClientInstance() {
        if (instance == null) {
            synchronized (TG.class) {
                if (instance == null) {
                    if (dir == null) {
                        return null;
                    }
                    Client local = Client.create(updatesHandler, dir);
                    new Thread(local).start();
                    instance = local;
                }
            }
        }
        return instance;
    }
}
