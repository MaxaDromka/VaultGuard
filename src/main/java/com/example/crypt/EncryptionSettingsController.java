package com.example.crypt;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.security.Provider;
import java.security.Security;
import java.util.Set;
import java.util.TreeSet;

public class EncryptionSettingsController {
    @FXML private ComboBox<String> algorithmBox;
    @FXML private ComboBox<String> fsTypeBox;

    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private ToggleButton showPasswordBtn;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordVisibleField;
    @FXML private ToggleButton showConfirmPasswordBtn;
    @FXML private ProgressIndicator progressIndicator;


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
        // Получаем список поддерживаемых алгоритмов
        algorithmBox.getItems().addAll(CRYPTSETUP_CIPHERS);
        algorithmBox.getSelectionModel().selectFirst();

        // Заполнение типов файловых систем
        fsTypeBox.getItems().addAll("ext4");
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

    private static final String[] CRYPTSETUP_CIPHERS = {
            "aes-cbc",
            "serpent-cbc",
            "twofish-cbc",
            "aes-xts",
            "serpent-xts",
            "twofish-xts"
    };


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

        // Показываем индикатор и блокируем кнопки
        progressIndicator.setVisible(true);
        encryptBtn.setDisable(true);
        generatePasswordBtn.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                EncryptionManager.createContainer(
                        containerPath,
                        containerSize,
                        containerName,
                        algorithm,
                        password,
                        fsType
                );
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            progressIndicator.setVisible(false);
            encryptBtn.setDisable(false);
            generatePasswordBtn.setDisable(false);

            showAlert("Успех", "Контейнер успешно создан и отформатирован.", Alert.AlertType.INFORMATION);

            Stage stage = (Stage) encryptBtn.getScene().getWindow();
            stage.close();
        });

        task.setOnFailed(event -> {
            progressIndicator.setVisible(false);
            encryptBtn.setDisable(false);
            generatePasswordBtn.setDisable(false);

            Throwable e = task.getException();
            showAlert("Ошибка", "Не удалось создать контейнер: " + (e != null ? e.getMessage() : "Неизвестная ошибка"), Alert.AlertType.ERROR);
        });

        new Thread(task).start();
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

    public static Set<String> getAvailableCiphers() {
        Set<String> algorithms = new TreeSet<>();
        for (Provider provider : Security.getProviders()) {
            for (Object keyObj : provider.keySet()) {
                String key = keyObj.toString();
                if (key.startsWith("Cipher.")) {
                    String algorithm = key.substring("Cipher.".length());
                    int spaceIndex = algorithm.indexOf(' ');
                    if (spaceIndex > 0) {
                        algorithm = algorithm.substring(0, spaceIndex);
                    }
                    algorithms.add(algorithm);
                }
            }
        }
        return algorithms;
    }
}
