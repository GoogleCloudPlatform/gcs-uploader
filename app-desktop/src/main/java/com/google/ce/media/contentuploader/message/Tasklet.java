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

import com.google.cloud.storage.BlobInfo;

/**
 * Created in gcs-uploader on 2019-12-19.
 */
public class Tasklet {
  private final String taskGuid;
  private final BlobInfo blobInfo;
  private final int index;
  private TaskStatus status;
  private final int size;
  private int errorCount = 0;


  public Tasklet(String taskGuid, BlobInfo blobInfo, int index, int size) {
    this.taskGuid = taskGuid;
    this.blobInfo = blobInfo;
    this.index = index;
    this.size = size;
    this.status = TaskStatus.WAITING;
  }

  public String getTaskGuid() {
    return taskGuid;
  }

  public int getSize() {
    return size;
  }

  public BlobInfo getBlobInfo() {
    return blobInfo;
  }

  public int getIndex() {
    return index;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public int getErrorCount() {
    return errorCount;
  }

  public void incrementErrorCount() {
    errorCount++;
  }
}
