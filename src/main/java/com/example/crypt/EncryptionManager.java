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
            System.err.println("Ошибка: cryptsetup-devel не найдена. Установите её с помощью 'sudo dnf install cryptsetup-devel'");
            System.exit(1);
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
            Process truncate = Runtime.getRuntime().exec(
                    String.format("sudo truncate -s %dM %s", sizeMB, containerPath)
            );
            if (!truncate.waitFor(30, TimeUnit.SECONDS) || truncate.exitValue() != 0) {
                throw new IOException("Ошибка при создании файла контейнера");
            }

            // 3. Отключаем существующие loop-устройства
            detachLoopDevices(containerPath);

            // 4. Создание loop-устройства
            logger.info("Создание loop-устройства");
            Process losetup = Runtime.getRuntime().exec(
                    String.format("sudo losetup -f --show %s", containerPath)
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

            // 6. Проверяем и форматируем LUKS2
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

            logger.info("Контейнер успешно создан");
        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        } finally {
            if (loopDevice != null) {
                try {
                    Runtime.getRuntime().exec(String.format("sudo losetup -d %s", loopDevice)).waitFor();
                } catch (Exception e) {
                    logger.warning("Ошибка при отсоединении loop-устройства: " + e.getMessage());
                }
            }
            if (cd != null) {
                crypt.crypt_free(cd);
            }
        }
    }

    private static void detachLoopDevices(String containerPath) throws IOException {
        Process process = Runtime.getRuntime().exec("losetup -j " + containerPath);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(containerPath)) {
                    String loopDevice = line.split(":")[0].trim();
                    logger.info("Отключение loop-устройства: " + loopDevice);
                    Process detach = Runtime.getRuntime().exec("sudo losetup -d " + loopDevice);
                    if (!detach.waitFor(30, TimeUnit.SECONDS) || detach.exitValue() != 0) {
                        throw new IOException("Ошибка при отключении loop-устройства: " + loopDevice);
                    }
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Монтирует зашифрованный контейнер.
     */
    public static void mountContainer(String containerPath, String name, String password, String mountPoint) throws IOException {
        String loopDevice = null;
        PointerByReference cdRef = new PointerByReference();
        Pointer cd = null;

        try {
            // 1. Проверяем существование контейнера
            File container = new File(containerPath);
            if (!container.exists()) {
                throw new IOException("Контейнер не найден: " + containerPath);
            }

            // 2. Создание loop-устройства
            Process losetup = Runtime.getRuntime().exec(String.format("sudo losetup -f --show %s", containerPath));
            loopDevice = readProcessOutput(losetup);
            if (loopDevice == null || loopDevice.isEmpty()) {
                throw new IOException("Не удалось создать loop-устройство");
            }
            loopDevice = loopDevice.trim();

            // 3. Инициализация cryptsetup
            int result = crypt.crypt_init(cdRef, loopDevice);
            if (result < 0) {
                throw new IOException("Ошибка при инициализации cryptsetup: " + result);
            }
            cd = cdRef.getValue();

            // 4. Загрузка заголовка LUKS
            result = crypt.crypt_load(cd, "LUKS2", null);
            if (result < 0) {
                throw new IOException("Ошибка при загрузке заголовка LUKS: " + result);
            }

            // 5. Активация контейнера
            String mappedName = "crypt_" + name;
            File device = new File("/dev/mapper/" + mappedName);

            if (device.exists()) {
                System.out.println("Устройство уже существует. Деактивируем...");
                int deactivateResult = crypt.crypt_deactivate(cd, mappedName);
                if (deactivateResult < 0) {
                    throw new IOException("Ошибка при деактивации существующего устройства: " + mappedName);
                }
            }

            System.out.println("Попытка активации устройства: " + mappedName);
            result = crypt.crypt_activate_by_passphrase(cd, mappedName, -1, password, password.length(), 0);
            if (result < 0) {
                String errorMessage = crypt.crypt_get_error();
                throw new IOException("Ошибка при активации контейнера: " + result + " (" + errorMessage + ")");
            }
            System.out.println("Устройство успешно активировано: " + mappedName);

            // 6. Монтирование файловой системы
            String devicePath = "/dev/mapper/" + mappedName;
            File mountDir = new File(mountPoint);

            if (!mountDir.exists()) {
                System.out.println("Создаем директорию для монтирования: " + mountPoint);
                if (!mountDir.mkdirs()) {
                    throw new IOException("Не удалось создать директорию для монтирования: " + mountPoint);
                }
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

        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        } finally {
            if (cd != null) {
                crypt.crypt_free(cd);
            }
            if (loopDevice != null) {
                try {
                    Runtime.getRuntime().exec(String.format("sudo losetup -d %s", loopDevice)).waitFor();
                } catch (Exception e) {
                    System.err.println("Ошибка при отсоединении loop-устройства: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Размонтирует зашифрованный контейнер.
     */
    public static void unmountContainer(String name, String mountPoint) throws IOException {
        String mappedName = "crypt_" + name;
        PointerByReference cdRef = new PointerByReference();
        Pointer cd = null;

        try {
            // 1. Размонтирование файловой системы
            Process umount = Runtime.getRuntime().exec(String.format("sudo umount %s", mountPoint));
            if (!umount.waitFor(30, TimeUnit.SECONDS) || umount.exitValue() != 0) {
                throw new IOException("Ошибка при размонтировании файловой системы");
            }

            // 2. Закрытие зашифрованного контейнера
            int result = crypt.crypt_init(cdRef, "/dev/mapper/" + mappedName);
            if (result < 0) {
                throw new IOException("Ошибка при инициализации cryptsetup: " + result);
            }
            cd = cdRef.getValue();

            result = crypt.crypt_deactivate(cd, mappedName);
            if (result < 0) {
                throw new IOException("Ошибка при деактивации контейнера: " + result);
            }

            logger.info("Контейнер успешно размонтирован");
        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        } finally {
            if (cd != null) {
                crypt.crypt_free(cd);
            }
        }
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
        String containersDir = "/home/maksimka/containers"; // Используйте правильный путь
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
}