package com.example.crypt;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class EncryptionSettingsController {
    @FXML private ComboBox<String> algorithmBox;
    @FXML private ComboBox<String> fsTypeBox; // Новое поле для выбора файловой системы
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

        // Заполняем типы файловых систем
        fsTypeBox.getItems().addAll("ext4", "fat32", "ntfs"); // Добавлены поддерживаемые файловые системы
        fsTypeBox.getSelectionModel().selectFirst();

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

        if (usePasswordCheck.isSelected()) {
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
        // Закрываем текущее окно и возвращаемся к предыдущему
        Stage stage = (Stage) encryptBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleEncrypt() {
        // Логика шифрования
        String algorithm = algorithmBox.getValue();
        String fsType = fsTypeBox.getValue(); // Получаем выбранную файловую систему
        String password = usePasswordCheck.isSelected() ? passwordField.getText() : null;

        try {
            // Вызов метода создания контейнера
            EncryptionManager.createContainer(
                    containerPath,
                    containerSize,
                    containerName,
                    algorithm,
                    password,
                    fsType // Передаем тип файловой системы
            );

            // Уведомление об успешном создании контейнера
            showAlert("Успех", "Контейнер успешно создан и отформатирован.", Alert.AlertType.INFORMATION);

            // Закрываем окно после успешного создания
            Stage stage = (Stage) encryptBtn.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            // Обработка ошибок
            showAlert("Ошибка", "Не удалось создать контейнер: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Устанавливает данные контейнера.
     */
    public void setContainerData(String path, int size, String name) {
        this.containerPath = path;
        this.containerSize = size;
        this.containerName = name;
    }

    /**
     * Показывает диалоговое окно с сообщением.
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}