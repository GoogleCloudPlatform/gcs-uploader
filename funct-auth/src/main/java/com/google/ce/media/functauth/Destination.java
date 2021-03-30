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

package com.google.ce.media.functauth;

/**
 * Created in gcs-uploader on 2020-02-05.
 */
public class Destination {
  private String gcsBucket;
  private String gcsFolder;
  private String displayName;

  private Destination() {
  }

  public Destination(String gcsBucket, String gcsFolder, String displayName) {
    this.gcsBucket = gcsBucket;
    this.gcsFolder = gcsFolder;
    this.displayName = displayName;
  }

  public String getGcsBucket() {
    return gcsBucket;
  }

  public String getGcsFolder() {
    return gcsFolder;
  }

  public String getDisplayName() {
    return displayName;
  }
}
