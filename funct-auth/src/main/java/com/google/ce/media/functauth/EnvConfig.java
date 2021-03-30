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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Created in gcs-uploader on 2020-01-06.
 */
@Configuration
public class EnvConfig {

  @Value("${DEST_CONFIG_BUCKET}")
  private String destConfigBucket;

  @Value("${DEST_CONFIG_FILE}")
  private String destConfigFile;

  @Value("${CLIENT_ID}")
  private String clientId;

  @Value("${CLIENT_SECRET}")
  private String clientSecret;

  @Value("${SCOPES}")
  private String scopes;

  private String[] scopeList;

  @PostConstruct
  protected void init() {
    scopeList = scopes.split(",");
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String[] getScopeList() {
    return scopeList;
  }

  public String getScopes() {
    return scopes;
  }

  public String getDestConfigBucket() {
    return destConfigBucket;
  }

  public String getDestConfigFile() {
    return destConfigFile;
  }
}
