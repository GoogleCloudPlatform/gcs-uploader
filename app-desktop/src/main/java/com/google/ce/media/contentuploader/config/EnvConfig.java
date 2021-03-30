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

package com.google.ce.media.contentuploader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Created in gcs-uploader on 2020-01-06.
 */
@Configuration
public class EnvConfig {

  @Value("${AUTH_ENDPOINT}")
  private String authEndpoint;

  @Value("${UPLOAD_QUEUE_SIZE:20}")
  private Integer uploadQueueSize;

  @Value("${UPLOAD_WORKER_SIZE:10}")
  private Integer uploadWorkerPoolSize;

  @Value("${TASK_WORKER_SIZE:10}")
  private Integer taskWorkerPoolSize;

  @Value("${REQUIRE_MEDIA_ID:true}")
  private Boolean requireMediaId;

  private Long uploadChunkSize;

  public String getAuthEndpoint() {
    return authEndpoint;
  }

  public Integer getUploadQueueSize() {
    return uploadQueueSize;
  }

  public Integer getUploadWorkerPoolSize() {
    return uploadWorkerPoolSize;
  }

  public Integer getTaskWorkerPoolSize() {
    return taskWorkerPoolSize;
  }

  public Boolean getRequireMediaId() {
    return requireMediaId;
  }
}
