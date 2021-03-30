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

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

/**
 * Created in gcs-uploader on 2020-01-10.
 */
public class FileTaskCellEditor extends AbstractCellEditor implements TableCellEditor {

  private final JTextField textField = new JTextField();
  private final FileTaskTableModel taskTableModel;


  public FileTaskCellEditor(FileTaskTableModel taskTableModel) {
    this.taskTableModel = taskTableModel;
    this.textField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
  }


  @Override
  public Component getTableCellEditorComponent(JTable table,
                                               Object value,
                                               boolean isSelected,
                                               int row,
                                               int column) {
    TaskInfo taskInfo = (TaskInfo) value;
    if(column == 2) {
      textField.setText(taskInfo.getMediaId());
      return textField;
    }
    return null;
  }

  @Override
  public Object getCellEditorValue() {
    return textField.getText();
  }
}
