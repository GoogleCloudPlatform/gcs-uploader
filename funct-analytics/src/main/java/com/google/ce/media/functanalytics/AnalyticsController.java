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

package com.google.ce.media.functanalytics;

import com.google.cloud.WriteChannel;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created in gcs-uploader on 2020-02-05.
 */
@RestController
@RequestMapping(value = "/analytics", produces = "application/json", consumes = "application/json")
public class AnalyticsController {
  private static final Logger LOGGER = Logger.getLogger(AnalyticsController.class.getName());

  private final Storage storage;
  private final Datastore datastore;
  private final BigQuery bigQuery;
  private final TopicPublishers topicPublishers;
  private final StoragePublisher storagePublisher;
  private final EnvConfig envConfig;
  private final TableId tableId;
  private final Gson gson = new Gson();

  public AnalyticsController(Storage storage,
      Datastore datastore,
      BigQuery bigQuery,
      TopicPublishers topicPublishers,
      StoragePublisher storagePublisher,
      EnvConfig envConfig) {
    this.storage = storage;
    this.datastore = datastore;
    this.bigQuery = bigQuery;
    this.topicPublishers = topicPublishers;
    this.storagePublisher = storagePublisher;
    this.envConfig = envConfig;

    if (this.envConfig.isEnableBigQuery()) {
      this.tableId = TableId.of(envConfig.getDatasetName(), envConfig.getTableName());
    }
    else {
      tableId = null;
    }
  }

  @RequestMapping(value = "/event", method = RequestMethod.POST)
  @ResponseBody
  public Map<String, String> getInfo(@RequestBody AnalyticsItem analyticsItem) {

    Map<String,String> map = new HashMap<>();

    if(envConfig.isEnableBigQuery()) {
      InsertAllResponse response = insertAnalyticsItem(analyticsItem);
      map.put("status", response.hasErrors()?"error":"ok");
    }
    else {
      map.put("status", "ok");
    }

    if("COMPLETE".equals(analyticsItem.getEvent())) {
      JsonObject notification = new JsonObject();
      notification.addProperty("bucket", analyticsItem.getBucket());
      notification.addProperty("objectName", analyticsItem.getObjectName());
      notification.addProperty("fileName", analyticsItem.getFileName());
      notification.addProperty("username", analyticsItem.getUsername());
      notification.addProperty("mediaId", analyticsItem.getMediaId());
      notification.addProperty("size", analyticsItem.getSize());
      notification.addProperty("guid", analyticsItem.getGuid());

      String notificationString = gson.toJson(notification);
      LOGGER.info("notification string is: " + notificationString);
      if (topicPublishers.isPublishing()) {
        LOGGER.info("will publish to topic: " + envConfig.getUploadNotificationTopic());
        PubsubMessage message =
                PubsubMessage
                        .newBuilder()
                        .setData(ByteString.copyFrom(notificationString, StandardCharsets.UTF_8))
                        .build();
        topicPublishers.getUploadNotificationPublisher().publish(message);
      }
      if(storagePublisher.isPublishing()) {
        String objectName = storagePublisher.getFolder();
        if (objectName.length() > 0) {
          objectName += "/";
        }
        objectName += analyticsItem.getObjectName();
        objectName += "-"+analyticsItem.getGuid()+".json";
        LOGGER.info("will publish to storage: gs://" + envConfig.getUploadNotificationBucket() + "/" + objectName );
        BlobInfo blobInfo =
            BlobInfo.newBuilder(storagePublisher.getBucket(), objectName)
                .setContentType("application/json")
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .build();
        byte[] bytes = notificationString.getBytes(StandardCharsets.UTF_8);
        exponentialWrite(blobInfo, bytes, 0);
      }
      LOGGER.info("sent COMPLETE notification: " + notificationString);
    }

    return map;
  }

  private final Object LOCK = new Object();
  private void exponentialWrite(BlobInfo blobInfo, byte[] content, int iteration) {
    if(iteration > 5) {
      LOGGER.severe("exceeded max writes to storage for object: gs://" + blobInfo.getBucket() + "/" + blobInfo.getName());
      return;
    }
    if(iteration > 0) {
      synchronized (LOCK) {
        try {
          LOCK.wait(iteration * 250L);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    Blob blob = storagePublisher.getStorage().create(blobInfo);
    try {
      WriteChannel writeChannel = blob.writer();
      writeChannel.write(ByteBuffer.wrap(content));
      writeChannel.close();
      LOGGER.info("storage iteration["+iteration+"] successfully wrote object ("+content.length+" bytes): gs://"+blobInfo.getBucket() +"/" + blobInfo.getName());
    } catch (IOException e) {
      e.printStackTrace();
      iteration++;
      exponentialWrite(blobInfo, content, iteration);
    }
  }

  private InsertAllResponse insertAnalyticsItem(AnalyticsItem analyticsItem) {
    Map<String,Object> row = new HashMap<>();
    row.put("guid",      analyticsItem.getGuid());
    row.put("type",      analyticsItem.getType());
    row.put("timestamp", analyticsItem.getTimestamp());
    row.put("index",     analyticsItem.getIndex());
    row.put("username",  analyticsItem.getUsername());
    row.put("status",    analyticsItem.getStatus());
    row.put("event",     analyticsItem.getEvent());
    row.put("blobId",    analyticsItem.getBlobId());
    row.put("startTime", analyticsItem.getStartTime());
    row.put("endTime",   analyticsItem.getEndTime());
    row.put("fileName",  analyticsItem.getFileName());
    row.put("size",      analyticsItem.getSize());
    row.put("mediaId",   analyticsItem.getMediaId());
    row.put("mbps",      analyticsItem.getMbps());
    row.put("timeTaken", analyticsItem.getTimeTaken());
    row.put("comment",   analyticsItem.getComment());

    InsertAllRequest.Builder builder = InsertAllRequest.newBuilder(tableId);
    builder.addRow(row);
    InsertAllRequest request = builder.build();
    InsertAllResponse response = bigQuery.insertAll(request);
    return response;
  }


}
