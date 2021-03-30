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

package com.google.ce.media.contentuploader.exec;

import com.google.ce.media.contentuploader.message.AnalyticsMessage;
import com.google.ce.media.contentuploader.message.AuthInfo;
import com.google.gson.Gson;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created in gcs-uploader on 2/12/20.
 */
public class AnalyticsTask implements Runnable {

  private static final Charset charset = StandardCharsets.UTF_8;
  private final AuthInfo authInfo;
  private final AnalyticsMessage message;
  private final RestTemplate restTemplate;
  private final Gson gson = new Gson();

  public AnalyticsTask(AuthInfo authInfo, AnalyticsMessage message) {
    this.authInfo = authInfo;
    this.message = message;
    this.restTemplate = new RestTemplate();
    this.restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(authInfo.getAnalyticsUsername(), authInfo.getAnalyticsPassword(), charset));
  }

  @Override
  public void run() {
    String url = authInfo.getAnalyticsEndpoint()+"/analytics/event";
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      RequestEntity requestEntity = new RequestEntity<>(gson.toJson(message), headers, HttpMethod.POST, new URI(url));
      ResponseEntity<AnalyticsResponse> response = restTemplate.exchange(requestEntity, AnalyticsResponse.class);
      System.out.println("response.getStatusCodeValue() = " + response.getStatusCodeValue());
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  private static class AnalyticsResponse {

  }
}
