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

import com.google.ce.media.contentuploader.config.AuthConfig;
import com.google.ce.media.contentuploader.message.AnalyticsMessage;
import com.google.ce.media.contentuploader.message.TaskInfo;
import com.google.ce.media.contentuploader.message.Tasklet;
import com.google.ce.media.contentuploader.utils.DateUtils;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created in gcs-uploader on 2020-01-25.
 */
public class StitchTask implements Runnable {
  private Storage storage;
  private final boolean stitch;
  private final AuthConfig authConfig;
  private final TaskInfo taskInfo;
  private final List<BlobInfo> secondaryBlobs = new ArrayList<>();
  private final Object STITCH_LOCK = new Object();

  public StitchTask(boolean stitch, TaskInfo taskInfo) {
    this.stitch = stitch;
    this.authConfig = taskInfo.getAuthConfig();
    this.taskInfo = taskInfo;
  }

  @Override
  public void run() {
    Long startTime = System.currentTimeMillis();
    AnalyticsMessage m1 = AnalyticsMessage.from(taskInfo,
                                                AnalyticsMessage.Event.STITCH_START,
                                                "Started Composite Stitching");
    m1.setStartTime(DateUtils.time(startTime));
    AnalyticsService.getInstance().enqueue(taskInfo.getAuthConfig().getAuthInfo(), m1);

    storage = authConfig.getStorage();
    List<BlobInfo> blobInfos = taskInfo.getTasklets().stream().map(Tasklet::getBlobInfo).collect(Collectors.toList());
    if(stitch) {
      taskInfo.stitchPreparing();
      synchronized (STITCH_LOCK) {
        try {
          STITCH_LOCK.wait(5000L);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      doStitch(blobInfos, taskInfo.getBlobInfo(), 0);
    }
    taskInfo.getUploadTaskPool().enqueue(new CleanupTask(taskInfo, blobInfos));

    Long endTime = System.currentTimeMillis();
    AnalyticsMessage m2 = AnalyticsMessage.from(taskInfo,
                                                AnalyticsMessage.Event.STITCH_END,
                                                "End Composite Stitching");
    m2.setStartTime(DateUtils.time(startTime));
    m2.setEndTime(DateUtils.time(endTime));
    m2.setTimeTaken(endTime - startTime);
    m2.setMbps(taskInfo.getMbps());
    AnalyticsService.getInstance().enqueue(taskInfo.getAuthConfig().getAuthInfo(), m2);

    long uploadStartTime = taskInfo.getStartTime();
    long time = (endTime - uploadStartTime)/1000;
    AnalyticsMessage m_complete = AnalyticsMessage.from(taskInfo,
                                                AnalyticsMessage.Event.COMPLETE,
                                                "Complete Composite Upload");
    m_complete.setStartTime(DateUtils.time(uploadStartTime));
    m_complete.setEndTime(DateUtils.time(endTime));
    m_complete.setTimeTaken(time);
    m_complete.setMbps(taskInfo.getMbps());
    AnalyticsService.getInstance().enqueue(taskInfo.getAuthConfig().getAuthInfo(), m_complete);
  }

  private void doStitch(List<BlobInfo> blobInfos, BlobInfo finalBlobName, int generation) {
    taskInfo.stitchStarted();
    List<String> currGroup = new ArrayList<>();

    if(blobInfos.size() > 30) {
      List<BlobInfo> followOn = new ArrayList<>();
      int iteration = 0;

      for (BlobInfo blobInfo : blobInfos) {
        String info = blobInfo.getName();
        currGroup.add(info);
        System.out.println(">> info = " + info);
        if(currGroup.size() == 30) {
          buildFollowOn(currGroup, followOn, generation, iteration);
          currGroup.clear();
          iteration++;
        }
      }
      if(currGroup.size() > 0) {
        buildFollowOn(currGroup, followOn, generation, iteration);
        currGroup.clear();
      }
      generation++;
      secondaryBlobs.addAll(followOn);
      doStitch(followOn, finalBlobName, generation);
    }
    else {
      //do final version
      System.out.println(">> doing final with: ");

      for (BlobInfo blobInfo : blobInfos) {
        currGroup.add(blobInfo.getName());
      }
      currGroup.forEach(System.out::println);
      Blob finalBlob = storage.compose(Storage.ComposeRequest.newBuilder().addSource(currGroup).setTarget(finalBlobName).build());
      boolean crcMatch = finalBlob.getCrc32c().equals(taskInfo.getCrc32c());
      taskInfo.stitchEnded(finalBlob,crcMatch);
    }
  }

  private void buildFollowOn(List<String> currGroup, List<BlobInfo> followOn, int generation, int iteration) {
    BlobInfo targetBlob = BlobInfo.newBuilder(taskInfo.getBlobInfo().getBucket(),
                                              taskInfo.getCorrelationId()+"__g-"+generation+"__i-"+iteration).build();
    Blob newBlob = storage.compose(Storage.ComposeRequest.newBuilder().addSource(currGroup).setTarget(targetBlob).build());

    followOn.add(BlobInfo.newBuilder(newBlob.getBlobId()).build());
  }
}
