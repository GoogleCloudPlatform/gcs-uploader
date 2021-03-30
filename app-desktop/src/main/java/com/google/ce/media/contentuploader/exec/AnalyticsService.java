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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created in gcs-uploader on 2/13/20.
 */
public class AnalyticsService {
  private static final AnalyticsService instance = new AnalyticsService();

  private final ExecutorService runner = Executors.newSingleThreadExecutor();

  public void enqueue(AuthInfo authInfo, AnalyticsMessage message) {
    runner.submit(new AnalyticsTask(authInfo, message));
  }

  public static AnalyticsService getInstance() {
    return instance;
  }
}
