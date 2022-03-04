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

import com.google.ce.media.contentuploader.message.*;
import com.google.ce.media.contentuploader.utils.DateUtils;
import com.google.cloud.storage.BlobInfo;
import org.apache.commons.codec.digest.PureJavaCrc32C;
import org.springframework.context.ApplicationEventPublisher;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created in gcs-uploader on 2020-01-23.
 */
public class CompositeUploadTask implements Runnable {
  private final TaskInfo taskInfo;
  private final UploadTaskPool uploadTaskPool;
  //  private final Hasher md5HasherTotal = md5Digest.newHasher();
//  private final Hasher crcHasherTotal = crcDigest.newHasher();
  private final PureJavaCrc32C crc32C = new PureJavaCrc32C();

  public CompositeUploadTask(TaskInfo taskInfo,
                             UploadTaskPool uploadTaskPool,
                             ApplicationEventPublisher applicationEventPublisher) throws NoSuchAlgorithmException {
    this.taskInfo = taskInfo;
    this.uploadTaskPool = uploadTaskPool;
  }

  @Override
  public void run() {
    if (taskInfo.getStatus() != TaskStatus.WAITING && taskInfo.getStatus() != TaskStatus.QUEUED) {
      return;
    }
    try {
      Long startTime = System.currentTimeMillis();

      taskInfo.setStatus(TaskStatus.RUNNING);
      taskInfo.setStartTime(startTime);

      AnalyticsMessage m1 = AnalyticsMessage.from(taskInfo, AnalyticsMessage.Event.COMPOSITE_FILE_START, "Started Composite Upload");
      m1.setStartTime(DateUtils.time(startTime));
      AnalyticsService.getInstance().enqueue(taskInfo.getAuthConfig().getAuthInfo(), m1);

      FileInputStream fileInputStream = new FileInputStream(taskInfo.getFile());
      long read = 0;
      int count = 0;
      int index = 0;
      while (read < taskInfo.getSize() && !taskInfo.isCancelling()) {
        byte[] buffer;
        if(read + TaskConsts.COMPOSITE_CHUNK_SIZE < taskInfo.getSize()) {
          buffer = new byte[(int) TaskConsts.COMPOSITE_CHUNK_SIZE];
        }
        else {
          buffer = new byte[(int) (taskInfo.getSize() - read)];
        }
        int segRead = 0;
        while (segRead < buffer.length) {
          count = fileInputStream.read(buffer, segRead, buffer.length-segRead);
          read += count;
          segRead += count;
        }
//        md5HasherTotal.putBytes(buffer);
//        crcHasherTotal.putBytes(buffer);
        crc32C.update(buffer, 0, buffer.length);
        Map<String,String> metadata = new HashMap<>();
        metadata.put("x-uploader-file-category", "temp");
        String shard = "" + (System.currentTimeMillis() % 100L);
        BlobInfo blobInfo = BlobInfo.newBuilder(taskInfo.getBlobInfo().getBucket(),
                                                shard + "-" + taskInfo.getCorrelationId() + index)
                .setMetadata(metadata)
                .build();
        Tasklet tasklet = new Tasklet(taskInfo.getId(), blobInfo, index, buffer.length);
        taskInfo.addTasklet(tasklet);
        BlobUploadTask blobUploadTask = new BlobUploadTask(taskInfo,
                                                           tasklet,
                                                           ByteBuffer.wrap(buffer, 0, count));
        uploadTaskPool.enqueue(blobUploadTask);
        index++;
      }
//      taskInfo.setMd5(TaskUtils.md5ToString(md5HasherTotal.hash()));
//      taskInfo.setCrc32c(TaskUtils.crcToString(crcHasherTotal.hash()));
      if(!taskInfo.isCancelling()) {
        taskInfo.setCrc32c(TaskUtils.hashIntToString((int) crc32C.getValue()));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
