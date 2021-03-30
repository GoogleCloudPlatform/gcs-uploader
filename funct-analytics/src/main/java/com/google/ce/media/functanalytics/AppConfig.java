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

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Created in gcs-uploader on 2020-02-05.
 */
@Component
public class AppConfig {

  private final EnvConfig envConfig;

  public AppConfig(EnvConfig envConfig) {
    this.envConfig = envConfig;
  }

  @Bean
  public Storage getStorage() {
    return StorageOptions.getDefaultInstance().getService();
  }

  @Bean
  public BigQuery getBigQuery() {
    return BigQueryOptions.getDefaultInstance().getService();
  }

  @Bean
  public Datastore getDatastore() {
    return DatastoreOptions.getDefaultInstance().getService();
  }

}
