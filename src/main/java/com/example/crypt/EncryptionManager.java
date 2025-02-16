package com.example.crypt;

public class EncryptionManager {
    public static void createContainer(
            String path,
            int sizeMB,
            String name,
            String algorithm,
            String password
    ) {
        //быть реализация создания контейнера
        //использования команд
        // dd if=/dev/zero of=container bs=1M count=sizeMB
        // cryptsetup luksFormat container
        // и т.д.

        System.out.println("Creating container with settings:");
        System.out.println("Path: " + path);
        System.out.println("Size: " + sizeMB + "MB");
        System.out.println("Algorithm: " + algorithm);
        System.out.println("Password: " + (password != null ? "set" : "none"));
    }
}