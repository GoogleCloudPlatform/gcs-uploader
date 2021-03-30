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

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.ce.media.contentuploader.message.AuthInfo;
import com.google.ce.media.contentuploader.message.Destination;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created in gcs-uploader on 2020-01-10.
 */
@Component
public class AuthConfig {
  public static final long REFRESH_THRESHOLD_SECS = 60*10L; //10 minutes
  public static final Object REFRESH_LOCK = new Object();
  private final EnvConfig envConfig;

  private AuthInfo authInfo;
  private GoogleCredentials credentials;
  private Storage storage;

  public AuthConfig(EnvConfig envConfig) {
    this.envConfig = envConfig;
  }


  public void updateToken(final AuthConfigListener listener) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.add("x-content-refresh-token", authInfo.getRefreshToken());
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setContentLength(0L);
    try {
      RequestEntity requestEntity =
              new RequestEntity(headers,
                                HttpMethod.POST,
                                new URI(envConfig.getAuthEndpoint()+"/auth/renew_access_token"));
      ResponseEntity<AuthInfo> authInfoResponse = restTemplate.exchange(requestEntity, AuthInfo.class);
      AuthInfo newAuthInfo = authInfoResponse.getBody();
      newAuthInfo.setRefreshToken(authInfo.getRefreshToken());
      authInfo = newAuthInfo;
      credentials =
              GoogleCredentials.create(new AccessToken(authInfo.getAccessToken(),
                                                       new Date(authInfo.getExpirationSeconds() * 1000)));
      buildStorage();
      validateAuthorization();
      if (listener != null) {
        listener.authInfoUpdated();
      }
    } catch (Exception e) {
      e.printStackTrace();
      if (listener != null) {
        listener.authInfoError(e);
      }
    }
  }

  public AuthInfo swapToken(String offlineCode, String host) throws URISyntaxException {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.add("x-content-upload-offline-code", offlineCode);
    headers.add("x-content-upload-host", host);
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setContentLength(0L);
    RequestEntity requestEntity =
            new RequestEntity(headers,
                              HttpMethod.POST,
                              new URI(envConfig.getAuthEndpoint()+"/auth/offline_info"));
    ResponseEntity<AuthInfo> authInfoResponse = restTemplate.exchange(requestEntity, AuthInfo.class);
    authInfo = authInfoResponse.getBody();
    credentials =
            GoogleCredentials.create(new AccessToken(authInfo.getAccessToken(),
                                                     new Date(authInfo.getExpirationSeconds() * 1000)));
    buildStorage();
    validateAuthorization();
    return authInfo;
  }

  public Storage getStorage() {
    synchronized (REFRESH_LOCK) {
      if((authInfo.getExpirationSeconds() - (System.currentTimeMillis()/1000L)) < REFRESH_THRESHOLD_SECS) {
        updateToken(null);
      }
      return storage;
    }
  }

  public AuthInfo getAuthInfo() {
    return authInfo;
  }

  private void buildStorage() {
    storage = StorageOptions
            .newBuilder()
            .setCredentials(credentials)
            .build()
            .getService();
  }

  public interface AuthConfigListener {
    public void authInfoUpdated();
    public void authInfoError(Exception e);
  }

  private void validateAuthorization() {
    List<Destination> destinations = authInfo.getDestinations();
    List<Destination> authorizedDestinations = new ArrayList<>();
    authInfo.setAuthorizedDestinations(authorizedDestinations);

    for (Destination destination : destinations) {
      try {
        Page<Blob> list = storage.list(destination.getGcsBucket(), Storage.BlobListOption.pageSize(1));
        authorizedDestinations.add(destination);
      }
      catch (StorageException e) {
        System.out.println("destination not accessible");
      }
    }

  }
}

