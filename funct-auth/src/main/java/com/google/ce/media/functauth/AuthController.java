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

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;

/**
 */
@RestController
@RequestMapping(
        value = "/auth",
        produces = "application/json",
        consumes = "application/json")
public class AuthController {

  private final JacksonFactory JACKSON_FACTORY = JacksonFactory.getDefaultInstance();
  private final NetHttpTransport NET_HTTP_TRANSPORT = new NetHttpTransport();
  private final EnvConfig envConfig;
  private final ExternalConfigProvider externalConfigProvider;


  public AuthController(EnvConfig envConfig, ExternalConfigProvider externalConfigProvider) {
    this.envConfig = envConfig;
    this.externalConfigProvider = externalConfigProvider;
  }

  @RequestMapping(value = "/offline_info", method = RequestMethod.POST, produces = "application/json")
  @ResponseBody
  public ResponseEntity<AuthInfo> getOfflineUploadInfo(@RequestHeader(value = "x-content-upload-offline-code") String offlineCode,
                                                       @RequestHeader(value = "x-content-upload-host") String desktopHost,
                                                       HttpServletRequest request) throws IOException {
    Enumeration<String> headers = request.getHeaderNames();
    while (headers.hasMoreElements()) {
      String header = headers.nextElement();
      System.out.println("[" + header +"] = " + request.getHeader(header));
    }

    String url = request.getRequestURL().toString();
    String protocol = url.substring(0, url.indexOf(':'));

    System.out.println("host = " + desktopHost);
    System.out.println("offlineCode = " + offlineCode);

    GoogleTokenResponse tokenResponse =
            new GoogleAuthorizationCodeTokenRequest(
                    NET_HTTP_TRANSPORT,
                    JACKSON_FACTORY,
                    envConfig.getClientId(),
                    envConfig.getClientSecret(),
                    offlineCode,
                    protocol+"://"+desktopHost).execute();

    AuthInfo authInfo = processTokenResponse(tokenResponse);

    return new ResponseEntity<>(authInfo, HttpStatus.OK);
  }

  @RequestMapping(value = "/renew_access_token", method = RequestMethod.POST, produces = "application/json")
  @ResponseBody
  public ResponseEntity<AuthInfo> getUpdatedAccessToken(@RequestHeader(value = "x-content-refresh-token") String refreshToken) throws IOException {
    GoogleTokenResponse tokenResponse =
            new GoogleRefreshTokenRequest(
                    NET_HTTP_TRANSPORT,
                    JACKSON_FACTORY,
                    refreshToken,
                    envConfig.getClientId(),
                    envConfig.getClientSecret()).execute();


    AuthInfo authInfo = processTokenResponse(tokenResponse);

    return new ResponseEntity<>(authInfo, HttpStatus.OK);
  }

  private AuthInfo processTokenResponse(GoogleTokenResponse tokenResponse) throws IOException {
    AuthInfo authInfo = new AuthInfo();
    GoogleIdToken idToken = tokenResponse.parseIdToken();
    GoogleIdToken.Payload payload = idToken.getPayload();
    authInfo.setUserId(payload.getSubject());  // Use this value as a key to identify a user.
    authInfo.setEmail(payload.getEmail());
    authInfo.setName((String) payload.get("name"));
    authInfo.setPictureUrl((String) payload.get("picture"));
    authInfo.setIssuedSeconds(payload.getIssuedAtTimeSeconds());
    authInfo.setExpirationSeconds(payload.getExpirationTimeSeconds());

    String accessToken = tokenResponse.getAccessToken();
    System.out.println("accessToken = " + accessToken);
    authInfo.setAccessToken(accessToken);

    String refreshToken = tokenResponse.getRefreshToken();
    System.out.println("refreshToken = " + refreshToken);
    authInfo.setRefreshToken(refreshToken);

    //get fresh config from GCS
    ExternalConfig externalConfig = externalConfigProvider.getExternalConfig();
    authInfo.setBucket(envConfig.getDestConfigBucket());
    authInfo.setDestinations(externalConfig.getDestinations());
    authInfo.setAnalyticsEndpoint(externalConfig.getAnalyticsEndpoint());
    authInfo.setAnalyticsUsername(externalConfig.getAnalyticsAuthUsername());
    authInfo.setAnalyticsPassword(externalConfig.getAnalyticsAuthPassword());

    return authInfo;
  }



}
