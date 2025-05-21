package com.example.crypt;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

public class EditController {
    @FXML private TableView<Partition> partitionsTable;
    @FXML private TableColumn<Partition, String> nameColumn;
    @FXML private TableColumn<Partition, String> sizeColumn;
    @FXML private TableColumn<Partition, String> algorithmColumn;
    @FXML private TableColumn<Partition, String> creationTimeColumn;
    @FXML private TableColumn<Partition, String> encryptionMethodColumn;
    @FXML private TableColumn<Partition, String> mountColumn;
    @FXML private TableColumn<Partition, String> deleteColumn;
    @FXML private TableColumn<Partition, String> copyColumn;
    private final ObservableList<Partition> partitions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Инициализация колонок
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        algorithmColumn.setCellValueFactory(new PropertyValueFactory<>("algorithm"));
        creationTimeColumn.setCellValueFactory(new PropertyValueFactory<>("creationTime"));
        encryptionMethodColumn.setCellValueFactory(new PropertyValueFactory<>("encryptionMethod"));
        mountColumn.setCellValueFactory(new PropertyValueFactory<>("mountButton"));
        deleteColumn.setCellValueFactory(new PropertyValueFactory<>("deleteButton"));
        copyColumn.setCellValueFactory(new PropertyValueFactory<>("copyButton"));

        // Загрузка списка контейнеров
        refreshPartitionsList();

        partitions.forEach(partition -> {
            partition.getMountButton().setOnAction(e -> handleMountAction(partition));
            partition.getDeleteButton().setOnAction(e -> handleDeleteAction(partition));
            partition.getCopyButton().setOnAction(e -> handleCopyAction(partition));
        });
    }

    private void checkMountedStatus() {
        for (Partition partition : partitions) {
            if (isMounted(partition)) {
                partition.setIsMounted(true);
                partition.getMountButton().setText("Размонтировать");
            } else {
                partition.setIsMounted(false);
                partition.getMountButton().setText("Монтировать");
            }
        }
    }
    private boolean isMounted(Partition partition) {
        String mountPoint = "/home/" + EncryptionManager.getUsername() + "/.mountContainers/" + partition.getName();
        try {
            Process process = Runtime.getRuntime().exec("mount");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(mountPoint)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при проверке состояния монтирования: " + e.getMessage());
        }
        return false;
    }

    private void handleMountAction(Partition partition) {
        try {
            String mountPoint = "/home/" + EncryptionManager.getUsername() + "/.mountContainers/" + partition.getName();
            
            if (partition.isIsMounted()) {
                // Размонтирование
                EncryptionManager.unmountContainer(partition.getName(), mountPoint);

                partition.setIsMounted(false);
                partition.getMountButton().setText("Монтировать");
                showSuccessAlert("Диск успешно размонтирован");
            } else {
                // Монтирование
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Монтирование диска");
                dialog.setHeaderText("Введите пароль для диска " + partition.getName());
                dialog.setContentText("Пароль:");

                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    String password = result.get();
                    new File(mountPoint).mkdirs();

                    // Замеряем время начала монтирования
                    long startTime = System.currentTimeMillis();

                    EncryptionManager.mountContainer(partition.getPath(), partition.getName(), password, mountPoint);

                    // Замеряем время окончания монтирования
                    long endTime = System.currentTimeMillis();
                    long mountTime = endTime - startTime;

                    partition.setIsMounted(true);
                    partition.getMountButton().setText("Размонтировать");
                    showSuccessAlert(String.format("Диск успешно смонтирован в %s\nВремя монтирования: %.2f секунд", 
                        mountPoint, mountTime / 1000.0));
                }
            }
        } catch (IOException e) {
            showErrorAlert("Ошибка при работе с диском: " + e.getMessage());
        }
    }

    private void handleDeleteAction(Partition partition) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Вы уверены, что хотите удалить диск " + partition.getName() + "?");
        alert.setContentText("Это действие нельзя отменить.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String mountPoint = "/home/" + EncryptionManager.getUsername() + "/.mountContainers/" + partition.getName();
                
                if (partition.isIsMounted()) {
                    EncryptionManager.unmountContainer(partition.getName(), mountPoint);
                }

                // Удаляем файл контейнера
                File containerFile = new File(partition.getPath());
                if (!containerFile.delete()) {
                    throw new IOException("Не удалось удалить файл контейнера");
                }

                partitions.remove(partition);
                showSuccessAlert("Диск успешно удален");
            } catch (IOException e) {
                showErrorAlert("Ошибка при удалении диска: " + e.getMessage());
            }
        }
    }

    private void handleCopyAction(Partition partition) {
        if (!partition.isIsMounted()) {
            showErrorAlert("Сначала необходимо смонтировать диск");
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите директорию для копирования");
        File selectedDirectory = directoryChooser.showDialog(partitionsTable.getScene().getWindow());

        if (selectedDirectory != null) {
            try {
                String mountPoint = "/home/" + EncryptionManager.getUsername() + "/.mountContainers/" + partition.getName();
                
                // Замеряем время начала копирования
                long startTime = System.currentTimeMillis();

                EncryptionManager.copyDataToContainer(selectedDirectory.getAbsolutePath(), partition.getName(), mountPoint);

                // Замеряем время окончания копирования
                long endTime = System.currentTimeMillis();
                long copyTime = endTime - startTime;

                showSuccessAlert(String.format("Данные успешно скопированы\nВремя копирования: %.2f секунд", 
                    copyTime / 1000.0));
            } catch (IOException e) {
                showErrorAlert("Ошибка при копировании данных: " + e.getMessage());
            }
        }
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Успех");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleApply() {
        closeWindow();
    }

    @FXML
    private void handleClose() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) partitionsTable.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void refreshPartitionsList() {
        partitions.clear();
        ObservableList<Partition> containers = EncryptionManager.getContainersList();
        System.out.println("Количество найденных контейнеров: " + containers.size()); // Логирование
        partitions.addAll(containers);
        partitionsTable.setItems(partitions);
    }
}