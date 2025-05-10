package com.example.crypt.gui;

import com.example.crypt.*;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import java.util.*;

public class StatisticsWindow {
    private static Stage statsStage;
    private static TableView<Partition> containerTable;
    private static PieChart usageChart;
    private static LineChart<Number, Number> activityChart;
    private static TextArea logArea;
    private static Timer updateTimer;

    public static void show() {
        if (statsStage == null) {
            createWindow();
        }
        
        if (!statsStage.isShowing()) {
            statsStage.show();
            updateData();
        }
    }

    private static void createWindow() {
        statsStage = new Stage();
        statsStage.setTitle("Статистика контейнеров");
        
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
        statsStage.setScene(scene);
        
        // Обработчик закрытия окна
        statsStage.setOnCloseRequest(e -> {
            statsStage.hide();
            e.consume();
        });

        // Создаем таймер для обновления данных
        updateTimer = new Timer(true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (statsStage.isShowing()) {
                    updateData();
                }
            }
        }, 0, 5000);
    }
    
    private static TableView<Partition> createContainerTable() {
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
        
        // Добавляем обработчик выбора контейнера
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showContainerDetails(newSelection);
            }
        });
        
        return table;
    }
    
    private static VBox createChartsPanel() {
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
    
    private static HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        
        Button refreshBtn = new Button("Обновить");
        refreshBtn.setOnAction(e -> updateData());
        
        Button showLogsBtn = new Button("Показать логи");
        showLogsBtn.setOnAction(e -> showLogs());
        
        toolbar.getChildren().addAll(refreshBtn, showLogsBtn);
        return toolbar;
    }
    
    private static void showContainerDetails(Partition container) {
        ContainerMonitor.ContainerStats stats = ContainerMonitor.getContainerStats(container.getPath());
        if (stats != null) {
            // Обновляем круговую диаграмму
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Использовано", stats.getUsagePercentage()),
                new PieChart.Data("Свободно", 100 - stats.getUsagePercentage())
            );
            usageChart.setData(pieChartData);
            
            // Обновляем график активности
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("Активность");
            series.getData().add(new XYChart.Data<>(0, stats.getAccessCount()));
            activityChart.getData().clear();
            activityChart.getData().add(series);
            
            // Показываем последние действия
            StringBuilder activities = new StringBuilder("Последние действия:\n");
            for (String activity : stats.getRecentActivities()) {
                activities.append(activity).append("\n");
            }
            logArea.setText(activities.toString());
        }
    }
    
    private static void showLogs() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Логи");
        dialog.setHeaderText("Последние записи в логах");
        
        TabPane tabPane = new TabPane();
        
        // Вкладка с основными логами
        Tab mainLogTab = new Tab("Основные логи");
        TextArea mainLogArea = new TextArea(LogManager.getRecentLogs(100));
        mainLogArea.setEditable(false);
        mainLogTab.setContent(mainLogArea);
        
        // Вкладка с аудит-логами
        Tab auditLogTab = new Tab("Аудит-логи");
        TextArea auditLogArea = new TextArea(LogManager.getRecentAuditLogs(100));
        auditLogArea.setEditable(false);
        auditLogTab.setContent(auditLogArea);
        
        tabPane.getTabs().addAll(mainLogTab, auditLogTab);
        
        dialog.getDialogPane().setContent(tabPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        dialog.showAndWait();
    }
    
    private static void updateData() {
        // Обновляем таблицу
        containerTable.setItems(EncryptionManager.getContainersList());
        
        // Обновляем графики для выбранного контейнера
        Partition selected = containerTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showContainerDetails(selected);
        }
    }
} 