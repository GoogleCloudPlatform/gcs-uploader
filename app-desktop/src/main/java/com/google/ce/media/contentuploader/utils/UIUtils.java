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

package com.google.ce.media.contentuploader.utils;

import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Created in gcs-uploader on 3/19/20.
 */
public class UIUtils {
  private static Image appImage;
  static {
    try {
      appImage = ImageIO.read(new ClassPathResource("images/Generic_GCP.png").getInputStream());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static ImageIcon getApplicationIcon(int height) {
    return new ImageIcon(appImage.getScaledInstance(-1, height, Image.SCALE_SMOOTH));
  }
}
