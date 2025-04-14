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

import java.io.IOException;
import java.util.Optional;
import java.io.File;

public class EditController {
    @FXML private TableView<Partition> partitionsTable;
    private final ObservableList<Partition> partitions = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Получаем список реальных контейнеров
        partitions.addAll(EncryptionManager.getContainersList());
        partitionsTable.setItems(partitions);

        // Добавляем обработчики для каждой кнопки
        partitions.forEach(partition -> {
            // Обработчик кнопки монтирования
            partition.getMountButton().setOnAction(e -> handleMountAction(partition));
            
            // Обработчик кнопки удаления
            partition.getDeleteButton().setOnAction(e -> handleDeleteAction(partition));
        });
    }

    private void handleMountAction(Partition partition) {
        try {
            if (!partition.isIsMounted()) {
                // Запрос пароля для монтирования
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Монтирование контейнера");
                dialog.setHeaderText("Введите пароль для контейнера " + partition.getName());
                dialog.setContentText("Пароль:");

                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    String password = result.get();
                    // Создаем точку монтирования
                    String mountPoint = System.getProperty("user.home") + "/mnt/" + partition.getName();
                    new File(mountPoint).mkdirs();
                    
                    // Монтируем контейнер
                    EncryptionManager.mountContainer(partition.getPath(), partition.getName(), password, mountPoint);
                    partition.getMountButton().setText("Размонтировать");
                    showSuccessAlert("Контейнер успешно смонтирован в " + mountPoint);
                }
            } else {
                // Размонтирование
                String mountPoint = System.getProperty("user.home") + "/mnt/" + partition.getName();
                EncryptionManager.unmountContainer(partition.getName(), mountPoint);
                partition.getMountButton().setText("Монтировать");
                showSuccessAlert("Контейнер успешно размонтирован");
            }
        } catch (IOException e) {
            showErrorAlert("Ошибка при работе с контейнером: " + e.getMessage());
        }
    }

    private void handleDeleteAction(Partition partition) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Вы уверены, что хотите удалить контейнер " + partition.getName() + "?");
        alert.setContentText("Это действие нельзя отменить.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (partition.isIsMounted()) {
                    // Сначала размонтируем
                    String mountPoint = System.getProperty("user.home") + "/mnt/" + partition.getName();
                    EncryptionManager.unmountContainer(partition.getName(), mountPoint);
                }
                // Удаляем файл контейнера
                new File(partition.getPath()).delete();
                partitions.remove(partition);
                showSuccessAlert("Контейнер успешно удален");
            } catch (IOException e) {
                showErrorAlert("Ошибка при удалении контейнера: " + e.getMessage());
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
}
