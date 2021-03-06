/***
 * Copyright (c) 2017 Oscar Salguero www.oscarsalguero.com
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oscarsalguero.smartystreetsautocomplete.async;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Background executor service
 */
public enum BackgroundExecutorService {
    INSTANCE, BackgroundExecutorService;

    private static final String LOG_TAG = BackgroundExecutorService.class.getName();

    /*
     * Max single thread ExecutorService that will spin down thread after use
     */
    private final Executor executor;

    {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1,
                5L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(@NonNull final Runnable r) {
                        return new Thread(r, LOG_TAG + "Thread");
                    }
                });
        executor.allowCoreThreadTimeOut(true);
        this.executor = executor;
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public <R> void enqueue(final BackgroundJob<R> job) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final R result = job.executeInBackground();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            job.onSuccess(result);
                        }
                    });
                } catch (final Exception e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            job.onFailure(e);
                        }
                    });
                }
            }
        });
    }
}
