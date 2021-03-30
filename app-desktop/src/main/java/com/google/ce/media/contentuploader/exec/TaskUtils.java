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

import com.google.common.hash.HashCode;
import org.apache.commons.codec.binary.Base64;

import java.nio.ByteBuffer;

/**
 * Created in gcs-uploader on 2020-01-27.
 */
public class TaskUtils {


  public static String crcToString(HashCode crcCode) {
    return hashIntToString(crcCode.asInt());
  }
  public static String hashIntToString(int number) {
    ByteBuffer intBuf = ByteBuffer.allocate(Integer.BYTES);
    intBuf.putInt(number);
    byte[] intBytes = intBuf.array();
    return Base64.encodeBase64String(intBytes);
  }

  public static String md5ToString(HashCode md5Code) {
    return Base64.encodeBase64String(md5Code.asBytes());
  }
}
