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

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.gson.Gson;
import org.apache.commons.codec.Charsets;
import org.springframework.stereotype.Component;

/**
 * Created in gcs-uploader on 2/12/20.
 */
@Component
public class ExternalConfigProvider {
  private final EnvConfig envConfig;
  private final Storage storage;
  private final Gson gson = new Gson();

  public ExternalConfigProvider(EnvConfig envConfig,
                                Storage storage) {
    this.envConfig = envConfig;
    this.storage = storage;
  }

  public ExternalConfig getExternalConfig() {
    Blob blob = storage.get(BlobId.of(envConfig.getDestConfigBucket(), envConfig.getDestConfigFile()));
    String jsonString = new String(blob.getContent(), Charsets.UTF_8);
    ExternalConfig externalConfig = gson.fromJson(jsonString, ExternalConfig.class);

    return externalConfig;
  }
}
