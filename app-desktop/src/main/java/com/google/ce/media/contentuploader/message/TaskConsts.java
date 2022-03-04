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

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Created in gcs-uploader on 2020-01-25.
 */
public class TaskConsts {
  public static final long COMPOSITE_CHUNK_SIZE = 1024*1024*128; //128 mb
  public static final long COMPOSITE_LIMIT = 1024L * 1024L * 256L; // 256mb

  public static final HashFunction md5Digest = Hashing.md5();
  public static final HashFunction crcDigest = Hashing.crc32c();

}
