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

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Strings;
import org.springframework.stereotype.Component;

/**
 * Created in gcs-uploader on 5/4/20.
 */
@Component
public class StoragePublisher {
  private final EnvConfig envConfig;
  private final boolean isPublishing;

  private final String bucket;
  private final String folder;

  private final Storage storage;


  public StoragePublisher(EnvConfig envConfig) {
    this.envConfig = envConfig;

    this.bucket = envConfig.getUploadNotificationBucket();
    String folderStr = envConfig.getUploadNotificationFolder();
    if(Strings.isNullOrEmpty(folderStr)) {
      this.folder = "";
    }
    else {
      this.folder = folderStr;
    }

    this.isPublishing = envConfig.getUploadNotificationBucket() != null;

    if(isPublishing) {
      this.storage = StorageOptions.getDefaultInstance().getService();
    }
    else {
      this.storage = null;
    }
  }

  public boolean isPublishing() {
    return isPublishing;
  }

  public String getBucket() {
    return bucket;
  }

  public String getFolder() {
    return folder;
  }

  public Storage getStorage() {
    return storage;
  }
}
