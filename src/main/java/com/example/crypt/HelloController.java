package com.example.crypt;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class HelloController {
    @FXML private ComboBox<String> diskBox;
    @FXML private Slider sizeSlider;
    @FXML private Label sizeValue;
    @FXML private TextField nameField;
    @FXML private Button nextBtn;

    private Stage stage;

    @FXML
    private void initialize() {
        // Заполняем список дисков
        List<String> disks = getAvailableDisks();
        diskBox.getItems().addAll(disks);
        diskBox.getSelectionModel().selectFirst();

        // Обновляем значение размера
        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            sizeValue.setText(String.format("%d", newVal.intValue()));
            validateFields();
        });

        // Валидация полей
        nameField.textProperty().addListener((obs, oldVal, newVal) -> validateFields());
    }

    private List<String> getAvailableDisks() {
        List<String> disks = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("lsblk -o NAME,SIZE,TYPE -n -l");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("disk")) {
                    String[] parts = line.split("\\s+");
                    disks.add("/dev/" + parts[0]);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при получении списка дисков: " + e.getMessage());
        }
        return disks;
    }

    private void validateFields() {
        boolean valid = !nameField.getText().isEmpty()
                && diskBox.getValue() != null;
        nextBtn.setDisable(!valid);
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }

    @FXML
    private void handleNext() {
        String disk = diskBox.getValue();
        int size = (int) sizeSlider.getValue();
        String name = nameField.getText();

        // Формируем путь к контейнеру
        String containerPath = disk + File.separator + name + ".container";

        // Переходим к следующему окну
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/encryption_settings.fxml"));
            Parent root = loader.load();

            EncryptionSettingsController controller = loader.getController();
            controller.setContainerData(containerPath, size, name);

            Stage stage = (Stage) nextBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}