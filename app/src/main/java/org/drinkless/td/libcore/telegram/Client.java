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

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Main class for interaction with TDLib.
 */
public class Client implements Runnable {
    /**
     * Interface for handler for results of queries to TDLib and
     * incoming updates from TDLib.
     */
    public interface ResultHandler {
        /**
         * Callback called on result of query to TDLib or incoming update from TDLib.
         *
         * @param object Result of query or update of type TdApi.Update about new events.
         */
        void onResult(TdApi.TLObject object);
    }

    /**
     * Sends request to TDLib.
     *
     * @param function Object representing request.
     * @param handler  Result handler with onResult method which will be called with result
     *                 of query or with TdApi.error as parameter.
     * @throws NullPointerException if function or handler is null.
     */
    public void send(TdApi.TLFunction function, ResultHandler handler) {
        if (handler == null || function == null) {
            throw new NullPointerException();
        }

        try {
            queryQueue.put(new Query(function, handler));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        wakeUp();
    }

    /**
     * Replaces handler for incoming updates from TDLib.
     *
     * @param handler Handler with onResult method which will be called for every incoming
     *                update from TDLib.
     */
    public void setUpdatesHandler(ResultHandler handler) {
        try {
            queryQueue.put(new Query(null, handler));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        wakeUp();
    }

    /**
     * Function for benchmarking number of queries per second which can handle TDLib, ignore it.
     *
     * @param function Object representing request.
     * @param handler  Result handler with onResult method which will be called with result
     *                 of query or with TdApi.error ad parameter.
     * @param n        Number of times to repeat request.
     * @throws NullPointerException if dir is null.
     */
    public void bench(TdApi.TLFunction function, ResultHandler handler, int n) {
        if (handler == null || function == null) {
            throw new NullPointerException();
        }

        Query query = new Query(function, handler);
        for (int i = 0; i < n; i++) {
            try {
                queryQueue.put(query);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        wakeUp();
    }

    /**
     * Overridden method from Runnable, do not call it directly.
     */
    @Override
    public void run() {
        while (true) {
            if (stopFlag) {
                doStop();
                break;
            }
            processQueue();
            flushQueries(10.0 /*seconds*/);
        }
    }

    /**
     * Creates new Client. Do not call it directly. Use TG.getClientInstance() instead.
     *
     * @param updatesHandler Handler for incoming updates.
     * @param dir            Directory for persistent database.
     * @throws NullPointerException if dir is null.
     */
    static Client create(ResultHandler updatesHandler, String dir) {
        if (dir == null) {
            throw new NullPointerException();
        }

        return new Client(updatesHandler, dir);
    }

    /**
     * Stops Client. Do not call it directly. Use TG.stopClient() instead.
     */
    void stop() {
        stopFlag = true;
        wakeUp();
    }


    private volatile boolean stopFlag;
    private final long nativeClientId;
    private final BlockingQueue<Query> queryQueue;
    private long currentId;
    private final HashMap<Long, ResultHandler> handlers;
    private static final int IDS_SIZE = 1000;
    private int idsI;
    private final long[] ids;
    private final TdApi.TLObject[] events;

    private static class Query {
        final TdApi.TLFunction function;
        final ResultHandler handler;

        Query(TdApi.TLFunction function, ResultHandler handler) {
            this.function = function;
            this.handler = handler;
        }
    }

    private Client(ResultHandler updatesHandler, String dir) {
        long nativeClientId = NativeClient.createClient();
        NativeClient.clientInit(nativeClientId, dir);

        stopFlag = false;
        this.nativeClientId = nativeClientId;
        queryQueue = new LinkedBlockingQueue<>();
        currentId = 0L;

        //TODO: SparseArray?
        handlers = new HashMap<>();
        if (updatesHandler != null) {
            handlers.put(0L, updatesHandler);
        }

        idsI = 0;
        ids = new long[IDS_SIZE];
        events = new TdApi.TLObject[IDS_SIZE];
    }

    private void doStop() {
        NativeClient.clientClear(nativeClientId);
        NativeClient.destroyClient(nativeClientId);
    }

    private void wakeUp() {
        NativeClient.clientWakeUp(nativeClientId);
    }


    private void processResult(long id, TdApi.TLObject object) {
        ResultHandler handler = handlers.get(id);
        if (handler == null) {
            Log.w("DLTD", "Can't find handler for result " + id + " -- ignore result");
            return;
        }

        try {
            handler.onResult(object);
        } catch (Throwable cause) {
            Log.w("DLTD", "handler throws", cause);
        }

        //update handler stays forever
        if (id != 0) {
            handlers.remove(id);
        }
    }

    private void flushQueries(double timeout) {
        int resultN = NativeClient.clientRun(nativeClientId, ids, events, idsI, timeout);
        idsI = 0;
        for (int i = 0; i < resultN; i++) {
            processResult(ids[i], events[i]);
            events[i] = null;
        }
    }

    private void processQuery(Query query) {
        if (query.function == null) {
            //fix updates handler
            if (query.handler == null) {
                handlers.remove(0L);
            } else {
                handlers.put(0L, query.handler);
            }
            return;
        }
        if (idsI == IDS_SIZE) {
            flushQueries(0.0);
        }

        //Condition in while is quite impossible
        do {
            currentId++;
        } while (currentId == 0 || handlers.containsKey(currentId));

        ids[idsI] = currentId;
        events[idsI] = query.function;
        handlers.put(currentId, query.handler);
        idsI++;
    }

    private void processQueue() {
        while (true) {
            Query query = queryQueue.poll();
            if (query == null) {
                break;
            }
            processQuery(query);
        }
    }
}
