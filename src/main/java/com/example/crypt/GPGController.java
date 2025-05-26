package com.example.crypt;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GPGController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TableView<GPGManager.GPGKey> keysTable;
    @FXML private TableColumn<GPGManager.GPGKey, String> keyIdColumn;
    @FXML private TableColumn<GPGManager.GPGKey, String> keyNameColumn;
    @FXML private TableColumn<GPGManager.GPGKey, String> keyEmailColumn;
    @FXML private TextField sourceFileField;
    @FXML private TextField encryptedFileField;
    @FXML private PasswordField decryptPasswordField;
    @FXML private ComboBox<GPGManager.GPGKey> recipientComboBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        // Настройка таблицы ключей
        keysTable.setPlaceholder(new Label("Таблица пуста"));
        keyIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        keyNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        keyEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        // Загрузка списка ключей
        refreshKeysList();
    }

    private void refreshKeysList() {
        try {
            List<GPGManager.GPGKey> keys = GPGManager.listKeys();
            keysTable.getItems().setAll(keys);
            recipientComboBox.getItems().setAll(keys);
        } catch (IOException e) {
            showError("Ошибка при загрузке списка ключей", e);
        }
    }

    @FXML
    private void handleGenerateKeyPair() {
        String name = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Ошибка", "Все поля должны быть заполнены");
            return;
        }

        try {
            showProgress("Генерация ключевой пары...");
            GPGManager.generateKeyPair(name, email, password);
            showSuccess("Ключевая пара успешно создана");
            refreshKeysList();
            clearKeyGenFields();
        } catch (IOException e) {
            showError("Ошибка при генерации ключевой пары", e);
        } finally {
            hideProgress();
        }
    }

    @FXML
    private void handleExportKey() {
        GPGManager.GPGKey selectedKey = keysTable.getSelectionModel().getSelectedItem();
        if (selectedKey == null) {
            showError("Ошибка", "Выберите ключ для экспорта");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить публичный ключ");
        fileChooser.setInitialFileName(selectedKey.getEmail() + ".asc");
        File file = fileChooser.showSaveDialog(getStage());

        if (file != null) {
            try {
                showProgress("Экспорт публичного ключа...");
                GPGManager.exportPublicKey(selectedKey.getId(), file.getAbsolutePath());
                showSuccess("Публичный ключ успешно экспортирован");
            } catch (IOException e) {
                showError("Ошибка при экспорте ключа", e);
            } finally {
                hideProgress();
            }
        }
    }

    @FXML
    private void handleImportKey() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите публичный ключ");
        File file = fileChooser.showOpenDialog(getStage());

        if (file != null) {
            try {
                showProgress("Импорт публичного ключа...");
                GPGManager.importPublicKey(file.getAbsolutePath());
                showSuccess("Публичный ключ успешно импортирован");
                refreshKeysList();
            } catch (IOException e) {
                showError("Ошибка при импорте ключа", e);
            } finally {
                hideProgress();
            }
        }
    }

    @FXML
    private void handleSelectSourceFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл для шифрования");
        File file = fileChooser.showOpenDialog(getStage());
        if (file != null) {
            sourceFileField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleSelectEncryptedFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите зашифрованный файл");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("GPG файлы", "*.gpg")
        );
        File file = fileChooser.showOpenDialog(getStage());
        if (file != null) {
            encryptedFileField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleEncrypt() {
        String sourceFile = sourceFileField.getText();
        GPGManager.GPGKey recipient = recipientComboBox.getValue();

        if (sourceFile.isEmpty() || recipient == null) {
            showError("Ошибка", "Выберите файл и получателя");
            return;
        }

        try {
            showProgress("Шифрование файла...");
            GPGManager.encryptFile(sourceFile, recipient.getId());
            showSuccess("Файл успешно зашифрован");
            sourceFileField.clear();
        } catch (IOException e) {
            showError("Ошибка при шифровании файла", e);
        } finally {
            hideProgress();
        }
    }

    @FXML
    private void handleDecrypt() {
        String encryptedFile = encryptedFileField.getText();
        String password = decryptPasswordField.getText();

        if (encryptedFile.isEmpty() || password.isEmpty()) {
            showError("Ошибка", "Заполните все поля");
            return;
        }

        try {
            showProgress("Расшифровка файла...");
            GPGManager.decryptFile(encryptedFile, password);
            showSuccess("Файл успешно расшифрован");
            encryptedFileField.clear();
            decryptPasswordField.clear();
        } catch (IOException e) {
            showError("Ошибка при расшифровке файла", e);
        } finally {
            hideProgress();
        }
    }

    private void clearKeyGenFields() {
        nameField.clear();
        emailField.clear();
        passwordField.clear();
    }

    private void showProgress(String message) {
        progressBar.setVisible(true);
        statusLabel.setText(message);
    }

    private void hideProgress() {
        progressBar.setVisible(false);
        statusLabel.setText("");
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успех");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        statusLabel.setText("Ошибка: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, Exception e) {
        showError(title, e.getMessage());
    }

    private Stage getStage() {
        return (Stage) nameField.getScene().getWindow();
    }
} 