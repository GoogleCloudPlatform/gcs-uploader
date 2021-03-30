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

package com.google.ce.media.contentuploader.ui;

import com.google.ce.media.contentuploader.message.TaskInfo;
import com.google.ce.media.contentuploader.message.TaskStatus;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;

/**
 * Created in gcs-uploader on 2020-01-10.
 */
public class FileTaskTableRenderer implements TableCellRenderer {

  private final FileTaskTableModel taskTableModel;

  private final JLabel nameLabel = new JLabel();
  private final JLabel sizeLabel = new JLabel();
  private final JLabel mediaIdLabel = new JLabel();
  private final JLabel statusLabel = new JLabel();
  private final JProgressBar progressBar = new JProgressBar(0, 100);
  private final JLabel mbpsLabel = new JLabel();
  private final JLabel creationTimeLabel = new JLabel();
  private final JLabel secLabel = new JLabel();
  private final JLabel stitchLabel = new JLabel();
  private final JButton cancelButton = new JButton("Cancel");
  private final JLabel cancellingLabel = new JLabel("Cancelling");
  private final JLabel blankLabel = new JLabel(" ");

  public FileTaskTableRenderer(FileTaskTableModel taskTableModel) {
    this.taskTableModel = taskTableModel;
    this.statusLabel.setOpaque(true);
    this.stitchLabel.setOpaque(true);
    this.mediaIdLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
    this.cancelButton.addActionListener(e->{
      System.out.println("e = " + e);
    });
    this.sizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    this.secLabel.setHorizontalAlignment(SwingConstants.RIGHT);
  }

  @Override
  public Component getTableCellRendererComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 boolean hasFocus,
                                                 int row,
                                                 int column) {
    if(value == null) {
      return null;
    }
    TaskInfo taskInfo = (TaskInfo) value;
    switch (column) {
      case 0: {
        nameLabel.setText(taskInfo.getFile().getName());
        return nameLabel;
      }
      case 1: {
        sizeLabel.setText(NumberFormat.getInstance().format(taskInfo.getSize()));
        return sizeLabel;
      }
      case 2: {
        mediaIdLabel.setText(taskInfo.getMediaId());
        return mediaIdLabel;
      }
      case 3: {
        statusLabel.setText(taskInfo.getStatus().getText());
        switch (taskInfo.getStatus()) {
          case WAITING:
            statusLabel.setBackground(Color.lightGray);
            break;
          case RUNNING:
            statusLabel.setBackground(Color.orange);
            break;
          case FINISHED:
            statusLabel.setBackground(Color.green);
            break;
          case FAILED:
            statusLabel.setBackground(Color.red);
            break;
        }
        return statusLabel;
      }
      case 4: {
        progressBar.setValue(taskInfo.getPctComplete());
        return progressBar;
      }
      case 5: {
        mbpsLabel.setText(""+taskInfo.getMbps());
        return mbpsLabel;
      }
      case 6: {
        creationTimeLabel.setText(taskInfo.getCreationTime());
        return creationTimeLabel;
      }
      case 7: {
        int gap = (int) (taskInfo.secondsElapsed()/1000L);
        int hrs = gap/60;
        int min = hrs%60;
        int sec = gap%60;
        hrs = hrs / 60;
        secLabel.setText(String.format("%02dh:%02dm:%02ds", hrs, min, sec));
        return secLabel;
      }
      case 8: {
        stitchLabel.setText(taskInfo.getStitchStatus().name());
        switch (taskInfo.getStitchStatus()) {
          case NA:
            stitchLabel.setBackground(Color.gray);
            break;
          case PENDING:
            stitchLabel.setBackground(Color.lightGray);
            break;
          case PREPARING:
            stitchLabel.setBackground(Color.yellow);
            break;
          case STARTED:
            stitchLabel.setBackground(Color.orange);
            break;
          case FINISHED:
            stitchLabel.setBackground(Color.green);
            break;
          case FAILED:
            stitchLabel.setBackground(Color.red);
            break;
        }
        return stitchLabel;
      }
      case 9:{
        TaskStatus currStatus = taskInfo.getStatus();
        if(currStatus==TaskStatus.WAITING || currStatus==TaskStatus.QUEUED || currStatus==TaskStatus.RUNNING) {
          return taskInfo.isCancelling() ? cancellingLabel : cancelButton;
        }
        return blankLabel;
      }
    }
    return null;
  }
}
