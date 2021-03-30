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

package com.google.ce.media.contentuploader.message;

import java.util.List;

/**
 * Created in gcs-uploader on 2020-01-06.
 */
public class AuthInfo {

  private String bucket;
  private String accessToken;
  private String refreshToken;

  private String userId, name, email, pictureUrl;

  private Long issuedSeconds, expirationSeconds;

  private List<Destination> destinations;

  private List<Destination> authorizedDestinations;

  private String analyticsEndpoint;

  private String analyticsUsername, analyticsPassword;

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPictureUrl() {
    return pictureUrl;
  }

  public void setPictureUrl(String pictureUrl) {
    this.pictureUrl = pictureUrl;
  }

  public Long getIssuedSeconds() {
    return issuedSeconds;
  }

  public void setIssuedSeconds(Long issuedSeconds) {
    this.issuedSeconds = issuedSeconds;
  }

  public Long getExpirationSeconds() {
    return expirationSeconds;
  }

  public void setExpirationSeconds(Long expirationSeconds) {
    this.expirationSeconds = expirationSeconds;
  }

  public List<Destination> getDestinations() {
    return destinations;
  }

  public void setDestinations(List<Destination> destinations) {
    this.destinations = destinations;
  }

  public String getAnalyticsEndpoint() {
    return analyticsEndpoint;
  }

  public void setAnalyticsEndpoint(String analyticsEndpoint) {
    this.analyticsEndpoint = analyticsEndpoint;
  }

  public String getAnalyticsUsername() {
    return analyticsUsername;
  }

  public void setAnalyticsUsername(String analyticsUsername) {
    this.analyticsUsername = analyticsUsername;
  }

  public String getAnalyticsPassword() {
    return analyticsPassword;
  }

  public void setAnalyticsPassword(String analyticsPassword) {
    this.analyticsPassword = analyticsPassword;
  }

  public List<Destination> getAuthorizedDestinations() {
    return authorizedDestinations;
  }

  public void setAuthorizedDestinations(List<Destination> authorizedDestinations) {
    this.authorizedDestinations = authorizedDestinations;
  }
}
