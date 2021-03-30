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

import com.google.api.client.util.Strings;
import com.google.ce.media.contentuploader.config.AuthConfig;
import com.google.ce.media.contentuploader.config.EnvConfig;
import com.google.ce.media.contentuploader.exec.CompositeUploadTask;
import com.google.ce.media.contentuploader.exec.UploadTask;
import com.google.ce.media.contentuploader.exec.UploadTaskPool;
import com.google.ce.media.contentuploader.message.Destination;
import com.google.ce.media.contentuploader.message.TaskInfo;
import com.google.ce.media.contentuploader.message.TaskInfoMessage;
import com.google.ce.media.contentuploader.message.TaskStatus;
import com.google.ce.media.contentuploader.utils.UIUtils;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import org.joda.time.DateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created in gcs-uploader on 2020-01-08.
 */
@Component
public class UiMasterController implements AuthConfig.AuthConfigListener, ApplicationListener<TaskInfoMessage> {

  private final AuthConfig authConfig;
  private final EnvConfig envConfig;
  private final UploadTaskPool uploadTaskPool;
  private final ApplicationEventPublisher applicationEventPublisher;

  private Timer refreshTimer;


  private JFrame appFrame;
  private JPanel appPanel;
  private JPanel toolbar;
  private JPanel commandToolbar;
  private JPanel destinationToolbar;
//  private JList taskList;
  private JTable taskTable;

  private JLabel tokenExpLabel;

  private FileTaskTableModel taskTableModel;
  private FileTaskTableRenderer taskTableRenderer;
  private FileTaskCellEditor taskTableCellEditor;
  private FileTaskCancelColumn taskCancelColumn;

  private JComboBox<Destination> destinationSelector;
  private Destination selectedDestination;

  private JSplitPane homeContentPane;
  private JTextField fillMediaIdField;

  public UiMasterController(AuthConfig authConfig,
      EnvConfig envConfig,
      UploadTaskPool uploadTaskPool,
      ApplicationEventPublisher applicationEventPublisher) {
    this.authConfig = authConfig;
    this.envConfig = envConfig;
    this.uploadTaskPool = uploadTaskPool;
    this.applicationEventPublisher = applicationEventPublisher;
    this.taskTableModel = new FileTaskTableModel(this.uploadTaskPool, this.authConfig, applicationEventPublisher);
    this.taskTableRenderer = new FileTaskTableRenderer(this.taskTableModel);
    this.taskTableCellEditor = new FileTaskCellEditor(this.taskTableModel);
    this.taskCancelColumn = new FileTaskCancelColumn();
  }


  @PostConstruct
  public void init() {
    appPanel = new JPanel(new BorderLayout());

    toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    toolbar.setBackground(Color.WHITE);
    JPanel northToolbarHolder = new JPanel(new BorderLayout());
    northToolbarHolder.setBackground(Color.WHITE);
    northToolbarHolder.add(toolbar, BorderLayout.EAST);

    try {
      Image cloudImg = ImageIO.read(new ClassPathResource("images/Google_Cloud_Logo.png").getInputStream());
      cloudImg = cloudImg.getScaledInstance(-1, 32, Image.SCALE_SMOOTH);
      ImageIcon cloudIcon = new ImageIcon(cloudImg);
      JLabel cloudIconLabel = new JLabel(cloudIcon);
      cloudIconLabel.setBorder(new EmptyBorder(2, 10, 2, 0));
      northToolbarHolder.add(cloudIconLabel, BorderLayout.WEST);
    } catch (IOException e) {
      e.printStackTrace();
    }

    appPanel.add(northToolbarHolder, BorderLayout.NORTH);

    JPanel bottomToolbar = new JPanel(new BorderLayout());
    bottomToolbar.setBackground(Color.WHITE);
    commandToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    commandToolbar.setBackground(Color.WHITE);
    bottomToolbar.add(commandToolbar, BorderLayout.EAST);

    destinationToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    destinationToolbar.setBackground(Color.WHITE);
    bottomToolbar.add(destinationToolbar, BorderLayout.WEST);

    appPanel.add(bottomToolbar, BorderLayout.SOUTH);

    TransferHandler transferHandler = new TransferHandler(null){
      @Override
      public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
      }

      @Override
      public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
          return false;
        }
        Transferable transferable = support.getTransferable();

        try {
          java.util.List<File> fileList =
                  (java.util.List<File>)transferable.getTransferData(DataFlavor.javaFileListFlavor);

          for (File file : fileList) {
            taskTableModel.addTaskInfo(new TaskInfo(file, uploadTaskPool, authConfig));
          }
        } catch (UnsupportedFlavorException e) {
          return false;
        } catch (IOException e) {
          return false;
        }

        return true;
      }
    };


//    taskList = new JList(taskListModel);
//    taskList.setCellRenderer(new FileTaskCellEditor());

    taskTable = new JTable(taskTableModel) {
      @Override
      public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
        return taskTableRenderer;
      }
    };

    taskTable.getColumnModel().getColumn(2).setCellEditor(taskTableCellEditor);
    taskTable.getColumnModel().getColumn(9).setCellEditor(taskCancelColumn);


    JScrollPane scrollPane = new JScrollPane(taskTable,
                                             JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                             JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

    appPanel.add(scrollPane, BorderLayout.CENTER);

    appFrame = new JFrame("Media Content Uploader");
/*
    homeContentPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                     appPanel,
                                     scrollPane);
    homeContentPane.setDividerLocation(36);
*/
    appFrame.setContentPane(/*homeContentPane*/ appPanel);

    appFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    appFrame.setTransferHandler(transferHandler);
    appFrame.setSize(1024, 768);
    appFrame.setIconImage(UIUtils.getApplicationIcon(48).getImage());
    appFrame.setVisible(true);

    refreshToolbar();

  }

  public void validateAuthorization() {
    if (authConfig.getAuthInfo().getAuthorizedDestinations().size() > 0) {
      refreshToolbar();
    }
    else {
      SwingUtilities.invokeLater(()->{
                                   JOptionPane.showMessageDialog(appFrame,
                                                                 "One or more destinations are unauthorized",
                                                                 "Error with Authorization",
                                                                 JOptionPane.ERROR_MESSAGE);
                                 }
                                );
    }

  }

  public void refreshToolbar() {
    SwingUtilities.invokeLater(()->{
      toolbar.removeAll();
      commandToolbar.removeAll();
      destinationToolbar.removeAll();
      if(authConfig.getAuthInfo() == null) {
        JButton button =
                buildButton("Please Login",
                            "images/account_circle-black-48dp/2x/baseline_account_circle_black_48dp.png");
        button.addActionListener(e -> {
          if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
              Desktop.getDesktop().browse(URI.create("http://localhost:8080"));
            } catch (IOException ex) {
              ex.printStackTrace();
            }
          }
        });
        toolbar.add(button);
        buildClearButton();
      }
      else {
        buildDestinationSelector();
        buildClearButton();
        buildUploadButton();
        buildTokenButtons();

        refreshTimer = new Timer(1000, new RefreshLabelUpdater());
        refreshTimer.start();

        startUpdates();
      }
      appFrame.revalidate();
    });

  }

  private void buildDestinationSelector() {
    JLabel label = new JLabel("Destination: ");
    destinationSelector = new JComboBox<>(authConfig.getAuthInfo().getAuthorizedDestinations().toArray(new Destination[0]));
    destinationSelector.addActionListener(e->{
      selectedDestination = (Destination) destinationSelector.getSelectedItem();
    });
    destinationToolbar.add(label);
    destinationToolbar.add(destinationSelector);
    destinationSelector.setSelectedIndex(0);

    JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
    separator.setForeground(Color.lightGray);
    destinationToolbar.add(separator);

    JLabel fillMediaIdLabel = new JLabel("Fill Media Id: ");
    fillMediaIdField = new JTextField(12);
    JButton fillMediaIdButton = buildButton("", "images/check-black-48dp/2x/baseline_check_black_48dp.png");
    fillMediaIdButton.setToolTipText("Set This Media Id on All Items");
    fillMediaIdButton.addActionListener(e -> {
      String mediaId = fillMediaIdField.getText();
      taskTableModel.getTaskList().forEach(taskInfo -> {
        if(taskInfo.getStatus() == TaskStatus.WAITING) {
          taskInfo.setMediaId(mediaId);
        }
      });
      taskTableModel.fireTableDataChanged();
    });
    destinationToolbar.add(fillMediaIdLabel);
    destinationToolbar.add(fillMediaIdField);
    destinationToolbar.add(fillMediaIdButton);
  }

  private boolean isMediaIdEmpty(TaskInfo taskInfo) {
    if(envConfig.getRequireMediaId()) {
      return Strings.isNullOrEmpty(taskInfo.getMediaId());
    }
    else {
      return false;
    }
  }

  private void buildUploadButton() {
    JButton uploadButton =
            buildButton("Do Upload",
                        "images/cloud_upload-black-48dp/2x/baseline_cloud_upload_black_48dp.png");
    uploadButton.addActionListener(e -> {
      try {
        List<TaskInfo> taskInfoList = taskTableModel.getTaskList();
        int mediaIdEmptyCount = 0;
        for (TaskInfo taskInfo : taskInfoList) {
          boolean isEmpty = false;
          if(taskInfo.getStatus() != TaskStatus.WAITING || (isEmpty = isMediaIdEmpty(taskInfo))) {
            if(isEmpty) {
              mediaIdEmptyCount++;
            }
            continue;
          }
          DateTime now = DateTime.now();
          Map<String,String> metadata = new HashMap<>();
          String paddedMonth = String.format("%02d", now.getMonthOfYear());
          String paddedDate = String.format("%02d", now.getDayOfMonth());
          String blobName =
                  selectedDestination.getGcsFolder()
                          +"/"+now.getYear()
                          +"/"+paddedMonth
                          +"/"+paddedDate
                          +"/"+taskInfo.getFile().getName();
          metadata.put("x-uploader-media-id", taskInfo.getMediaId());
          metadata.put("x-uploader-email", authConfig.getAuthInfo().getEmail());
          metadata.put("x-uploader-guid", taskInfo.getId());
          BlobInfo blobInfo = Blob.newBuilder(selectedDestination.getGcsBucket(),
                                              blobName).setMetadata(metadata).build();
          taskInfo.setBlobInfo(blobInfo);
          taskInfo.setStatus(TaskStatus.QUEUED);
          if (taskInfo.isComposite()) {
            CompositeUploadTask compositeUploadTask = new CompositeUploadTask(taskInfo,
                                                                              uploadTaskPool,
                                                                              applicationEventPublisher);
            uploadTaskPool.enqueue(compositeUploadTask);
          }
          else {
            UploadTask uploadTask = new UploadTask(taskInfo,
                                                   applicationEventPublisher);
            uploadTaskPool.enqueue(uploadTask);
          }
        }
        if (mediaIdEmptyCount > 0) {
          JOptionPane.showMessageDialog(appFrame, "At least 1 file doesn't have a Media ID specified; Please enter missing Media IDs and retry.",
                                        "Missing Media IDs",
                                        JOptionPane.WARNING_MESSAGE);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    });
    commandToolbar.add(uploadButton);
  }

  private void buildClearButton() {
    JButton clearButton =
            buildButton("Clear List",
                        "images/clear_all-black-48dp/2x/baseline_clear_all_black_48dp.png");
    clearButton.addActionListener(e->{
      taskTableModel.clearTaskList();
    });
    commandToolbar.add(clearButton);
  }

  private void buildTokenButtons() {
    tokenExpLabel = new JLabel();
    toolbar.add(tokenExpLabel);

    JButton refreshCredButton = buildButton("","images/refresh-black-48dp/2x/baseline_refresh_black_48dp.png");
    refreshCredButton.addActionListener(e -> {
      authConfig.updateToken(this);
    });
    refreshCredButton.setToolTipText("Refresh Credentials");
    toolbar.add(refreshCredButton);

    toolbar.add(Box.createHorizontalStrut(12));
    toolbar.add(new JLabel(authConfig.getAuthInfo().getName()));

    try {
      Image cloudImg = ImageIO.read(new URL(authConfig.getAuthInfo().getPictureUrl()));
      cloudImg = cloudImg.getScaledInstance(-1, 32, Image.SCALE_SMOOTH);
      ImageIcon cloudIcon = new ImageIcon(cloudImg);
      JLabel headshotLabel = new JLabel(cloudIcon);
      headshotLabel.setBorder(new EmptyBorder(2, 10, 2, 10));
      toolbar.add(headshotLabel);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private JButton buildButton(String text, String resource) {
    JButton button = new JButton(text);
    button.setBackground(Color.WHITE);
    try {
      Image img = ImageIO.read(new ClassPathResource(resource).getInputStream());
      img = img.getScaledInstance(-1, 24, Image.SCALE_SMOOTH);
      ImageIcon imageIcon = new ImageIcon(img);
      button.setIcon(imageIcon);
    } catch (IOException e) {
      e.printStackTrace();
    }
    button.setMargin(new Insets(0, 5, 0, 5));
    return button;
  }

  private ScheduledExecutorService scheduledExecutorService;
  public void startUpdates() {
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(()->{
                                                   taskTableModel.fireTableDataChanged();
                                                 },
                                                 1000,
                                                 1000,
                                                 TimeUnit.MILLISECONDS);
  }

  public void stopUpdates() {
    scheduledExecutorService.shutdown();
  }

/*********** auth config listener **********/
  @Override
  public void authInfoUpdated() {

  }

  @Override
  public void authInfoError(Exception e) {
    SwingUtilities.invokeLater(()->{
      tokenExpLabel.setText("Error! Please Restart App");
    });
  }

  /*********** Application Event listener **********/

  @Override
  public void onApplicationEvent(TaskInfoMessage event) {
    if(event.getType() == TaskInfoMessage.Type.CANCEL) {
      System.out.println("-->> RECEIVED CANCEL EVENT in master controller");
      TaskInfo taskInfo = event.getTaskInfo();
      if(taskInfo.isCancelling()) {
        //only remove tasklets for composite uploads with non null tasklets
        if (taskInfo.isComposite() && taskInfo.getTasklets() != null) {
          List<BlobId> blobIds = new LinkedList<>();
          taskInfo.getTasklets().stream().forEach(t->blobIds.add(t.getBlobInfo().getBlobId()));
          taskInfo.getAuthConfig().getStorage().delete(blobIds.toArray(new BlobId[blobIds.size()]));
          System.out.println("-->> DELETED blobs from storage: " + blobIds.size());
        }
      }
      System.out.println("++>> got CANCEL event for " + event.getTaskInfo().getFile());
      taskTableModel.remove(event.getTaskInfo());
    }
  }

  private class RefreshLabelUpdater implements ActionListener {


    @Override
    public void actionPerformed(ActionEvent e) {
      Long now = System.currentTimeMillis() / 1000;
      Long exp = authConfig.getAuthInfo().getExpirationSeconds();
      if(now < exp) {
        int gap = (int) (exp - now);

        int hrs = gap/60;
        int min = hrs%60;
        int sec = gap%60;
        hrs = hrs / 60;

        tokenExpLabel.setText(String.format("Credentials Expire in %02dh:%02dm:%02ds", hrs, min, sec));
        tokenExpLabel.setForeground(Color.black);
        if((exp - now) < AuthConfig.REFRESH_THRESHOLD_SECS) {
          synchronized (AuthConfig.REFRESH_LOCK) {
            authConfig.updateToken(null);
          }
        }
      }
      else {
        tokenExpLabel.setText("Credentials Expired! Please Refresh!");
        tokenExpLabel.setForeground(Color.red);
      }
    }
  }
}
