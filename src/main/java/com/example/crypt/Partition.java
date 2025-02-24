package com.example.crypt;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;

public class Partition {
    private final SimpleStringProperty name;
    private final SimpleStringProperty path;
    private final SimpleStringProperty size;
    private final SimpleStringProperty encryptionMethod;
    private final CheckBox autoMount;
    private final Button mountButton;
    private final Button deleteButton;
    private final SimpleBooleanProperty isMounted;

    public Partition(String name, String path, String size, String encryptionMethod) {
        this.name = new SimpleStringProperty(name);
        this.path = new SimpleStringProperty(path);
        this.size = new SimpleStringProperty(size);
        this.encryptionMethod = new SimpleStringProperty(encryptionMethod);
        this.autoMount = new CheckBox();
        this.isMounted = new SimpleBooleanProperty(false);

        // Кнопка монтирования/размонтирования
        this.mountButton = new Button();
        updateMountButtonText();

        // Обработчик для кнопки монтирования
        this.mountButton.setOnAction(e -> {
            isMounted.set(!isMounted.get());
            updateMountButtonText();
            // Здесь можно добавить реальную логику монтирования
        });

        // Кнопка удаления
        this.deleteButton = new Button("Удалить");
        this.deleteButton.getStyleClass().add("danger-button");
    }

    private void updateMountButtonText() {
        mountButton.setText(isMounted.get() ? "Размонтировать" : "Монтировать");
    }

    // Геттеры для свойств
    public String getName() { return name.get(); }
    public String getPath() { return path.get(); }
    public String getSize() { return size.get(); }
    public String getEncryptionMethod() { return encryptionMethod.get(); }
    public CheckBox getAutoMount() { return autoMount; }
    public Button getMountButton() { return mountButton; }
    public Button getDeleteButton() { return deleteButton; }
    public boolean isIsMounted() { return isMounted.get(); }
}