package com.example.crypt;

import javafx.scene.control.Button;
public class Partition {
    private String name;
    private String path;
    private String size;
    private String encryptionType;
    private boolean isMounted;
    private boolean autoMount; // Добавлено новое поле
    private Button mountButton;
    private Button deleteButton;

    public Partition(String name, String path, String size, String encryptionType) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.encryptionType = encryptionType;
        this.isMounted = false;
        this.autoMount = false; // Инициализация нового поля
        this.mountButton = new Button("Монтировать");
        this.deleteButton = new Button("Удалить");
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getSize() {
        return size;
    }

    public String getEncryptionType() {
        return encryptionType;
    }

    public boolean isIsMounted() {
        return isMounted;
    }

    public void setIsMounted(boolean mounted) {
        isMounted = mounted;
    }

    public boolean isAutoMount() { // Геттер для нового поля
        return autoMount;
    }

    public void setAutoMount(boolean autoMount) { // Сеттер для нового поля
        this.autoMount = autoMount;
    }

    public Button getMountButton() {
        return mountButton;
    }

    public Button getDeleteButton() {
        return deleteButton;
    }
}