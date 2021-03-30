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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Enumeration;

/**
 * Created in gcs-uploader on 2020-01-06.
 */
@RestController
@RequestMapping(value = "/", produces = "application/json")
public class InfoController {
  private final Gson gson = new Gson();
  private final EnvConfig envConfig;

  public InfoController(EnvConfig envConfig) {
    this.envConfig = envConfig;
  }



  @RequestMapping(method = RequestMethod.GET)
  public ResponseEntity<String> getInfo(HttpServletRequest request) {
    JsonObject headerJson = new JsonObject();
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String header = headerNames.nextElement();
      String val = request.getHeader(header);
      headerJson.addProperty(header, val);
    }

    Date date = new Date();
    JsonObject jsonObject = new JsonObject();
    jsonObject.add("headers", headerJson);
    jsonObject.addProperty("time", date.getTime());
    jsonObject.addProperty("date", date.toString());


    return new ResponseEntity<>(gson.toJson(jsonObject), HttpStatus.OK);
  }

  @RequestMapping(value = "/setup", method = RequestMethod.GET)
  public ResponseEntity<String> getSetup(HttpServletRequest request) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("clientId", envConfig.getClientId());
    jsonObject.addProperty("scopes", envConfig.getScopes());

    return new ResponseEntity<>(gson.toJson(jsonObject), HttpStatus.OK);
  }
}
