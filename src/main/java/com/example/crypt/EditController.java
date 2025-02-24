package com.example.crypt;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class EditController {
    @FXML private TableView<Partition> partitionsTable;
    private final ObservableList<Partition> partitions = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Добавляем тестовые данные
        partitions.add(new Partition(
                "Личные данные",
                "/dev/sda1",
                "2048",
                "AES-256 (XTS)"
        ));

        partitions.add(new Partition(
                "Резервная копия",
                "/dev/sdb1",
                "4096",
                "Twofish (XTS)"
        ));

        partitionsTable.setItems(partitions);

        // Добавляем обработчик удаления
        partitions.forEach(partition ->
                partition.getDeleteButton().setOnAction(e ->
                        partitions.remove(partition)
                )
        );
    }

    @FXML
    private void handleApply() {
        closeWindow();
    }

    @FXML
    private void handleClose() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) partitionsTable.getScene().getWindow();
        stage.close();
    }
}
