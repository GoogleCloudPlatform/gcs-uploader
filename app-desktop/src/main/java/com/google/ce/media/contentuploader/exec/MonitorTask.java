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

import com.google.ce.media.contentuploader.message.TaskInfo;
import com.google.ce.media.contentuploader.message.TaskStatus;
import com.google.ce.media.contentuploader.message.Tasklet;

import java.util.List;

public class MonitorTask implements Runnable {
    private final List<TaskInfo> modelTaskInfos;

    public MonitorTask(List<TaskInfo> modelTaskInfos) {
        this.modelTaskInfos = modelTaskInfos;
    }

    @Override
    public void run() {
        try {
            for (TaskInfo taskInfo : modelTaskInfos) {
                TaskStatus taskStatus = taskInfo.getStatus();
                switch (taskStatus) {
                    case WAITING:
                        break;
                    case QUEUED:
                        break;
                    case RUNNING:
                        checkRunningTask(taskInfo);
                        break;
                    case FINISHED:
                        break;
                    case FAILED:
                        break;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkRunningTask(TaskInfo taskInfo) {
        List<Tasklet> tasklets = taskInfo.getTasklets();

    }
}
