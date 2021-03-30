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

package com.google.ce.media.contentuploader.controllers;

import com.google.ce.media.contentuploader.config.AuthConfig;
import com.google.ce.media.contentuploader.config.EnvConfig;
import com.google.ce.media.contentuploader.exec.AnalyticsService;
import com.google.ce.media.contentuploader.message.AnalyticsMessage;
import com.google.ce.media.contentuploader.message.AuthInfo;
import com.google.ce.media.contentuploader.message.SetupInfo;
import com.google.ce.media.contentuploader.message.UserInfo;
import com.google.ce.media.contentuploader.ui.UiMasterController;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created in gcs-uploader on 2020-01-06.
 */
@RestController
@RequestMapping(value = "/token", produces = "application/json")
public class TokenController {

  private final AuthConfig authConfig;
  private final UiMasterController uiMasterController;
  private final EnvConfig envConfig;

  public TokenController(AuthConfig authConfig, UiMasterController uiMasterController, EnvConfig envConfig) {
    this.authConfig = authConfig;
    this.uiMasterController = uiMasterController;
    this.envConfig = envConfig;
  }

  @RequestMapping(value = "/swap", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<UserInfo> swapCode(@RequestHeader(value = "x-content-upload-offline-code") String offlineCode,
                                           @RequestHeader(value = "host") String host,
                                           HttpServletRequest request) throws URISyntaxException {

    AuthInfo authInfo = authConfig.swapToken(offlineCode, host);
    uiMasterController.validateAuthorization();

    UserInfo userInfo = new UserInfo();
    userInfo.setPictureUrl(authInfo.getPictureUrl());
    userInfo.setEmail(authInfo.getEmail());
    userInfo.setUserId(authInfo.getUserId());
    userInfo.setName(authInfo.getName());

    AnalyticsMessage m1 = AnalyticsMessage.from(authInfo, AnalyticsMessage.Event.LOGIN, "Login");
    AnalyticsService.getInstance().enqueue(authInfo, m1);


    return new ResponseEntity<>(userInfo, HttpStatus.OK);
  }

  @RequestMapping(value = "/setup", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<SetupInfo> getSetup() throws URISyntaxException {
    RequestEntity requestEntity =
            new RequestEntity(HttpMethod.GET,
                              new URI(envConfig.getAuthEndpoint()+"/setup"));

    RestTemplate restTemplate = new RestTemplate();
    ResponseEntity<SetupInfo> responseEntity = restTemplate.exchange(requestEntity, SetupInfo.class);
    return new ResponseEntity<>(responseEntity.getBody(), HttpStatus.OK);
  }
}
