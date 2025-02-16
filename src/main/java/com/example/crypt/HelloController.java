package com.example.crypt;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;

public class HelloController {
    @FXML private TextField pathField;
    @FXML private Slider sizeSlider;
    @FXML private Label sizeValue;
    @FXML private Button nextBtn;
    @FXML private TextField nameField;

    private Stage stage;

    @FXML
    private void initialize() {
        if (sizeSlider == null || sizeValue == null) {
            throw new IllegalStateException("FXML elements not properly injected!");
        }

        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            sizeValue.setText(String.format("%d", newVal.intValue()));
            validateFields();
        });

        pathField.textProperty().addListener((obs, oldVal, newVal) -> validateFields());
        nameField.textProperty().addListener((obs, oldVal, newVal) -> validateFields());
    }

    private void validateFields() {
        boolean valid = !pathField.getText().isEmpty()
                && !nameField.getText().isEmpty()
                && sizeSlider.getValue() >= sizeSlider.getMin();
        nextBtn.setDisable(!valid);
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void handleNext() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/encryption_settings.fxml"));
            Parent root = loader.load();
            EncryptionSettingsController controller = loader.getController();
            controller.setContainerData(
                    pathField.getText(),
                    (int) sizeSlider.getValue(),
                    nameField.getText()
            );
            Stage stage = (Stage) nextBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBrowse(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите папку для создания зашифрованного контейнера");

        // Открываем диалоговое окно и получаем выбранную директорию
        File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            pathField.setText(selectedDirectory.getAbsolutePath()); // Устанавливаем путь в текстовое поле
        }
    }
}