package com.example.crypt;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class HelloApplication extends Application {
    private static final String CONFIG_FILE = "encrypted_sections.ini";
    private Preferences prefs;

    public static void main(String[] args) {
        try {
            System.out.println("Начало программы...");
            // Получаем имя текущего пользователя
            String userName = System.getProperty("user.name");
            System.out.println("Имя пользователя: " + userName);
            String homeDir = System.getProperty("user.home");
            System.out.println("Домашняя директория: " + homeDir);

            // Вызов остальной логики программы
            launch(args);
        } catch (Exception e) {
            System.err.println("Ошибка при запуске программы: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void init() throws Exception {
        // Инициализация конфигурационного файла
        prefs = Preferences.userRoot().node(this.getClass().getName());
        File config = new File(CONFIG_FILE);
        if (!config.exists()) {
            config.createNewFile();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        setupSystemTray();
    }

    private void setupSystemTray() {
        if (!java.awt.SystemTray.isSupported()) {
            System.out.println("SystemTray not supported");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/image/icons8-system-tray-50.png"));
        TrayIcon trayIcon = new java.awt.TrayIcon(image);

        try {
            tray.add(trayIcon);
        } catch (java.awt.AWTException e) {
            e.printStackTrace();
        }

        // Создание контекстного меню
        java.awt.PopupMenu popup = new java.awt.PopupMenu();

        // Пункт меню "Создание"
        java.awt.MenuItem createItem = new java.awt.MenuItem("Создание раздела");
        createItem.addActionListener(e -> Platform.runLater(this::showCreationWindow));

        // Пункт меню "Редактировать разделы"
        java.awt.MenuItem editItem = new java.awt.MenuItem("Управление разделами");
        editItem.addActionListener(e -> Platform.runLater(this::showManagementWindow));

        java.awt.MenuItem automationItem = new java.awt.MenuItem("Автоматизация и резервное копирование");
        automationItem.addActionListener(e -> Platform.runLater(this::showAutomationWindow));

        // Добавляем пункт меню для GPG операций
        java.awt.MenuItem gpgItem = new java.awt.MenuItem("Шифрование отдельных файлов");
        gpgItem.addActionListener(e -> Platform.runLater(this::showGPGWindow));

        // Пункт меню "Редактировать разделы
        //        java.awt.MenuItem helpItem = new .ad"
        //java.awt.MenuItem backupItem = new java.awt.MenuItem("Бэкап");
        //backupItem.addActionListener(e -> Platform.runLater(this::showManagementWindow));

        java.awt.MenuItem helpItem = new java.awt.MenuItem("Справка");
        helpItem.addActionListener(e -> Platform.runLater(this::showHelpWindow));


        // Пункт меню "Выход"
        java.awt.MenuItem exitItem = new java.awt.MenuItem("Выход");
        exitItem.addActionListener(e -> {
            Platform.exit();
            tray.remove(trayIcon);
            System.exit(0);
        });

        popup.add(createItem);
        popup.add(editItem); // Добавляем кнопку редактирования
        popup.add(automationItem);
        //popup.add(backupItem);
        popup.add(gpgItem);
        popup.addSeparator();
        popup.add(helpItem);
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);
    }

    private void showManagementWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/edit_window.fxml"));
            Parent root = loader.load();
            Stage managementStage = new Stage();
            managementStage.setScene(new Scene(root));
            managementStage.show();
        } catch (IOException e) {
            System.err.println("Ошибка загрузки FXML: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Неизвестная ошибка: " + e.getMessage());
        }
    }

    private void showCreationWindow() {
        try {
            FXMLLoader loader = new FXMLLoader();
            URL fxmlUrl = getClass().getResource("/fxml/creation_window.fxml");
            loader.setLocation(fxmlUrl);
            Parent root = loader.load();

            HelloController controller = loader.getController();
            Stage creationStage = new Stage();
            controller.setStage(creationStage);

            Scene scene = new Scene(root);
            creationStage.setTitle("Создание шифрованного раздела");
            creationStage.setScene(scene);
            creationStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showHelpWindow() {
        try {
            FXMLLoader loader = new FXMLLoader();
            URL fxmlUrl = getClass().getResource("/fxml/help_window.fxml");
            loader.setLocation(fxmlUrl);
            Parent root = loader.load();
            
            Stage helpStage = new Stage();
            helpStage.setTitle("Справка");
            helpStage.setScene(new Scene(root));
            helpStage.show();
        } catch (IOException e) {
            System.err.println("Ошибка загрузки FXML: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Неизвестная ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showGPGWindow() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gpg_window.fxml"));
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setTitle("GPG Шифрование");
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Не удалось открыть окно GPG операций");
            }
        });
    }
    private static final Logger logger = Logger.getLogger(HelloApplication.class.getName());

    private void showAutomationWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/automation_window.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Автоматизация и резервное копирование");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Ошибка при открытии окна автоматизации", e);
        }
    }

    /*private void saveToConfig(String path, int size) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE, true)) {
            writer.write(String.format("[encrypted_section%n", System.currentTimeMillis()));
            writer.write(String.format("path = %s%n", path));
            writer.write(String.format("size = %d%n%n", size));
        } catch (Exception e) {
            e.printStackTrace();
        }+-
    }*/
}