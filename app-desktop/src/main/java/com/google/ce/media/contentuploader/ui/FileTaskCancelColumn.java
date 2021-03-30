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
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created in gcs-uploader on 2020-03-17.
 */
public class FileTaskCancelColumn
        extends AbstractCellEditor
        implements TableCellEditor, ActionListener {

  private JButton editButton;

  public FileTaskCancelColumn() {
    editButton = new JButton("Cancel");
    editButton.setFocusPainted( false );
    editButton.addActionListener( this );
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    fireEditingStopped();
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
    TaskInfo taskInfo = (TaskInfo) value;
    TaskStatus currStatus = taskInfo.getStatus();
    if(currStatus==TaskStatus.WAITING || currStatus==TaskStatus.QUEUED || currStatus==TaskStatus.RUNNING) {
      return editButton;
    }
    return null;
  }

  @Override
  public Object getCellEditorValue() {
      return "Cancel";
  }
}
