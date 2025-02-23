package com.example.crypt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class EncryptionManager {

    /**
     * Создает зашифрованный контейнер, аналогично VeraCrypt.
     *
     * @param path      Путь для создания контейнера.
     * @param sizeMB    Размер контейнера в мегабайтах.
     * @param name      Имя контейнера.
     * @param algorithm Алгоритм шифрования (например, "aes-xts-plain64").
     * @param password  Пароль для контейнера.
     * @param fsType    Тип файловой системы (например, "ext4", "fat32").
     */
    public static void createContainer(
            String path,
            int sizeMB,
            String name,
            String algorithm,
            String password,
            String fsType
    ) {
        try {
            // Создаем файл-контейнер с помощью dd
            String containerPath = path + File.separator + name + ".container";
            createContainerFile(containerPath, sizeMB);

            // Инициализируем контейнер с помощью cryptsetup
            setupLUKSContainer(containerPath, algorithm, password);

            // Открываем контейнер и монтируем его
            String mapperName = "crypt_" + name;
            openLUKSContainer(containerPath, mapperName, password);

            // Создаем файловую систему внутри контейнера
            createFileSystem(mapperName, fsType);

            // Закрываем контейнер
            closeLUKSContainer(mapperName);

            System.out.println("Контейнер успешно создан и отформатирован: " + containerPath);
        } catch (IOException | InterruptedException e) {
            System.err.println("Ошибка при создании контейнера: " + e.getMessage());
        }
    }

    /**
     * Создает файл-контейнер с помощью команды dd.
     */
    private static void createContainerFile(String containerPath, int sizeMB) throws IOException, InterruptedException {
        String[] ddCommand = {
                "dd",
                "if=/dev/zero",
                "of=" + containerPath,
                "bs=1M",
                "count=" + sizeMB
        };

        executeCommand(ddCommand);
    }

    /**
     * Инициализирует контейнер с помощью cryptsetup.
     */
    private static void setupLUKSContainer(String containerPath, String algorithm, String password) throws IOException, InterruptedException {
        String[] luksFormatCommand = {
                "cryptsetup",
                "luksFormat",
                containerPath,
                "--type", "luks2",
                "--cipher", algorithm,
                "--key-size", "512"
        };

        executeCommandWithInput(luksFormatCommand, password + "\n");
    }

    /**
     * Открывает контейнер и создает виртуальное устройство.
     */
    private static void openLUKSContainer(String containerPath, String mapperName, String password) throws IOException, InterruptedException {
        String[] luksOpenCommand = {
                "cryptsetup",
                "open",
                containerPath,
                mapperName
        };

        executeCommandWithInput(luksOpenCommand, password + "\n");
    }

    /**
     * Создает файловую систему внутри контейнера.
     */
    private static void createFileSystem(String mapperName, String fsType) throws IOException, InterruptedException {
        String devicePath = "/dev/mapper/" + mapperName;
        String[] mkfsCommand;

        switch (fsType.toLowerCase()) {
            case "ext4":
                mkfsCommand = new String[]{"mkfs.ext4", devicePath};
                break;
            case "fat32":
                mkfsCommand = new String[]{"mkfs.vfat", "-F", "32", devicePath};
                break;
            default:
                throw new IllegalArgumentException("Неподдерживаемый тип файловой системы: " + fsType);
        }

        executeCommand(mkfsCommand);
    }

    /**
     * Закрывает контейнер.
     */
    private static void closeLUKSContainer(String mapperName) throws IOException, InterruptedException {
        String[] luksCloseCommand = {
                "cryptsetup",
                "close",
                mapperName
        };

        executeCommand(luksCloseCommand);
    }

    /**
     * Выполняет команду в терминале.
     */
    private static void executeCommand(String[] command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Чтение вывода команды
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Команда завершилась с ошибкой: " + exitCode);
        }
    }

    /**
     * Выполняет команду с вводом через stdin.
     */
    private static void executeCommandWithInput(String[] command, String input) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Передача ввода через stdin
        try (var writer = process.getOutputStream()) {
            writer.write(input.getBytes());
            writer.flush();
        }

        // Чтение вывода команды
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Команда завершилась с ошибкой: " + exitCode);
        }
    }
}