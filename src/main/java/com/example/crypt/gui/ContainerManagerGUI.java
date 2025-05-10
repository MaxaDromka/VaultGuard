package com.example.crypt.gui;

import com.example.crypt.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.util.*;
import java.io.IOException;

public class ContainerManagerGUI extends Application {
    private TableView<Partition> containerTable;
    private PieChart usageChart;
    private LineChart<Number, Number> activityChart;
    private TextArea logArea;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Менеджер зашифрованных контейнеров");
        
        // Создаем основной layout
        BorderPane root = new BorderPane();
        
        // Создаем таблицу контейнеров
        containerTable = createContainerTable();
        root.setCenter(containerTable);
        
        // Создаем панель с графиками
        VBox chartsBox = createChartsPanel();
        root.setRight(chartsBox);
        
        // Создаем панель логов
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(10);
        root.setBottom(logArea);
        
        // Создаем панель инструментов
        HBox toolbar = createToolbar();
        root.setTop(toolbar);
        
        // Настраиваем сцену
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        
        // Запускаем мониторинг
        ContainerMonitor.startMonitoring();
        
        // Обновляем данные каждые 5 секунд
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateData();
            }
        }, 0, 5000);
        
        primaryStage.show();
    }
    
    private TableView<Partition> createContainerTable() {
        TableView<Partition> table = new TableView<>();
        
        TableColumn<Partition, String> nameCol = new TableColumn<>("Имя");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        
        TableColumn<Partition, String> pathCol = new TableColumn<>("Путь");
        pathCol.setCellValueFactory(data -> data.getValue().pathProperty());
        
        TableColumn<Partition, String> sizeCol = new TableColumn<>("Размер");
        sizeCol.setCellValueFactory(data -> data.getValue().sizeProperty());
        
        TableColumn<Partition, String> algorithmCol = new TableColumn<>("Алгоритм");
        algorithmCol.setCellValueFactory(data -> data.getValue().algorithmProperty());
        
        table.getColumns().addAll(nameCol, pathCol, sizeCol, algorithmCol);
        table.setItems(EncryptionManager.getContainersList());
        
        return table;
    }
    
    private VBox createChartsPanel() {
        VBox chartsBox = new VBox(10);
        chartsBox.setPadding(new Insets(10));
        
        // Создаем круговую диаграмму использования
        usageChart = new PieChart();
        usageChart.setTitle("Использование контейнеров");
        
        // Создаем график активности
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Время");
        yAxis.setLabel("Активность");
        
        activityChart = new LineChart<>(xAxis, yAxis);
        activityChart.setTitle("Активность контейнеров");
        
        chartsBox.getChildren().addAll(usageChart, activityChart);
        return chartsBox;
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        
        Button createBtn = new Button("Создать контейнер");
        createBtn.setOnAction(e -> showCreateContainerDialog());
        
        Button mountBtn = new Button("Монтировать");
        mountBtn.setOnAction(e -> mountSelectedContainer());
        
        Button unmountBtn = new Button("Размонтировать");
        unmountBtn.setOnAction(e -> unmountSelectedContainer());
        
        Button resizeBtn = new Button("Изменить размер");
        resizeBtn.setOnAction(e -> showResizeContainerDialog());
        
        Button refreshBtn = new Button("Обновить");
        refreshBtn.setOnAction(e -> updateData());
        
        toolbar.getChildren().addAll(createBtn, mountBtn, unmountBtn, resizeBtn, refreshBtn);
        return toolbar;
    }
    
    private void showCreateContainerDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Создание нового контейнера");
        dialog.setHeaderText("Введите параметры контейнера");
        
        // Создаем поля ввода
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        TextField sizeField = new TextField();
        ComboBox<String> algorithmCombo = new ComboBox<>(FXCollections.observableArrayList(
            "AES (XTS-PLAIN64)", "Twofish (XTS-PLAIN64)", "Serpent (XTS-PLAIN64)"
        ));
        ComboBox<String> fsTypeCombo = new ComboBox<>(FXCollections.observableArrayList(
            "ext4", "xfs", "btrfs"
        ));
        
        grid.add(new Label("Имя:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Размер (МБ):"), 0, 1);
        grid.add(sizeField, 1, 1);
        grid.add(new Label("Алгоритм:"), 0, 2);
        grid.add(algorithmCombo, 1, 2);
        grid.add(new Label("Файловая система:"), 0, 3);
        grid.add(fsTypeCombo, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType createButtonType = new ButtonType("Создать", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == createButtonType) {
                try {
                    String name = nameField.getText();
                    int size = Integer.parseInt(sizeField.getText());
                    String algorithm = algorithmCombo.getValue();
                    String fsType = fsTypeCombo.getValue();
                    
                    // Здесь будет вызов метода создания контейнера
                    LogManager.logInfo("Создание нового контейнера: " + name);
                } catch (NumberFormatException e) {
                    LogManager.logError("Ошибка при создании контейнера", e);
                }
            }
        });
    }
    
    private void mountSelectedContainer() {
        Partition selected = containerTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Здесь будет вызов метода монтирования
            LogManager.logInfo("Монтирование контейнера: " + selected.getName());
        }
    }
    
    private void unmountSelectedContainer() {
        Partition selected = containerTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Здесь будет вызов метода размонтирования
            LogManager.logInfo("Размонтирование контейнера: " + selected.getName());
        }
    }
    
    private void showResizeContainerDialog() {
        Partition selected = containerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Выберите контейнер для изменения размера");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Изменение размера контейнера");
        dialog.setHeaderText("Введите новый размер для контейнера " + selected.getName());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField sizeField = new TextField();
        sizeField.setPromptText("Размер в МБ");

        grid.add(new Label("Новый размер (МБ):"), 0, 0);
        grid.add(sizeField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        ButtonType resizeButtonType = new ButtonType("Изменить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resizeButtonType, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == resizeButtonType) {
                try {
                    int newSize = Integer.parseInt(sizeField.getText());
                    EncryptionManager.resizeContainer(selected.getPath(), newSize);
                    LogManager.logInfo("Размер контейнера " + selected.getName() + " изменен на " + newSize + " МБ");
                    updateData();
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Ошибка", "Введите корректное числовое значение");
                } catch (IOException e) {
                    showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось изменить размер контейнера: " + e.getMessage());
                }
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void updateData() {
        // Обновляем таблицу
        containerTable.setItems(EncryptionManager.getContainersList());
        
        // Обновляем круговую диаграмму
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Partition container : containerTable.getItems()) {
            ContainerMonitor.ContainerStats stats = ContainerMonitor.getContainerStats(container.getPath());
            if (stats != null) {
                pieChartData.add(new PieChart.Data(
                    container.getName(),
                    stats.getUsagePercentage()
                ));
            }
        }
        usageChart.setData(pieChartData);
        
        // Обновляем график активности
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Активность");
        for (Partition container : containerTable.getItems()) {
            ContainerMonitor.ContainerStats stats = ContainerMonitor.getContainerStats(container.getPath());
            if (stats != null) {
                series.getData().add(new XYChart.Data<>(
                    System.currentTimeMillis(),
                    stats.accessCount
                ));
            }
        }
        activityChart.getData().clear();
        activityChart.getData().add(series);
        
        // Обновляем логи
        updateLogArea();
    }
    
    private void updateLogArea() {
        // Здесь будет обновление области логов
        // Можно использовать LogManager для получения последних записей
    }
    
    @Override
    public void stop() {
        ContainerMonitor.stopMonitoring();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 