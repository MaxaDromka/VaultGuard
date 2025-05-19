package com.example.crypt;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;

public class Partition {
    private final StringProperty name;
    private final StringProperty path;
    private final StringProperty size;
    private final StringProperty algorithm;
    private final StringProperty creationTime;
    private final StringProperty encryptionMethod;
    private boolean isMounted;
    private boolean autoMount;
    private Button mountButton;
    private Button deleteButton;

    public Partition(String name, String path, String size, String algorithm, String creationTime, String encryptionMethod) {
        this.name = new SimpleStringProperty(name);
        this.path = new SimpleStringProperty(path);
        this.size = new SimpleStringProperty(size);
        this.algorithm = new SimpleStringProperty(algorithm);
        this.creationTime = new SimpleStringProperty(creationTime);
        this.encryptionMethod = new SimpleStringProperty(encryptionMethod);
        this.isMounted = false;
        this.autoMount = false;
        this.mountButton = new Button("Монтировать");
        this.deleteButton = new Button("Удалить");
    }

    // Геттеры для свойств
    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty pathProperty() {
        return path;
    }

    public StringProperty sizeProperty() {
        return size;
    }

    public StringProperty algorithmProperty() {
        return algorithm;
    }

    public StringProperty creationTimeProperty() {
        return creationTime;
    }

    public StringProperty encryptionMethodProperty() {
        return encryptionMethod;
    }

    // Обычные геттеры
    public String getName() {
        return name.get();
    }

    public String getPath() {
        return path.get();
    }

    public String getSize() {
        return size.get();
    }

    public String getAlgorithm() {
        return algorithm.get();
    }

    public String getCreationTime() {
        return creationTime.get();
    }

    public String getEncryptionMethod() {
        return encryptionMethod.get();
    }

    // Сеттеры
    public void setName(String name) {
        this.name.set(name);
    }

    public void setPath(String path) {
        this.path.set(path);
    }

    public void setSize(String size) {
        this.size.set(size);
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm.set(algorithm);
    }

    public void setCreationTime(String creationTime) {
        this.creationTime.set(creationTime);
    }

    public void setEncryptionMethod(String encryptionMethod) {
        this.encryptionMethod.set(encryptionMethod);
    }

    public boolean isIsMounted() {
        return isMounted;
    }

    public void setIsMounted(boolean mounted) {
        isMounted = mounted;
    }

    public boolean isAutoMount() {
        return autoMount;
    }

    public void setAutoMount(boolean autoMount) {
        this.autoMount = autoMount;
    }

    public Button getMountButton() {
        return mountButton;
    }

    public Button getDeleteButton() {
        return deleteButton;
    }
}