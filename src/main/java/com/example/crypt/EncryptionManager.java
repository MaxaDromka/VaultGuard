package com.example.crypt;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jnr.ffi.Pointer;
import jnr.ffi.byref.PointerByReference;
import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class EncryptionManager {
    private static final CryptSetup crypt = CryptSetup.load();
    private static final Logger logger = Logger.getLogger(EncryptionManager.class.getName());

    static {
        try {
            System.loadLibrary("cryptsetup");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Ошибка: cryptsetup-devel не найдена. Установите её с помощью 'pkexec dnf install cryptsetup-devel'");
            System.exit(1);
        }
    }

    private static void executeWithPolkit(String command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
            "pkexec",
            "sh",
            "-c",
            command
        );
        
        Process process = pb.start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // Читаем вывод ошибки
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String errorLine;
                    StringBuilder errorMessage = new StringBuilder();
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorMessage.append(errorLine).append("\n");
                    }
                    throw new IOException("Команда завершилась с ошибкой: " + exitCode + "\n" + errorMessage.toString());
                }
            }
        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        }
    }

    /**
     * Создает зашифрованный контейнер.
     */
    public static void createContainer(
            String containerPath,
            int sizeMB,
            String name,
            String algorithm,
            String password,
            String fsType
    ) throws IOException {
        PointerByReference cdRef = new PointerByReference();
        Pointer cd = null;
        String loopDevice = null;

        try {
            // 1. Проверяем и удаляем существующий файл контейнера
            File containerFile = new File(containerPath);
            if (containerFile.exists()) {
                logger.warning("Файл контейнера уже существует: " + containerPath);
                if (!containerFile.delete()) {
                    throw new IOException("Не удалось удалить существующий файл контейнера: " + containerPath);
                }
            }

            // 2. Создание файла-контейнера
            logger.info("Создание файла-контейнера размером " + sizeMB + "MB");
            executeWithPolkit("/usr/bin/truncate -s " + sizeMB + "M " + containerPath);

            // 3. Отключаем существующие loop-устройства
            detachLoopDevices(containerPath);

            // 4. Создание loop-устройства
            logger.info("Создание loop-устройства");
            Process losetup = Runtime.getRuntime().exec(
                    "pkexec /usr/sbin/losetup -f --show " + containerPath
            );
            loopDevice = readProcessOutput(losetup);
            if (loopDevice == null || loopDevice.isEmpty()) {
                throw new IOException("Не удалось создать loop-устройство");
            }
            loopDevice = loopDevice.trim();
            logger.info("Создано loop-устройство: " + loopDevice);

            // 5. Инициализация cryptsetup
            logger.info("Инициализация cryptsetup");
            int result = crypt.crypt_init(cdRef, loopDevice);
            if (result < 0) {
                throw new IOException("Ошибка при инициализации cryptsetup: " + result);
            }
            cd = cdRef.getValue();

            // 6. Форматирование LUKS2
            logger.info("Форматирование LUKS2");
            int loadResult = crypt.crypt_load(cd, "LUKS2", null);
            if (loadResult == 0) {
                logger.warning("Устройство уже является LUKS2. Форматирование будет пропущено.");
            } else {
                int formatResult = crypt.crypt_format(
                        cd,
                        "LUKS2",
                        "aes",
                        "xts-plain64",
                        null,
                        null,
                        64,
                        null
                );
                if (formatResult < 0) {
                    throw new IOException("Ошибка при форматировании LUKS2: " + formatResult);
                }
            }

            // 7. Добавление ключа
            logger.info("Добавление ключа шифрования");
            int keyslotResult = crypt.crypt_keyslot_add_by_volume_key(
                    cd,
                    -1,
                    null,
                    0,
                    password,
                    password.length()
            );
            if (keyslotResult < 0) {
                throw new IOException("Ошибка при добавлении ключа: " + keyslotResult);
            }

            // 8. Активация контейнера
            logger.info("Активация контейнера");
            String mappedName = "crypt_" + name;
            int activateResult = crypt.crypt_activate_by_passphrase(
                    cd,
                    mappedName,
                    -1,
                    password,
                    password.length(),
                    0
            );
            if (activateResult < 0) {
                throw new IOException("Ошибка при активации контейнера: " + activateResult);
            }

            // 9. Создание файловой системы
            logger.info("Создание файловой системы типа " + fsType);
            String devicePath = "/dev/mapper/" + mappedName;
            Process mkfs = Runtime.getRuntime().exec(String.format("sudo mkfs.%s %s", fsType, devicePath));
            if (!mkfs.waitFor(30, TimeUnit.SECONDS) || mkfs.exitValue() != 0) {
                throw new IOException("Ошибка при создании файловой системы");
            }

            // 10. Настройка автоматического монтирования через /etc/fstab
            logger.info("Настройка автоматического монтирования через /etc/fstab");
            String mountPoint = "/mnt/" + name;
            String uuid = getUUID(devicePath);
            String fstabEntry = String.format(
                    "UUID=%s  %s  %s  defaults,nofail  0  2\n",
                    uuid,
                    mountPoint,
                    fsType
            );

            // Добавляем запись в /etc/fstab
            Process appendFstab = Runtime.getRuntime().exec(
                    String.format("echo '%s' | sudo tee -a /etc/fstab", fstabEntry)
            );
            if (!appendFstab.waitFor(30, TimeUnit.SECONDS) || appendFstab.exitValue() != 0) {
                throw new IOException("Ошибка при добавлении записи в /etc/fstab");
            }

            logger.info("Контейнер успешно создан и настроен для автоматического монтирования");
        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        } finally {
            if (loopDevice != null) {
                try {
                    executeWithPolkit("/usr/sbin/losetup -d " + loopDevice);
                } catch (Exception e) {
                    logger.warning("Ошибка при отсоединении loop-устройства: " + e.getMessage());
                }
            }
            if (cd != null) {
                crypt.crypt_free(cd);
            }
        }
    }

    private static String getUUID(String devicePath) throws IOException {
        logger.info("Получение UUID устройства: " + devicePath);
        Process blkid = Runtime.getRuntime().exec(String.format("sudo blkid -s UUID -o value %s", devicePath));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(blkid.getInputStream()))) {
            String uuid = reader.readLine();
            if (uuid == null || uuid.isEmpty()) {
                throw new IOException("Не удалось получить UUID для устройства: " + devicePath);
            }
            return uuid.trim();
        }
    }

    private static void detachLoopDevices(String containerPath) throws IOException {
        Process process = Runtime.getRuntime().exec("pkexec /usr/sbin/losetup -j " + containerPath);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(containerPath)) {
                    String loopDevice = line.split(":")[0].trim();
                    logger.info("Отключение loop-устройства: " + loopDevice);
                    executeWithPolkit("/usr/sbin/losetup -d " + loopDevice);
                }
            }
        }
    }

    /**
     * Монтирует зашифрованный контейнер.
     */
    public static void mountContainer(String containerPath, String name, String password, String mountPoint) throws IOException {
        PointerByReference cdRef = new PointerByReference();
        Pointer cd = null;

        try {
            // 1. Загрузка заголовка LUKS
            int result = crypt.crypt_init(cdRef, containerPath);
            if (result < 0) {
                throw new IOException("Ошибка при инициализации cryptsetup: " + result);
            }
            cd = cdRef.getValue();

            result = crypt.crypt_load(cd, "LUKS2", null);
            if (result < 0) {
                throw new IOException("Ошибка при загрузке заголовка LUKS: " + result);
            }

            // 2. Активация контейнера
            String mappedName = "crypt_" + name;
            File device = new File("/dev/mapper/" + mappedName);

            if (device.exists()) {
                System.out.println("Устройство уже активировано. Деактивируем...");
                int deactivateResult = crypt.crypt_deactivate(cd, mappedName);
                if (deactivateResult < 0) {
                    throw new IOException("Ошибка при деактивации существующего устройства: " + mappedName);
                }
            }

            System.out.println("Попытка активации устройства: " + mappedName);
            result = crypt.crypt_activate_by_passphrase(cd, mappedName, -1, password, password.length(), 0);
            if (result < 0) {
                throw new IOException("Ошибка при активации контейнера: " + result);
            }
            System.out.println("Устройство успешно активировано: " + mappedName);

            // 3. Монтирование файловой системы
            String devicePath = "/dev/mapper/" + mappedName;
            File mountDir = new File(mountPoint);

            if (!mountDir.exists()) {
                System.out.println("Создаем директорию для монтирования: " + mountPoint);
                if (!mountDir.mkdirs()) {
                    throw new IOException("Не удалось создать директорию для монтирования: " + mountPoint);
                }
            }

            System.out.println("Проверка типа файловой системы...");
            Process blkid = Runtime.getRuntime().exec(String.format("sudo blkid %s", devicePath));
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(blkid.getInputStream()))) {
                String output = reader.readLine();
                if (output == null || !output.contains("TYPE=")) {
                    throw new IOException("Файловая система не найдена. Создайте её с помощью 'sudo mkfs.ext4 " + devicePath + "'");
                }
                System.out.println("Тип файловой системы: " + output);
            }

            System.out.println("Попытка монтирования устройства: " + devicePath + " в " + mountPoint);
            Process mount = Runtime.getRuntime().exec(String.format("sudo mount %s %s", devicePath, mountPoint));

            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(mount.getErrorStream()))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    System.err.println("Ошибка монтирования: " + errorLine);
                }
            }

            if (!mount.waitFor(30, TimeUnit.SECONDS) || mount.exitValue() != 0) {
                throw new IOException("Ошибка при монтировании файловой системы");
            }
            System.out.println("Файловая система успешно смонтирована в: " + mountPoint);

            // 4. Регистрация устройства через udisks
            registerWithUdisks(devicePath);

            // 5. Изменение прав доступа
            Process chown = Runtime.getRuntime().exec(String.format("sudo chown -R %s:%s %s", System.getProperty("user.name"), System.getProperty("user.name"), mountPoint));
            if (!chown.waitFor(30, TimeUnit.SECONDS) || chown.exitValue() != 0) {
                throw new IOException("Ошибка при изменении прав");
            }

        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        } finally {
            if (cd != null) {
                crypt.crypt_free(cd);
            }
        }
    }

    private static void registerWithUdisks(String devicePath) throws IOException {
        // Проверяем, смонтировано ли устройство
        Process mountCheck = Runtime.getRuntime().exec("mount");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(mountCheck.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(devicePath)) {
                    System.out.println("Устройство уже смонтировано: " + devicePath);
                    return;
                }
            }
        }

        // Если устройство не смонтировано, регистрируем через udisksctl
        executeWithPolkit("/usr/bin/udisksctl mount -b " + devicePath);
    }

    /**
     * Размонтирует зашифрованный контейнер.
     */
    public static void unmountContainer(String name, String mountPoint) throws IOException {
        String mappedName = "crypt_" + name;

        // 1. Размонтирование файловой системы
        executeWithPolkit("/usr/bin/umount " + mountPoint);

        // 2. Закрытие зашифрованного контейнера
        PointerByReference cdRef = new PointerByReference();
        Pointer cd = null;
        int result = crypt.crypt_init_by_name(cdRef, mappedName);
        if (result < 0) {
            throw new IOException("Ошибка при инициализации: " + result);
        }
        cd = cdRef.getValue();
        result = crypt.crypt_deactivate(cd, mappedName);
        if (result < 0) {
            throw new IOException("Ошибка при деактивации контейнера: " + result);
        }

        // 3. Отключение loop-устройства
        detachLoopDevices(name);

        System.out.println("Контейнер успешно размонтирован");

    }

    private static String readProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.readLine();
            if (output == null) {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String error = errorReader.readLine();
                    if (error != null) {
                        throw new IOException("Ошибка выполнения команды: " + error);
                    }
                }
                throw new IOException("Команда не вернула результат");
            }
            return output;
        }
    }

    public static ObservableList<Partition> getContainersList() {
        ObservableList<Partition> containers = FXCollections.observableArrayList();
        String containersDir = "/home/maksim/containers"; // Используйте правильный путь
        File containerDir = new File(containersDir);

        if (!containerDir.exists()) {
            System.out.println("Папка не существует: " + containersDir);
            return containers;
        }

        if (containerDir.exists() && containerDir.isDirectory()) {
            File[] files = containerDir.listFiles((dir, name) ->
                    name.startsWith(".") && !name.equals(".") && !name.equals("..")
            );
            if (files != null) {
                for (File file : files) {
                    System.out.println("Найден файл: " + file.getName());
                    try {
                        String name = file.getName().replaceFirst("^\\.", ""); // Убираем точку из имени
                        long size = file.length() / (1024 * 1024); // Размер в МБ
                        String algorithm = getContainerAlgorithm(file.getAbsolutePath());
                        System.out.println("Алгоритм для файла " + file.getName() + ": " + algorithm);
                        containers.add(new Partition(name, file.getAbsolutePath(), String.valueOf(size), algorithm));
                    } catch (Exception e) {
                        System.err.println("Ошибка при обработке файла " + file.getName() + ": " + e.getMessage());
                    }
                }
            } else {
                System.out.println("Файлы не найдены");
            }
        } else {
            System.out.println("Папка не является директорией: " + containersDir);
        }

        System.out.println("Количество найденных контейнеров: " + containers.size());
        return containers;
    }

    private static String getContainerAlgorithm(String containerPath) {
        PointerByReference cdRef = new PointerByReference();
        Pointer cd = null;
        try {
            // Инициализация cryptsetup
            int result = crypt.crypt_init(cdRef, containerPath);
            if (result != 0) {
                System.err.println("Ошибка при инициализации cryptsetup для файла: " + containerPath);
                return "Неизвестный";
            }
            cd = cdRef.getValue();

            // Загрузка заголовка LUKS
            result = crypt.crypt_load(cd, "LUKS2", null);
            if (result != 0) {
                System.err.println("Ошибка при загрузке заголовка LUKS для файла: " + containerPath);
                return "Неизвестный";
            }

            // Получение информации о шифре
            String cipher = crypt.crypt_get_cipher(cd);
            String cipherMode = crypt.crypt_get_cipher_mode(cd);
            if (cipher != null && cipherMode != null) {
                return cipher.toUpperCase() + " (" + cipherMode.toUpperCase() + ")";
            }
            return "Неизвестный";
        } catch (Exception e) {
            System.err.println("Ошибка при определении алгоритма для файла: " + containerPath);
            e.printStackTrace();
            return "Неизвестный";
        } finally {
            if (cd != null) {
                crypt.crypt_free(cd);
            }
        }
    }

    /**
     * Изменяет размер существующего контейнера.
     */
    public static void resizeContainer(String containerPath, int newSizeMB) throws IOException {
        LogManager.logInfo("Начало изменения размера контейнера: " + containerPath);
        
        // 1. Проверяем существование контейнера
        File containerFile = new File(containerPath);
        if (!containerFile.exists()) {
            throw new IOException("Контейнер не найден: " + containerPath);
        }
        
        // 2. Получаем текущий размер
        long currentSize = containerFile.length() / (1024 * 1024); // Размер в МБ
        if (newSizeMB <= currentSize) {
            throw new IOException("Новый размер должен быть больше текущего");
        }
        
        // 3. Размонтируем контейнер, если он смонтирован
        String containerName = containerFile.getName().replaceFirst("^\\.", "");
        String mountPoint = "/mnt/" + containerName;
        try {
            unmountContainer(containerName, mountPoint);
        } catch (IOException e) {
            LogManager.logWarning("Контейнер не был размонтирован: " + e.getMessage());
        }
        
        // 4. Изменяем размер файла
        LogManager.logInfo("Изменение размера файла контейнера");
        executeWithPolkit("/usr/bin/truncate -s " + newSizeMB + "M " + containerPath);
        
        // 5. Расширяем файловую систему
        String mappedName = "crypt_" + containerName;
        String devicePath = "/dev/mapper/" + mappedName;
        
        // Активируем контейнер
        PointerByReference cdRef = new PointerByReference();
        Pointer cd = null;
        try {
            int result = crypt.crypt_init(cdRef, containerPath);
            if (result < 0) {
                throw new IOException("Ошибка при инициализации cryptsetup: " + result);
            }
            cd = cdRef.getValue();
            
            result = crypt.crypt_activate_by_passphrase(
                cd,
                mappedName,
                -1,
                "password", // Здесь нужно запросить пароль у пользователя
                "password".length(),
                0
            );
            if (result < 0) {
                throw new IOException("Ошибка при активации контейнера: " + result);
            }
            
            // Расширяем файловую систему
            LogManager.logInfo("Расширение файловой системы");
            Process resize = Runtime.getRuntime().exec(String.format("sudo resize2fs %s", devicePath));
            if (!resize.waitFor(30, TimeUnit.SECONDS) || resize.exitValue() != 0) {
                throw new IOException("Ошибка при расширении файловой системы");
            }
            
            LogManager.logInfo("Размер контейнера успешно изменен");
        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        } finally {
            if (cd != null) {
                crypt.crypt_free(cd);
            }
        }
    }
}