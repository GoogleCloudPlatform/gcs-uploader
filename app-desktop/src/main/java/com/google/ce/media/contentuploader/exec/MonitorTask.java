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

import com.google.ce.media.contentuploader.message.StitchStatus;
import com.google.ce.media.contentuploader.message.TaskInfo;
import com.google.ce.media.contentuploader.message.TaskStatus;
import com.google.ce.media.contentuploader.message.Tasklet;

import java.util.Date;
import java.util.List;

public class MonitorTask implements Runnable {
    private final List<TaskInfo> modelTaskInfos;
    private final UploadTaskPool uploadTaskPool;

    public MonitorTask(List<TaskInfo> modelTaskInfos, UploadTaskPool uploadTaskPool) {
        this.modelTaskInfos = modelTaskInfos;
        this.uploadTaskPool = uploadTaskPool;
    }

    @Override
    public void run() {
        System.out.println(">> >> running monitor task at " + (new Date()));
        try {
            for (TaskInfo taskInfo : modelTaskInfos) {
                TaskStatus taskStatus = taskInfo.getStatus();
                switch (taskStatus) {
                    case WAITING:
                        break;
                    case QUEUED:
                        break;
                    case RUNNING:
                        if(!taskInfo.isCancelling()) {
                            checkRunningTask(taskInfo);
                        }
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
        System.out.println(">> >> monitor checking running task: " + taskInfo.getName());
        List<Tasklet> tasklets = taskInfo.getTasklets();
        boolean isOk = true;
        for (Tasklet tasklet : tasklets) {
            isOk = isOk && tasklet.getStatus() == TaskStatus.FINISHED;
        }
        System.out.println("all tasklets for ["+taskInfo.getName()+"] register finished: " + isOk);
        StitchStatus stitchStatus = taskInfo.getStitchStatus();
        boolean isOkStitch = stitchStatus == StitchStatus.PREPARING || stitchStatus == StitchStatus.STARTED;
        if(isOk && !isOkStitch && taskInfo.getEndTime() == 0L) {
            System.out.println(">> >> monitor triggering stitch for: " + taskInfo.getName());
            uploadTaskPool.enqueue(new StitchTask(true, taskInfo));
        }
    }
}
