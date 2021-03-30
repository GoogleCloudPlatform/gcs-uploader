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

import com.google.ce.media.contentuploader.message.*;
import com.google.ce.media.contentuploader.utils.DateUtils;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.context.ApplicationEventPublisher;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;

/**
 * Created in gcs-uploader on 2020-01-08.
 */
public class UploadTask implements Runnable {

  private final TaskInfo taskInfo;
  private final ApplicationEventPublisher applicationEventPublisher;

  public UploadTask(TaskInfo taskInfo,
                    ApplicationEventPublisher applicationEventPublisher) {
    this.taskInfo = taskInfo;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void run() {
    if(taskInfo.isCancelling()) {
      System.out.println(">> >> CANCELLING upload for file = " + taskInfo.getFile());
    }
    if (taskInfo.getStatus() != TaskStatus.WAITING && taskInfo.getStatus() != TaskStatus.QUEUED) {
      return;
    }
    taskInfo.setStatus(TaskStatus.RUNNING);
    taskInfo.setStartTime(System.currentTimeMillis());
    AnalyticsMessage m1 = AnalyticsMessage.from(taskInfo, AnalyticsMessage.Event.SINGLE_FILE_START, "Started Single Upload");
    m1.setStartTime(DateUtils.time(taskInfo.getStartTime()));
    AnalyticsService.getInstance().enqueue(taskInfo.getAuthConfig().getAuthInfo(), m1);

    try {
      Storage storage = taskInfo.getAuthConfig().getStorage();
      BlobInfo blobInfo = taskInfo.getBlobInfo();
      Blob blob = storage.create(blobInfo);
      long size = Files.size(taskInfo.getFile().toPath());
      WriteChannel writeChannel = blob.writer();
      FileInputStream fileInputStream = new FileInputStream(taskInfo.getFile());
      byte[] buffer = new byte[1024 * 1024 * 8]; // 32 mb
      int count = 0;
      int total = 0;
      long startTime = System.currentTimeMillis();
      taskInfo.setStartTime(startTime);
      while ((count = fileInputStream.read(buffer)) > 0 && !taskInfo.isCancelling()) {
        long segStart = System.currentTimeMillis();
        writeChannel.write(ByteBuffer.wrap(buffer, 0, count));
        long segEnd = System.currentTimeMillis();
        double den = (segEnd - segStart)*1.0 / 1000d;

        double mibs = den == 0 ? 0 : ((count * 8d) / den) / (1024d * 1024d);
        taskInfo.setMbps(mibs);
        taskInfo.updateUploaded(count);
        taskInfo.setEndTime(segEnd);
        total += count;
      }
      writeChannel.close();

      long endTime = System.currentTimeMillis();
      taskInfo.setEndTime(endTime);

      long time = (endTime - startTime) / 1000;
      time = time == 0 ? 1 : time;
      double mbps = ((total * 8) / time) / (1024 * 1024);
      taskInfo.setMbps(mbps);

      if (!taskInfo.isCancelling()) {
        taskInfo.setStatus(TaskStatus.FINISHED);

        AnalyticsMessage m2 = AnalyticsMessage.from(taskInfo, AnalyticsMessage.Event.SINGLE_FILE_END, "End Single Upload");
        m2.setStartTime(m1.getStartTime());
        m2.setEndTime(DateUtils.time(endTime));
        m2.setTimeTaken(time);
        m2.setMbps((int) mbps);
        AnalyticsService.getInstance().enqueue(taskInfo.getAuthConfig().getAuthInfo(), m2);

        AnalyticsMessage m_complete = AnalyticsMessage.from(taskInfo,
                                                            AnalyticsMessage.Event.COMPLETE,
                                                            "Complete Single Upload");
        m_complete.setStartTime(m2.getStartTime());
        m_complete.setEndTime(m2.getEndTime());
        m_complete.setTimeTaken(time);
        m_complete.setMbps((int) mbps);
        AnalyticsService.getInstance().enqueue(taskInfo.getAuthConfig().getAuthInfo(), m_complete);
      }

    } catch (Exception e) {
      e.printStackTrace();
      taskInfo.setStatus(TaskStatus.FAILED);
      taskInfo.setEndTime(System.currentTimeMillis());

      AnalyticsMessage m_err = AnalyticsMessage.from(taskInfo, AnalyticsMessage.Event.ERROR, "Error in Single Upload: " + e.getMessage());
      m_err.setStartTime(m1.getStartTime());
      m_err.setEndTime(DateUtils.now());
      AnalyticsService.getInstance().enqueue(taskInfo.getAuthConfig().getAuthInfo(), m_err);
    }
    finally {
      if(taskInfo.isCancelling()) {
        taskInfo.getAuthConfig().getStorage().delete(taskInfo.getBlobInfo().getBlobId());
        applicationEventPublisher.publishEvent(new TaskInfoMessage(taskInfo, TaskInfoMessage.Type.CANCEL));
      }
    }
  }
}
