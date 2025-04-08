package com.example.crypt;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class AutomationController {
    private static final Logger logger = Logger.getLogger(AutomationController.class.getName());

    @FXML private ComboBox<String> containerBox;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox autoStartCheck;
    @FXML private DatePicker scheduleDatePicker;
    @FXML private ComboBox<Integer> scheduleHourBox;
    @FXML private ComboBox<Integer> scheduleMinuteBox;
    @FXML private CheckBox usbMountCheck;
    @FXML private ComboBox<String> usbDeviceBox;
    @FXML private Button saveAutomationButton;
    
    @FXML private ComboBox<String> backupContainerBox;
    @FXML private DatePicker backupDatePicker;
    @FXML private ComboBox<Integer> backupHourBox;
    @FXML private ComboBox<Integer> backupMinuteBox;
    @FXML private TableView<BackupEntry> backupTable;
    @FXML private TableColumn<BackupEntry, String> dateColumn;
    @FXML private TableColumn<BackupEntry, String> sizeColumn;
    @FXML private TableColumn<BackupEntry, String> statusColumn;
    @FXML private Button createBackupButton;
    @FXML private Button restoreBackupButton;
    @FXML private Button saveScheduleButton;

    private ObservableList<BackupEntry> backupList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        loadContainers();
        setupBackupTable();
        loadUSBDevices();
        loadBackups();
        
        // Инициализация комбобоксов для времени
        for (int i = 0; i < 24; i++) {
            scheduleHourBox.getItems().add(i);
            backupHourBox.getItems().add(i);
        }
        for (int i = 0; i < 60; i++) {
            scheduleMinuteBox.getItems().add(i);
            backupMinuteBox.getItems().add(i);
        }
    }

    private void loadContainers() {
        try {
            File containersDir = new File(System.getProperty("user.home") + "/containers");
            if (containersDir.exists() && containersDir.isDirectory()) {
                File[] containers = containersDir.listFiles((dir, name) -> name.endsWith(".container"));
                if (containers != null) {
                    for (File container : containers) {
                        containerBox.getItems().add(container.getName());
                        backupContainerBox.getItems().add(container.getName());
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при загрузке контейнеров", e);
        }
    }

    private void setupBackupTable() {
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        sizeColumn.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        backupTable.setItems(backupList);
    }

    private void loadUSBDevices() {
        try {
            // Здесь можно добавить код для получения списка USB-устройств
            // через Java API вместо lsblk
            usbDeviceBox.getItems().addAll("USB_DEVICE_1", "USB_DEVICE_2");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при загрузке USB-устройств", e);
        }
    }

    private void loadBackups() {
        try {
            File backupsDir = new File(System.getProperty("user.home") + "/.crypt/backups");
            if (backupsDir.exists() && backupsDir.isDirectory()) {
                File[] backups = backupsDir.listFiles((dir, name) -> name.endsWith(".backup"));
                if (backups != null) {
                    backupList.clear();
                    for (File backup : backups) {
                        backupList.add(new BackupEntry(
                            backup.getName(),
                            String.valueOf(backup.length() / 1024 / 1024) + " MB",
                            "Готово"
                        ));
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при загрузке резервных копий", e);
        }
    }

    @FXML
    private void handleSaveAutomation() {
        try {
            String containerPath = System.getProperty("user.home") + "/.crypt/containers/" + 
                                 containerBox.getValue();
            String password = passwordField.getText();

            if (autoStartCheck.isSelected()) {
                AutoMountManager.enableAutoMount(containerPath, password);
            }

            if (scheduleDatePicker.getValue() != null && 
                scheduleHourBox.getValue() != null && 
                scheduleMinuteBox.getValue() != null) {
                
                LocalDateTime scheduleTime = LocalDateTime.of(
                    scheduleDatePicker.getValue(),
                    java.time.LocalTime.of(
                        scheduleHourBox.getValue(),
                        scheduleMinuteBox.getValue()
                    )
                );
                AutoMountManager.scheduleMount(containerPath, password, scheduleTime);
            }

            if (usbMountCheck.isSelected() && usbDeviceBox.getValue() != null) {
                AutoMountManager.mountOnUSB(containerPath, password, usbDeviceBox.getValue());
            }

            showAlert(Alert.AlertType.INFORMATION, "Успех", 
                     "Настройки автоматизации сохранены");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при сохранении настроек автоматизации", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                     "Не удалось сохранить настройки автоматизации");
        }
    }

    @FXML
    private void handleCreateBackup() {
        try {
            String containerPath = System.getProperty("user.home") + "/.crypt/containers/" + 
                                 backupContainerBox.getValue();
            
            BackupManager.backupContainer(containerPath);
            loadBackups();
            
            showAlert(Alert.AlertType.INFORMATION, "Успех", 
                     "Резервная копия создана");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при создании резервной копии", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                     "Не удалось создать резервную копию");
        }
    }

    @FXML
    private void handleRestoreBackup() {
        try {
            BackupEntry selectedBackup = backupTable.getSelectionModel().getSelectedItem();
            if (selectedBackup == null) {
                showAlert(Alert.AlertType.WARNING, "Предупреждение", 
                         "Выберите резервную копию для восстановления");
                return;
            }

            String backupPath = System.getProperty("user.home") + "/.crypt/backups/" + 
                              selectedBackup.getDate();
            String containerPath = System.getProperty("user.home") + "/.crypt/containers/" + 
                                 backupContainerBox.getValue();

            BackupManager.restoreContainer(backupPath, containerPath);
            
            showAlert(Alert.AlertType.INFORMATION, "Успех", 
                     "Контейнер восстановлен из резервной копии");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при восстановлении из резервной копии", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                     "Не удалось восстановить контейнер");
        }
    }

    @FXML
    private void handleSaveSchedule() {
        try {
            String containerPath = System.getProperty("user.home") + "/.crypt/containers/" + 
                                 backupContainerBox.getValue();

            if (backupDatePicker.getValue() != null && 
                backupHourBox.getValue() != null && 
                backupMinuteBox.getValue() != null) {
                
                LocalDateTime scheduleTime = LocalDateTime.of(
                    backupDatePicker.getValue(),
                    java.time.LocalTime.of(
                        backupHourBox.getValue(),
                        backupMinuteBox.getValue()
                    )
                );
                BackupManager.scheduleBackup(containerPath, scheduleTime);
            }

            showAlert(Alert.AlertType.INFORMATION, "Успех", 
                     "Расписание резервного копирования сохранено");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при сохранении расписания", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                     "Не удалось сохранить расписание");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static class BackupEntry {
        private final javafx.beans.property.StringProperty date;
        private final javafx.beans.property.StringProperty size;
        private final javafx.beans.property.StringProperty status;

        public BackupEntry(String date, String size, String status) {
            this.date = new javafx.beans.property.SimpleStringProperty(date);
            this.size = new javafx.beans.property.SimpleStringProperty(size);
            this.status = new javafx.beans.property.SimpleStringProperty(status);
        }

        public String getDate() { return date.get(); }
        public void setDate(String value) { date.set(value); }
        public javafx.beans.property.StringProperty dateProperty() { return date; }

        public String getSize() { return size.get(); }
        public void setSize(String value) { size.set(value); }
        public javafx.beans.property.StringProperty sizeProperty() { return size; }

        public String getStatus() { return status.get(); }
        public void setStatus(String value) { status.set(value); }
        public javafx.beans.property.StringProperty statusProperty() { return status; }
    }
} 