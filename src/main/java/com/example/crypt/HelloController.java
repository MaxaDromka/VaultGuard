package com.example.crypt;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class HelloController {
    @FXML private TextField directoryField; // Поле для отображения выбранной папки
    @FXML private Slider sizeSlider;
    @FXML private TextField sizeField;
    @FXML private TextField nameField;
    @FXML private Button nextBtn;

    private Stage stage;

    @FXML
    private void initialize() {
        // Настройка ползунка и текстового поля
        sizeSlider.setMin(100);
        sizeSlider.setMax(100000);
        sizeSlider.setValue(1000);

        // Синхронизация ползунка и текстового поля
        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            sizeField.setText(String.format("%.0f", newVal));
        });

        sizeField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double value = Double.parseDouble(newVal);
                if (value >= sizeSlider.getMin() && value <= sizeSlider.getMax()) {
                    sizeSlider.setValue(value);
                }
            } catch (NumberFormatException e) {
                // Игнорируем некорректный ввод
            }
        });

        // Валидация полей
        directoryField.textProperty().addListener((obs, oldVal, newVal) -> validateFields());
        nameField.textProperty().addListener((obs, oldVal, newVal) -> validateFields());
    }

    @FXML
    private void handleChooseDirectory(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите папку для зашифрованного диска");
        File selectedDirectory = directoryChooser.showDialog(((Button) event.getSource()).getScene().getWindow());

        if (selectedDirectory != null) {
            directoryField.setText(selectedDirectory.getAbsolutePath());
            validateFields(); // Проверяем, можно ли активировать кнопку "Далее"
        }
    }

    private void validateFields() {
        boolean valid = !nameField.getText().isEmpty()
                && !directoryField.getText().isEmpty();
        nextBtn.setDisable(!valid);
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }

    @FXML
    private void handleNext() {
        String selectedDirectory = directoryField.getText();
        int size = (int) sizeSlider.getValue();
        String name = nameField.getText();

        // Формируем путь к контейнеру
        String containerPath = selectedDirectory + File.separator + "." + name;

        // Проверяем, не существует ли уже контейнер с таким именем
        File containerFile = new File(containerPath);
        if (containerFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Предупреждение");
            alert.setHeaderText(null);
            alert.setContentText("Контейнер с таким именем уже существует. Выберите другое имя.");
            alert.showAndWait();
            return;
        }

        // Переходим к следующему окну
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/encryption_settings.fxml"));
            Parent root = loader.load();

            EncryptionSettingsController controller = loader.getController();
            controller.setContainerData(containerPath, size, name);

            Stage stage = (Stage) nextBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText("Не удалось открыть окно настроек шифрования: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}