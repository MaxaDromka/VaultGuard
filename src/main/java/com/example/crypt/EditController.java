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

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class EditController {
    @FXML private TableView<Partition> partitionsTable;
    private final ObservableList<Partition> partitions = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        refreshDiskList();

        partitions.forEach(partition -> {
            partition.getMountButton().setOnAction(e -> handleMountAction(partition));
            partition.getDeleteButton().setOnAction(e -> handleDeleteAction(partition));
        });
    }

    private void handleMountAction(Partition partition) {
        try {
            if (!partition.isIsMounted()) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Монтирование диска");
                dialog.setHeaderText("Введите пароль для диска " + partition.getName());
                dialog.setContentText("Пароль:");

                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    String password = result.get();
                    String mountPoint = System.getProperty("user.home") + "/mnt/" + partition.getName();
                    new File(mountPoint).mkdirs(); // Создаем папку для монтирования
                    EncryptionManager.mountContainer(partition.getPath(), partition.getName(), password, mountPoint);
                    partition.setIsMounted(true);
                    partition.getMountButton().setText("Размонтировать");
                    showSuccessAlert("Диск успешно смонтирован в " + mountPoint);
                }
            } else {
                String mountPoint = System.getProperty("user.home") + "/mnt/" + partition.getName();
                EncryptionManager.unmountContainer(partition.getName(), mountPoint);
                partition.setIsMounted(false);
                partition.getMountButton().setText("Монтировать");
                showSuccessAlert("Диск успешно размонтирован");
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
                if (partition.isIsMounted()) {
                    String mountPoint = System.getProperty("user.home") + "/mnt/" + partition.getName();
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
    private void refreshDiskList() {
        partitions.clear();
        ObservableList<Partition> containers = EncryptionManager.getContainersList();
        System.out.println("Количество найденных контейнеров: " + containers.size()); // Логирование
        partitions.addAll(containers);
        partitionsTable.setItems(partitions);
    }
}