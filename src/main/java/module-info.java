module com.example.crypt {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;
    requires java.prefs;
    requires org.jnrproject.ffi;
    requires java.logging;


    opens com.example.crypt to javafx.fxml;
    exports com.example.crypt;
}