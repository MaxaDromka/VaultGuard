module com.example.crypt {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.jnrproject.ffi;
    requires java.logging;
    requires java.desktop;
    requires java.prefs;

    opens com.example.crypt to javafx.fxml;
    exports com.example.crypt;
}