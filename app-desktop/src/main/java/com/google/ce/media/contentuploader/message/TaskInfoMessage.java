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

import org.springframework.context.ApplicationEvent;

/**
 * Created in gcs-uploader on 2020-03-17.
 */
public class TaskInfoMessage extends ApplicationEvent {
  private final Type type;

  /**
   * Create a new {@code ApplicationEvent}.
   *
   * @param source the object on which the event initially occurred or with
   *               which the event is associated (never {@code null})
   */
  public TaskInfoMessage(TaskInfo source, Type type) {
    super(source);
    this.type = type;
  }

  public TaskInfo getTaskInfo() {
    return (TaskInfo) source;
  }

  public Type getType() {
    return type;
  }

  public enum Type {
    CANCEL,
  }
}
