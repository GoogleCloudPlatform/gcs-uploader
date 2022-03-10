/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.ce.media.contentuploader.exec;

import com.google.ce.media.contentuploader.config.EnvConfig;
import com.google.ce.media.contentuploader.message.TaskInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created in gcs-uploader on 2020-01-10.
 */
@Component
public class UploadTaskPool {
  private final EnvConfig envConfig;

  private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

  private final ExecutorService executorService;

  private final ExecutorService stitcherService = Executors.newFixedThreadPool(5);

  private final BlockingQueue<BlobUploadTask> blockingQueue;

  private final List<Thread> uploaderThreads;

  private final Object QUEUE_LOCK = new Object();

  private final Map<TaskInfo, Runnable> runnableMap = new HashMap<>();


  public UploadTaskPool(EnvConfig envConfig) {
    this.envConfig = envConfig;

    executorService = Executors.newFixedThreadPool(envConfig.getTaskWorkerPoolSize());
    blockingQueue = new ArrayBlockingQueue<>(5);
//    blockingQueue = new ArrayBlockingQueue<>(envConfig.getUploadQueueSize());
    uploaderThreads = new ArrayList<>(envConfig.getUploadWorkerPoolSize());

    for (int i = 0; i < envConfig.getUploadWorkerPoolSize(); i++) {
      Thread thread = new Thread(new UploaderRunnable(blockingQueue));
      uploaderThreads.add(thread);
      thread.start();
    }
  }

  public void enqueue(UploadTask uploadTask) {
    synchronized (QUEUE_LOCK) {
      executorService.submit(uploadTask);
    }
  }

  public void enqueue(CompositeUploadTask uploadTask) {
    synchronized (QUEUE_LOCK) {
      executorService.submit(uploadTask);
    }
  }

  public void enqueue(StitchTask stitchTask) {
    stitcherService.submit(stitchTask);
  }

  public void enqueue(CleanupTask cleanupTask) {
    stitcherService.submit(cleanupTask);
  }

  public void enqueue(BlobUploadTask blobUploadTask) throws InterruptedException {
    blockingQueue.put(blobUploadTask);
  }

  public void scheduleAtFixedRate(Runnable task, long periodMillis) {
    scheduledExecutorService.scheduleAtFixedRate(task, 5L, periodMillis, TimeUnit.MILLISECONDS);
  }

  private final class UploaderRunnable implements Runnable {
    private final BlockingQueue<BlobUploadTask> queue;
    private final Object LOCK = new Object();

    private UploaderRunnable(BlockingQueue<BlobUploadTask> queue) {
      this.queue = queue;
    }

    @Override
    public void run() {
      while (true) {
        try {
          int runningCount = 0;
          boolean ok = false;
          BlobUploadTask current = queue.take();
          while (runningCount < 3 && !ok) {
            try {
              runningCount++;
              ok = runCurrent(current);
            } catch (Exception e) {
              current.getTasklet().incrementErrorCount();
              e.printStackTrace();
              synchronized (LOCK) {
                System.out.println(">>> ["+current.getTaskInfo().getName()+"] WAITING due to exception: " + e.getMessage());
                LOCK.wait(runningCount * 1000L);
                System.out.println(">>> ["+current.getTaskInfo().getName()+"] RESUMING from exception: " + e.getMessage());
              }
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    private boolean runCurrent(BlobUploadTask current) throws Exception {
      current.run();
      return true;
    }
  }



}
