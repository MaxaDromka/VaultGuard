package com.example.crypt;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class HelloController {

    @FXML private TextField pathField;
    @FXML private Slider sizeSlider;
    @FXML private Label sizeValue;
    @FXML private Button nextBtn;
    @FXML private TextField nameField;

    private Stage stage;

    @FXML
    private void initialize() {
        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                sizeValue.setText(String.format("%d", newVal.intValue()))
        );
    }

    @FXML
    private void handleBrowse() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDir = directoryChooser.showDialog(stage);
        if (selectedDir != null) {
            pathField.setText(selectedDir.getAbsolutePath());
            nextBtn.setDisable(false);
        }
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void handleNext() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/encryption_settings.fxml"));
            Parent root = loader.load();

            EncryptionSettingsController controller = loader.getController();
            controller.setContainerData(
                    pathField.getText(),
                    (int) sizeSlider.getValue(),
                    nameField.getText()
            );

            Stage stage = (Stage) nextBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}