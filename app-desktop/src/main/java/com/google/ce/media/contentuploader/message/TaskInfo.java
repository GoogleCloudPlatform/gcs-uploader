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

package com.google.ce.media.contentuploader.message;

import com.google.ce.media.contentuploader.config.AuthConfig;
import com.google.ce.media.contentuploader.exec.AnalyticsService;
import com.google.ce.media.contentuploader.exec.StitchTask;
import com.google.ce.media.contentuploader.exec.UploadTaskPool;
import com.google.ce.media.contentuploader.utils.DateUtils;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;

import java.io.File;
import java.security.SecureRandom;
import java.util.*;

/**
 * Created in gcs-uploader on 2019-12-19.
 */
public class TaskInfo {
  private static final Random RANDOM = new SecureRandom();

  private final String id;
  private final String name;
  private final File file;
  private String mediaId;
  private final boolean isDirectory;
  private final long size;
  private long startTime, endTime;
  private double mbps;
  private TaskStatus status = TaskStatus.WAITING;
  private StitchStatus stitchStatus;
  private Blob compositeBlob;
  private long uploaded = 0L;
  private final Object LOCK = new Object();
  private BlobInfo blobInfo;
  private final boolean isComposite;
  private final String correlationId;
  private final UploadTaskPool uploadTaskPool;
  private final AuthConfig authConfig;
  private List<Tasklet> tasklets;
  private String md5;
  private String crc32c;
  private String creationTime;
  private boolean cancelling = false;

  public TaskInfo(File file, UploadTaskPool uploadTaskPool, AuthConfig authConfig) {
    this.id = UUID.randomUUID().toString();
    this.name = file.getName();
    this.file = file;
    this.isDirectory = file.isDirectory();
    this.size = isDirectory ? 0 : file.length();
    this.uploadTaskPool = uploadTaskPool;
    this.authConfig = authConfig;
    this.creationTime = DateUtils.now();
    if(this.size > TaskConsts.COMPOSITE_LIMIT) {
      this.isComposite = true;
      stitchStatus = StitchStatus.PENDING;
      this.tasklets = new LinkedList<>();
      String prefix = Long.toString(System.currentTimeMillis(),36) + Long.toString(Math.abs(RANDOM.nextLong()), 36);
      correlationId =
              "content-uploader-app/"
                      + prefix
                      + "/"
                      + file.getName()
                      +"-"
                      +Long.toString(System.currentTimeMillis(), 36)
                      + "_";

    }
    else {
      this.isComposite = false;
      this.stitchStatus = StitchStatus.NA;
      this.correlationId = null;
    }
  }

  public String getId() {
    return id;
  }

  public StitchStatus getStitchStatus() {
    return stitchStatus;
  }

  public String getName() {
    return name;
  }

  public File getFile() {
    return file;
  }

  public boolean isDirectory() {
    return isDirectory;
  }

  public int getPctComplete() {
    return (int)(((uploaded * 1.0)/ (size * 1.0)) * 100.0);
  }

  public long secondsElapsed() {
    switch (status) {
      case WAITING:
        return 0L;
      case RUNNING:
        return System.currentTimeMillis() - startTime;
      case FINISHED:
      case FAILED:
        return endTime - startTime;
    }
    return 0L;
  }

  public long getSize() {
    return size;
  }

  public int getMbps() {
    return (int) mbps;
  }

  public void setMbps(double mbps) {
    this.mbps = mbps;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskInfo taskInfo = (TaskInfo) o;
    return id.equals(taskInfo.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public void stitchPreparing() {
    stitchStatus = StitchStatus.PREPARING;
  }

  public void stitchStarted() {
    stitchStatus = StitchStatus.STARTED;
  }

  public void stitchEnded(Blob blob, boolean crcMatch) {
    stitchStatus = crcMatch ? StitchStatus.FINISHED : StitchStatus.FAILED;
    this.compositeBlob = blob;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public void addTasklet(Tasklet tasklet) {
    tasklets.add(tasklet);
  }

  public void updateUploaded(long uploaded) {
    synchronized (LOCK) {
      this.uploaded += uploaded;
      long time = System.currentTimeMillis() - startTime;
      mbps = (this.uploaded * 8.0) / (time == 0 ? 1 : time) / 1000.0;
      new Thread(new Runnable() {
        @Override
        public void run() {
          if(TaskInfo.this.uploaded == size) {
            TaskInfo.this.status = TaskStatus.FINISHED;
          }
          if(status == TaskStatus.FINISHED && isComposite) {
            setEndTime(System.currentTimeMillis());
            boolean ok = tasklets.stream().map(tasklet -> tasklet.getStatus() == TaskStatus.FINISHED).reduce(true, (a, b) -> a && b);
            if(ok) {
              mbps = (size * 8.0) / (endTime - startTime) / 1000.0;
            }
            System.out.println("IS ALL OK> ["+getFile().getPath()+"] ok = " + ok);
            uploadTaskPool.enqueue(new StitchTask(ok, TaskInfo.this));

            AnalyticsMessage m_complete = AnalyticsMessage.from(TaskInfo.this,
                                                                AnalyticsMessage.Event.COMPOSITE_FILE_END,
                                                                "End Composite Upload of Segments");
            m_complete.setStartTime(DateUtils.time(startTime));
            m_complete.setEndTime(DateUtils.time(endTime));
            m_complete.setTimeTaken(endTime - startTime);
            m_complete.setMbps((int) mbps);
            AnalyticsService.getInstance().enqueue(TaskInfo.this.getAuthConfig().getAuthInfo(), m_complete);

          }
        }
      }).start();
    }
  }

  public AuthConfig getAuthConfig() {
    return authConfig;
  }

  public void setBlobInfo(BlobInfo blobInfo) {
    this.blobInfo = blobInfo;
  }

  public BlobInfo getBlobInfo() {
    return blobInfo;
  }

  public boolean isComposite() {
    return isComposite;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public List<Tasklet> getTasklets() {
    return tasklets;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

  public String getCrc32c() {
    return crc32c;
  }

  public void setCrc32c(String crc32c) {
    this.crc32c = crc32c;
  }

  public String getMediaId() {
    return mediaId;
  }

  public void setMediaId(String mediaId) {
    this.mediaId = mediaId;
  }

  public String getCreationTime() {
    return creationTime;
  }

  public boolean isCancelling() {
    return cancelling;
  }

  public void setCancelling(boolean cancelling) {
    this.cancelling = cancelling;
  }

  public UploadTaskPool getUploadTaskPool() {
    return uploadTaskPool;
  }
}
