package com.example.crypt;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class EncryptionSettingsController {
    @FXML private ComboBox<String> algorithmBox;
    @FXML private ComboBox<String> fsTypeBox;

    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private ToggleButton showPasswordBtn;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordVisibleField;
    @FXML private ToggleButton showConfirmPasswordBtn;

    @FXML private Label passwordLabel;
    @FXML private Label confirmLabel;
    @FXML private Button generatePasswordBtn;
    @FXML private Button encryptBtn;

    private String containerPath;
    private int containerSize;
    private String containerName;

    @FXML
    private void initialize() {
        // Заполнение алгоритмов шифрования
        algorithmBox.getItems().addAll(
                "AES-256 (XTS)",
                "Serpent (XTS)",
                "Twofish (XTS)",
                "AES-Twofish (XTS)",
                "AES-Twofish-Serpent (XTS)"
        );
        algorithmBox.getSelectionModel().selectFirst();

        // Заполнение типов файловых систем
        fsTypeBox.getItems().addAll("ext4", "fat32", "ntfs");
        fsTypeBox.getSelectionModel().selectFirst();

        // Связываем managed с visible для корректного layout
        passwordVisibleField.managedProperty().bind(passwordVisibleField.visibleProperty());
        confirmPasswordVisibleField.managedProperty().bind(confirmPasswordVisibleField.visibleProperty());

        // Управление видимостью полей пароля
        passwordVisibleField.visibleProperty().bind(showPasswordBtn.selectedProperty());
        passwordField.visibleProperty().bind(showPasswordBtn.selectedProperty().not());

        confirmPasswordVisibleField.visibleProperty().bind(showConfirmPasswordBtn.selectedProperty());
        confirmPasswordField.visibleProperty().bind(showConfirmPasswordBtn.selectedProperty().not());

        // Синхронизация текста между полями
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordVisibleField.isFocused()) {
                passwordVisibleField.setText(newVal);
            }
            validateFields();
        });
        passwordVisibleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordField.isFocused()) {
                passwordField.setText(newVal);
            }
            validateFields();
        });

        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!confirmPasswordVisibleField.isFocused()) {
                confirmPasswordVisibleField.setText(newVal);
            }
            validateFields();
        });
        confirmPasswordVisibleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!confirmPasswordField.isFocused()) {
                confirmPasswordField.setText(newVal);
            }
            validateFields();
        });

        // Кнопки переключения текста
        showPasswordBtn.setOnAction(event -> {
            if (showPasswordBtn.isSelected()) {
                showPasswordBtn.setText("Скрыть");
            } else {
                showPasswordBtn.setText("Показать");
            }
        });

        showConfirmPasswordBtn.setOnAction(event -> {
            if (showConfirmPasswordBtn.isSelected()) {
                showConfirmPasswordBtn.setText("Скрыть");
            } else {
                showConfirmPasswordBtn.setText("Показать");
            }
        });

        // Изначальное состояние кнопок
        showPasswordBtn.setSelected(false);
        showPasswordBtn.setText("Показать");
        showConfirmPasswordBtn.setSelected(false);
        showConfirmPasswordBtn.setText("Показать");

        validateFields();
    }

    private void validateFields() {
        String password = passwordField.isVisible() ? passwordField.getText() : passwordVisibleField.getText();
        String confirm = confirmPasswordField.isVisible() ? confirmPasswordField.getText() : confirmPasswordVisibleField.getText();

        boolean valid = !password.isEmpty()
                && password.equals(confirm)
                && password.length() >= 8;

        encryptBtn.setDisable(!valid);
    }

    @FXML
    private void handleGeneratePassword() {
        String generated = PasswordGenerator.generate(16);
        passwordField.setText(generated);
        confirmPasswordField.setText(generated);
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/creation_window.fxml"));
            Parent root = loader.load();

            HelloController controller = loader.getController();
            controller.setStage((Stage) encryptBtn.getScene().getWindow());

            Stage stage = (Stage) encryptBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showAlert("Ошибка", "Не удалось вернуться к предыдущему экрану: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleEncrypt() {
        String password = passwordField.isVisible() ? passwordField.getText() : passwordVisibleField.getText();
        String confirm = confirmPasswordField.isVisible() ? confirmPasswordField.getText() : confirmPasswordVisibleField.getText();

        if (containerPath == null || containerPath.isEmpty()) {
            showAlert("Ошибка", "Путь к контейнеру не указан.", Alert.AlertType.ERROR);
            return;
        }
        if (containerSize <= 0) {
            showAlert("Ошибка", "Размер контейнера должен быть больше 0 МБ.", Alert.AlertType.ERROR);
            return;
        }
        if (containerName == null || containerName.isEmpty()) {
            showAlert("Ошибка", "Имя контейнера не указано.", Alert.AlertType.ERROR);
            return;
        }
        if (password.isEmpty()) {
            showAlert("Ошибка", "Пароль не может быть пустым.", Alert.AlertType.ERROR);
            return;
        }
        if (!password.equals(confirm)) {
            showAlert("Ошибка", "Пароли не совпадают.", Alert.AlertType.ERROR);
            return;
        }

        String algorithm = algorithmBox.getValue();
        String fsType = fsTypeBox.getValue();

        try {
            EncryptionManager.createContainer(
                    containerPath,
                    containerSize,
                    containerName,
                    algorithm,
                    password,
                    fsType
            );

            showAlert("Успех", "Контейнер успешно создан и отформатирован.", Alert.AlertType.INFORMATION);

            Stage stage = (Stage) encryptBtn.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось создать контейнер: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void setContainerData(String path, int size, String name) {
        this.containerPath = path;
        this.containerSize = size;
        this.containerName = name;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
