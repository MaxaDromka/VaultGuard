module com.example.crypt {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.prefs;


    opens com.example.crypt to javafx.fxml;
    exports com.example.crypt;
}