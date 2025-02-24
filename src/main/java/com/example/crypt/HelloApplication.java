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
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences;

public class HelloApplication extends Application {
    private static final String CONFIG_FILE = "encrypted_sections.ini";
    private Preferences prefs;

    public static void main(String[] args) {
        launch(args);
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
        java.awt.MenuItem createItem = new java.awt.MenuItem("Создание");
        createItem.addActionListener(e -> Platform.runLater(this::showCreationWindow));

        // Пункт меню "Редактировать разделы"
        java.awt.MenuItem editItem = new java.awt.MenuItem("Редактировать разделы");
        editItem.addActionListener(e -> Platform.runLater(this::showManagementWindow));



        // Пункт меню "Выход"
        java.awt.MenuItem exitItem = new java.awt.MenuItem("Выход");
        exitItem.addActionListener(e -> {
            Platform.exit();
            tray.remove(trayIcon);
            System.exit(0);
        });

        popup.add(createItem);
        popup.add(editItem); // Добавляем кнопку редактирования
        popup.addSeparator();
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

    /*private void saveToConfig(String path, int size) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE, true)) {
            writer.write(String.format("[encrypted_section%n", System.currentTimeMillis()));
            writer.write(String.format("path = %s%n", path));
            writer.write(String.format("size = %d%n%n", size));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
}