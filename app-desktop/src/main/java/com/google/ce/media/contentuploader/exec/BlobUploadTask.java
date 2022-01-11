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

import com.google.ce.media.contentuploader.message.AnalyticsMessage;
import com.google.ce.media.contentuploader.message.TaskInfo;
import com.google.ce.media.contentuploader.message.TaskStatus;
import com.google.ce.media.contentuploader.message.Tasklet;
import com.google.ce.media.contentuploader.utils.DateUtils;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created in gcs-uploader on 2020-01-25.
 */
public class BlobUploadTask implements Runnable {
  private Storage storage;
  private final TaskInfo taskInfo;
  private final Tasklet tasklet;
  private final ByteBuffer buffer;
  private final Object TASK_LOCK = new Object();

  public BlobUploadTask(TaskInfo taskInfo,
                        Tasklet tasklet,
                        ByteBuffer buffer) {
    this.taskInfo = taskInfo;
    this.tasklet = tasklet;
    this.buffer = buffer;
  }

  @Override
  public void run() {
    if(taskInfo.isCancelling()) {
      return;
    }
    Long startTime = System.currentTimeMillis();
    AnalyticsMessage m1 = AnalyticsMessage.from(taskInfo.getAuthConfig().getAuthInfo(),
                                                tasklet,
                                                AnalyticsMessage.Event.SEGMENT_START,
                                                "Started Segment Upload");
    m1.setStartTime(DateUtils.time(startTime));
    AnalyticsService.getInstance().enqueue(taskInfo.getAuthConfig().getAuthInfo(), m1);


    storage = taskInfo.getAuthConfig().getStorage();
    WriteChannel writeChannel = null;
    System.out.println(">> >> writing blobInfo.getName() = " + tasklet.getBlobInfo().getName());
    boolean hasException = false;
    Exception exception = null;
    try {
      // defensively rewind the buffer just in case this is a re-run due to error
      buffer.rewind();
      Blob blob = storage.create(tasklet.getBlobInfo());
      tasklet.setStatus(TaskStatus.RUNNING);

      byte[] transferBytes = new byte[4*1024*1024]; //4mb
      ByteBuffer transferBuffer = null;
      int count = 0;
      boolean isLast = false;

      writeChannel = blob.writer();
      while (buffer.remaining() > 0 && !taskInfo.isCancelling()) {
        if(transferBytes.length < buffer.remaining()) {
          buffer.get(transferBytes);
          transferBuffer = ByteBuffer.wrap(transferBytes);
          count = transferBytes.length;
        }
        else {
          count = buffer.remaining();
          buffer.get(transferBytes, 0, count);
          transferBuffer = ByteBuffer.wrap(transferBytes, 0, count);
          isLast = true;
        }
        writeChannel.write(transferBuffer);
        if (isLast) {
          // imporant to set this BEFORE updating the last count
          tasklet.setStatus(TaskStatus.FINISHED);
        }
        taskInfo.updateUploaded(count);
      }


    } catch (IOException e) {
      e.printStackTrace();
      exception = e;
      hasException = true;
    } finally {
      if(taskInfo.isCancelling()) {
        System.out.println("==>> CANCELLING tasklet for blob: " + tasklet.getBlobInfo().getName());
        storage.delete(tasklet.getBlobInfo().getBlobId());
        tasklet.setStatus(TaskStatus.FINISHED);
        return;
      }

      Long endTime = System.currentTimeMillis();
      if (writeChannel != null) {
        try {
          writeChannel.close();
        } catch (IOException e) {
          e.printStackTrace();
          if(exception == null) {
            exception = e;
          }
          hasException = true;
        }
      }
      if(hasException) {
        tasklet.setStatus(TaskStatus.FAILED);

        AnalyticsMessage m2 = AnalyticsMessage.from(taskInfo.getAuthConfig().getAuthInfo(),
                                                    tasklet,
                                                    AnalyticsMessage.Event.ERROR,
                                                    "Error in Segment Upload: " + exception.getMessage());
        m2.setStartTime(DateUtils.time(startTime));
        m2.setEndTime(DateUtils.time(endTime));
        m2.setTimeTaken(endTime - startTime);
        AnalyticsService.getInstance().enqueue(taskInfo.getAuthConfig().getAuthInfo(), m2);
      }
      else {
        double den = (endTime - startTime)*1.0 / 1000d;
        double mibs = den == 0 ? 0 : ((tasklet.getSize() * 8d) / den) / (1024d * 1024d);

//        tasklet.setStatus(TaskStatus.FINISHED);
//        taskInfo.updateUploaded(count);
        System.out.println(">> >> COMPLETE blobInfo.getName() = " + tasklet.getBlobInfo().getName());

        AnalyticsMessage m2 = AnalyticsMessage.from(taskInfo.getAuthConfig().getAuthInfo(),
                                                    tasklet,
                                                    AnalyticsMessage.Event.SEGMENT_END,
                                                    "End Segment Upload");
        m2.setStartTime(DateUtils.time(startTime));
        m2.setEndTime(DateUtils.time(endTime));
        m2.setTimeTaken(endTime - startTime);
        m2.setMbps((int) mibs);
        AnalyticsService.getInstance().enqueue(taskInfo.getAuthConfig().getAuthInfo(), m2);
      }
    }
  }
}
