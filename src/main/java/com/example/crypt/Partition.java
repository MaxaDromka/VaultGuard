package com.example.crypt;
import javafx.scene.control.Button;

public class Partition {
    private String name;
    private String path;
    private String size;
    private String encryptionMethod;
    private boolean isMounted;

    // Кнопки для управления контейнером
    private final Button mountButton = new Button("Монтировать");
    private final Button deleteButton = new Button("Удалить");

    public Partition(String name, String path, String size, String encryptionMethod) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.encryptionMethod = encryptionMethod;
        this.isMounted = false; // По умолчанию контейнер не смонтирован
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

    public String getEncryptionMethod() {
        return encryptionMethod;
    }

    public boolean isIsMounted() {
        return isMounted;
    }

    public void setIsMounted(boolean mounted) {
        isMounted = mounted;
        // Обновляем текст кнопки монтирования
        mountButton.setText(mounted ? "Размонтировать" : "Монтировать");
    }

    // Методы для доступа к кнопкам
    public Button getMountButton() {
        return mountButton;
    }

    public Button getDeleteButton() {
        return deleteButton;
    }
}