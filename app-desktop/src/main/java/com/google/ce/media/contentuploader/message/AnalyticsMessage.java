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

import com.google.ce.media.contentuploader.utils.DateUtils;
import com.google.cloud.storage.BlobInfo;

/**
 * Created in gcs-uploader on 2/12/20.
 */
public class AnalyticsMessage {
  //required fields
  private String guid;
  private Type type;
  private String timestamp;
  private Integer index;
  private String username;
  private TaskStatus status;
  private Event event;
  private String blobId;
  private String bucket;
  private String objectName;

  //optional fields
  private String startTime;
  private String endTime;
  private String fileName;
  private Long size;
  private String mediaId;
  private Integer mbps;
  private Long timeTaken;
  private String comment;

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public String getGuid() {
    return guid;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }

  public Integer getIndex() {
    return index;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getBlobId() {
    return blobId;
  }

  public void setBlobId(String blobId) {
    this.blobId = blobId;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getObjectName() {
    return objectName;
  }

  public void setObjectName(String objectName) {
    this.objectName = objectName;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public String getMediaId() {
    return mediaId;
  }

  public void setMediaId(String mediaId) {
    this.mediaId = mediaId;
  }

  public Integer getMbps() {
    return mbps;
  }

  public void setMbps(Integer mbps) {
    this.mbps = mbps;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Event getEvent() {
    return event;
  }

  public Long getTimeTaken() {
    return timeTaken;
  }

  public void setTimeTaken(Long timeTaken) {
    this.timeTaken = timeTaken;
  }

  public enum Type {
    TASK,
    TASKLET,
    INTERMEDIATE,
    STATUS,
  }

  public enum Event {
    SINGLE_FILE_START,
    SINGLE_FILE_END,
    COMPOSITE_FILE_START,
    COMPOSITE_FILE_END,
    SEGMENT_START,
    SEGMENT_END,
    STITCH_START,
    STITCH_END,
    INFO,
    LOGIN,
    RETOKEN,
    COMPLETE,
    ERROR,
  }

  public static AnalyticsMessage from(TaskInfo taskInfo, Event event, String comment) {
    AnalyticsMessage message = new AnalyticsMessage();
    message.setGuid(taskInfo.getId());
    message.setType(Type.TASK);
    message.timestamp = DateUtils.now();
    message.setIndex(0);
    message.setSize(taskInfo.getSize());
    message.setBlobId(toBlobIdString(taskInfo.getBlobInfo()));
    message.setBucket(taskInfo.getBlobInfo().getBucket());
    message.setObjectName(taskInfo.getBlobInfo().getName());
    message.setFileName(taskInfo.getName());
    message.setUsername(taskInfo.getAuthConfig().getAuthInfo().getEmail());
    message.setMediaId(taskInfo.getMediaId());
    message.setStatus(taskInfo.getStatus());
    message.event = event;
    message.setComment(comment);

    return message;
  }

  public static AnalyticsMessage from(AuthInfo authInfo, Tasklet tasklet, Event event, String comment) {
    AnalyticsMessage message = new AnalyticsMessage();
    message.setGuid(tasklet.getTaskGuid());
    message.setType(Type.TASKLET);
    message.timestamp = DateUtils.now();
    message.setIndex(tasklet.getIndex());
    message.setSize((long) tasklet.getSize());
    message.setBlobId(toBlobIdString(tasklet.getBlobInfo()));
    message.setBucket(tasklet.getBlobInfo().getBucket());
    message.setObjectName(tasklet.getBlobInfo().getName());
    message.setUsername(authInfo.getEmail());
    message.setStatus(tasklet.getStatus());
    message.event = event;
    message.setComment(comment);

    return message;
  }

  public static AnalyticsMessage from(AuthInfo authInfo, Event event, String comment) {
    AnalyticsMessage message = new AnalyticsMessage();
    message.setGuid(authInfo.getEmail());
    message.setType(Type.STATUS);
    message.timestamp = DateUtils.now();
    message.setUsername(authInfo.getEmail());
    message.event = event;
    if(comment != null) {
      message.setComment(comment);
    }

    return message;
  }

  private static String toBlobIdString(BlobInfo blobInfo) {
    return "gs://"+blobInfo.getBucket()+"/"+blobInfo.getName();
  }
}
