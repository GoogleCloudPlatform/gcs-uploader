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

import com.google.ce.media.contentuploader.config.AuthConfig;
import com.google.ce.media.contentuploader.exec.MonitorTask;
import com.google.ce.media.contentuploader.exec.UploadTaskPool;
import com.google.ce.media.contentuploader.message.TaskInfo;
import com.google.ce.media.contentuploader.message.TaskInfoMessage;
import com.google.ce.media.contentuploader.message.TaskStatus;
import com.google.ce.media.contentuploader.utils.UIUtils;
import org.springframework.context.ApplicationEventPublisher;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created in gcs-uploader on 2020-01-10.
 */
public class FileTaskTableModel extends AbstractTableModel {

  private final List<TaskInfo> taskList = new ArrayList<>();
  private final Set<TaskInfo> taskSet = new HashSet<>();
  private ScheduledExecutorService scheduledExecutorService;
  private final UploadTaskPool uploadTaskPool;
  private final AuthConfig authConfig;
  private final ApplicationEventPublisher applicationEventPublisher;

  private final String[] columns = {
          "File Name",          // col 0
          "Size (Bytes)",       // col 1
          "Media Id",           // col 2
          "Status",             // col 3
          "Progress",           // col 4
          "mbps",               // col 5
          "Creation Time",      // col 6
          "Total Time",         // col 7
          "Validation"          // col 8
          ,"Cancel"             // col 9
  };

  public FileTaskTableModel(UploadTaskPool uploadTaskPool, AuthConfig authConfig, ApplicationEventPublisher applicationEventPublisher) {
    this.uploadTaskPool = uploadTaskPool;
    this.authConfig = authConfig;
    this.applicationEventPublisher = applicationEventPublisher;

    this.uploadTaskPool.scheduleAtFixedRate(new MonitorTask(taskList, this.uploadTaskPool), 300000L);
  }


  @Override
  public int getRowCount() {
    return taskList.size();
  }

  @Override
  public int getColumnCount() {
    return columns.length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    return taskList.get(rowIndex);
/*
    TaskInfo taskInfo = taskList.get(rowIndex);
    switch (columnIndex) {
      case 0: return taskInfo.getId();
      case 1: return taskInfo.getSize();
      case 2: return taskInfo.getStatus().getText();
      case 3: return taskInfo.getPctComplete();
      case 4: return taskInfo.getMbps();
    }
    return "";
*/
  };

  @Override
  public String getColumnName(int column) {
    return columns[column];
  }

  public void addTaskInfo(TaskInfo taskInfo) {
    if(!taskSet.contains(taskInfo)) {
      deepAddTaskInfo(taskInfo);
    }
    fireTableDataChanged();
  }

  private void deepAddTaskInfo(TaskInfo taskInfo) {
    taskSet.add(taskInfo);
    if(taskInfo.isDirectory()) {
      File[] files = taskInfo.getFile().listFiles();
      if (files != null) {
        for (File file : files) {
          deepAddTaskInfo(new TaskInfo(file, uploadTaskPool, authConfig));
        }
      }
    }
    else {
      taskList.add(taskInfo);
    }
  }

  public void clearTaskList() {
    List<TaskInfo> toRemove = new ArrayList<>();
    for (TaskInfo taskInfo : taskList) {
      switch (taskInfo.getStatus()) {
        case WAITING:
        case FINISHED:
        case FAILED:
          toRemove.add(taskInfo);
      }
    }
    taskList.removeAll(toRemove);
    taskSet.removeAll(toRemove);
    fireTableDataChanged();
  }

  public void remove(TaskInfo taskInfo) {
    taskList.remove(taskInfo);
    taskSet.remove(taskInfo);
    fireTableDataChanged();
  }

  public List<TaskInfo> getTaskList() {
    return taskList;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    TaskInfo taskInfo = taskList.get(rowIndex);
    if(columnIndex == 2) { //media id column is editable
      return taskInfo.getStatus() == TaskStatus.WAITING;
    }
    if(columnIndex == 9) {
      return !taskInfo.isCancelling();
    }
    return false; //other columns are not editable
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    final TaskInfo taskInfo = taskList.get(rowIndex);
    switch (columnIndex) {
      case 2: {
        taskInfo.setMediaId((String) aValue);
        break;
      }
      case 9: {
        if("Cancel".equals(aValue)) {
          System.out.println("-->> click for cancelling " + taskInfo.getFile());
          TaskStatus currStatus = taskInfo.getStatus();
          if(!(currStatus==TaskStatus.WAITING || currStatus==TaskStatus.QUEUED || currStatus==TaskStatus.RUNNING)) {
            JOptionPane.showMessageDialog(JFrame.getFrames()[0],
                                                        "Cannot Cancel Finished Uploads",
                                                        "Not Applicable",
                                                        JOptionPane.INFORMATION_MESSAGE,
                                                        UIUtils.getApplicationIcon(48));
            return;
          }
          int confirm = JOptionPane.showConfirmDialog(JFrame.getFrames()[0],
                                                      "Cancelling will remove this upload. Are you sure?",
                                                      "Confirm",
                                                      JOptionPane.YES_NO_OPTION,
                                                      JOptionPane.QUESTION_MESSAGE,
                                                      UIUtils.getApplicationIcon(48));
          if(confirm != JOptionPane.YES_OPTION) {
            return;
          }
          taskInfo.setCancelling(true);
          if(taskInfo.getStatus() == TaskStatus.WAITING) {
            remove(taskInfo);
          }
          else if(taskInfo.getStatus()==TaskStatus.QUEUED) {
            remove(taskInfo);
          }
          else {
            applicationEventPublisher.publishEvent(new TaskInfoMessage(taskInfo, TaskInfoMessage.Type.CANCEL));
          }
        }
      }
    }
  }
}
