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

package com.google.ce.media.functanalytics;

import com.google.cloud.ServiceOptions;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Created in gcs-uploader on 2/11/20.
 */
@Configuration
public class EnvConfig {

  @Value("${UPLOAD_NOTIFICATION_TOPIC:@null}")
  private String uploadNotificationTopic;

  @Value("${UPLOAD_NOTIFICATION_BUCKET:@null}")
  private String uploadNotificationBucket;

  @Value("${UPLOAD_NOTIFICATION_FOLDER:@null}")
  private String uploadNotificationFolder;

  @Value("${DATASET_NAME:@null}")
  private String datasetName;

  @Value("${TABLE_NAME:@null}")
  private String tableName;

  @Value("${ENABLE_BIG_QUERY:true}")
  private boolean enableBigQuery;

  private String projectId;

  @PostConstruct
  private void init() {
    projectId = ServiceOptions.getDefaultProjectId();
  }

  public String getUploadNotificationTopic() {
    return uploadNotificationTopic;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public String getTableName() {
    return tableName;
  }

  public boolean isEnableBigQuery() {
    return enableBigQuery;
  }

  public String getUploadNotificationBucket() {
    return uploadNotificationBucket;
  }

  public String getUploadNotificationFolder() {
    return uploadNotificationFolder;
  }

  public String getProjectId() {
    return projectId;
  }
}
