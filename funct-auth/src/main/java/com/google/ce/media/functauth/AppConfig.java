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

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.util.Collections;
import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 */
@Component
public class AppConfig {
  private final EnvConfig envConfig;

  public AppConfig(EnvConfig envConfig) {
    this.envConfig = envConfig;
  }

  @PostConstruct
  private void init() {
  }

  @Bean
  public Storage getStorage() throws IOException {
    return StorageOptions.getDefaultInstance().getService();
  }

  @Bean
  public GoogleIdTokenVerifier getVerifier() {
    return new GoogleIdTokenVerifier.Builder(new NetHttpTransport.Builder().build(),
                                             GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(envConfig.getClientId()))
            .build();
  }

  @Bean
  public GoogleCredentials getGoogleCredentials() throws IOException {
    return ServiceAccountCredentials.getApplicationDefault().createScoped(envConfig.getScopeList());
  }

}
