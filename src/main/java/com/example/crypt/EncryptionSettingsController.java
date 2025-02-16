package com.example.crypt;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class EncryptionSettingsController {
    @FXML private ComboBox<String> algorithmBox;
    @FXML private CheckBox usePasswordCheck;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordLabel;
    @FXML private Label confirmLabel;
    @FXML private Button generatePasswordBtn;
    @FXML private Button encryptBtn;

    private String containerPath;
    private int containerSize;
    private String containerName;

    @FXML
    private void initialize() {
        // Заполняем алгоритмы
        algorithmBox.getItems().addAll(
                "AES-256 (XTS)",
                "Serpent (XTS)",
                "Twofish (XTS)",
                "AES-Twofish (XTS)",
                "AES-Twofish-Serpent (XTS)"
        );
        algorithmBox.getSelectionModel().selectFirst();

        // Привязка видимости элементов к чекбоксу
        usePasswordCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boolean visible = newVal != null && newVal;
            passwordLabel.setVisible(visible);
            passwordField.setVisible(visible);
            confirmLabel.setVisible(visible);
            confirmPasswordField.setVisible(visible);
            generatePasswordBtn.setVisible(visible);
            validateFields();
        });

        // Валидация полей
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> validateFields());
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validateFields());
    }

    private void validateFields() {
        boolean valid = true;

        if(usePasswordCheck.isSelected()) {
            valid = !passwordField.getText().isEmpty()
                    && passwordField.getText().equals(confirmPasswordField.getText());
        }

        encryptBtn.setDisable(!valid);
    }

    @FXML
    private void handleGeneratePassword() {
        // Генерация случайного пароля
        String generated = PasswordGenerator.generate(16);
        passwordField.setText(generated);
        confirmPasswordField.setText(generated);
    }

    @FXML
    private void handleBack() {
        // Вернуться к предыдущему окну
    }

    @FXML
    private void handleEncrypt() {
        // Логика шифрования
        String algorithm = algorithmBox.getValue();
        String password = usePasswordCheck.isSelected() ? passwordField.getText() : null;

        // Вызов метода создания контейнера
        EncryptionManager.createContainer(
                containerPath,
                containerSize,
                containerName,
                algorithm,
                password
        );
    }

    public void setContainerData(String path, int size, String name) {
        this.containerPath = path;
        this.containerSize = size;
        this.containerName = name;
    }
}