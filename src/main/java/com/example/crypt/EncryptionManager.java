package com.example.crypt;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.byref.PointerByReference;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class EncryptionManager {
    private static final CryptSetup crypt = CryptSetup.load();
    private static final Runtime runtime = Runtime.getRuntime(crypt);
    private static final Logger logger = Logger.getLogger(EncryptionManager.class.getName());

    static {
        try {
            System.loadLibrary("cryptsetup");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Ошибка: cryptsetup-devel не найдена. Установите её с помощью 'sudo dnf install cryptsetup-devel'");
            System.exit(1);
        }
    }

    private static String convertAlgorithmFormat(String algorithm) {
        switch (algorithm) {
            case "AES-256 (XTS)":
                return "aes-xts-plain64";
            case "Serpent (XTS)":
                return "serpent-xts-plain64";
            case "Twofish (XTS)":
                return "twofish-xts-plain64";
            case "AES-Twofish (XTS)":
                return "aes-twofish-xts-plain64";
            case "AES-Twofish-Serpent (XTS)":
                return "aes-twofish-serpent-xts-plain64";
            default:
                throw new IllegalArgumentException("Неподдерживаемый алгоритм шифрования: " + algorithm);
        }
    }

    private static void checkCipherSupport(String cipher) throws IOException {
        try {
            Process process = java.lang.Runtime.getRuntime().exec("cryptsetup benchmark");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean supported = false;

            while ((line = reader.readLine()) != null) {
                if (line.contains(cipher.split("-")[0])) { // Проверяем только базовый шифр
                    supported = true;
                    break;
                }
            }

            if (!supported) {
                throw new IOException("Шифр " + cipher + " не поддерживается в системе");
            }
        } catch (IOException e) {
            throw new IOException("Не удалось проверить поддержку шифров: " + e.getMessage());
        }
    }

    /**
     * Создает зашифрованный контейнер.
     */
    public static void createContainer(
            String path,
            int sizeMB,
            String name,
            String algorithm,
            String password,
            String fsType
    ) throws IOException {
        String containerPath = "/home/.maksimka";
        File containerDir = new File("/root/containers");
        if (!containerDir.exists()) {
            containerDir.mkdirs();
        }

        PointerByReference cdRef = new PointerByReference();
        Pointer cd = null;
        String loopDevice = null;

        try {
            // 1. Создание файла-контейнера
            logger.info("Создание файла-контейнера размером " + sizeMB + "MB");
            Process truncate = java.lang.Runtime.getRuntime().exec(
                    String.format("sudo truncate -s %dM %s", sizeMB, containerPath)
            );
            if (truncate.waitFor(30, TimeUnit.SECONDS) && truncate.exitValue() != 0) {
                throw new IOException("Ошибка при создании файла контейнера");
            }

            // Проверка размера файла
            File container = new File(containerPath);
            if (!container.exists() || container.length() != sizeMB * 1024L * 1024L) {
                throw new IOException("Файл контейнера не был создан или имеет неверный размер");
            }

            // 2. Создание loop-устройства
            logger.info("Создание loop-устройства");
            Process losetup = java.lang.Runtime.getRuntime().exec(
                    String.format("sudo losetup -f --show %s", containerPath)
            );
            loopDevice = readProcessOutput(losetup);
            if (loopDevice == null || loopDevice.isEmpty()) {
                throw new IOException("Не удалось создать loop-устройство");
            }
            loopDevice = loopDevice.trim();
            logger.info("Создано loop-устройство: " + loopDevice);

            // 3. Инициализация cryptsetup
            logger.info("Инициализация cryptsetup");
            int result = crypt.crypt_init(cdRef, loopDevice);
            if (result < 0) {
                throw new IOException("Ошибка при инициализации cryptsetup: " + result);
            }
            cd = cdRef.getValue();

            // 4. Форматирование LUKS2
            logger.info("Форматирование LUKS2");
            String cipher = "aes";            // Имя шифра
            String cipherMode = "xts-plain64";// Режим шифра
            int keySizeBytes = 64;           // Размер ключа в байтах (512 бит)
            result = crypt.crypt_format(
                    cd,
                    "LUKS2",                  // тип устройства
                    cipher,                   // имя шифра
                    cipherMode,               // режим шифра
                    null,                     // UUID (автогенерация)
                    null,                     // volumeKey (используется внутренний ключ)
                    keySizeBytes,             // размер ключа в байтах
                    null                      // params
            );
            if (result < 0) {
                String errorMessage = crypt.crypt_get_error(); // Получение последней ошибки
                throw new IOException("Ошибка при форматировании LUKS2: " + result + " (" + errorMessage + ")");
            }

            // 5. Добавление ключа
            logger.info("Добавление ключа шифрования");
            result = crypt.crypt_keyslot_add_by_volume_key(
                    cd,
                    -1,                       // CRYPT_ANY_SLOT
                    null,                     // volumeKey (используется внутренний ключ)
                    0,                        // размер ключа (не используется)
                    password,                 // пароль
                    password.length()         // длина пароля
            );

            if (result < 0) {
                String errorMessage = crypt.crypt_get_error();
                throw new IOException("Ошибка при добавлении ключа: " + result + " (" + errorMessage + ")");
            }
            logger.info("Ключ шифрования добавлен");

        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        } finally {
            // Отсоединяем loop-устройство
            if (loopDevice != null) {
                try {
                    Process losetup = java.lang.Runtime.getRuntime().exec(
                            String.format("sudo losetup -d %s", loopDevice)
                    );
                    losetup.waitFor(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.warning("Ошибка при отсоединении loop-устройства: " + e.getMessage());
                }
            }
            if (cd != null) {
                crypt.crypt_free(cd);
            }
        }
        logger.info("Успех");
    }
    private static String readProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String output = reader.readLine();
            if (output == null) {
                // Если вывод пустой, проверим stderr
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
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
            logger.info("Создание loop-устройства для " + containerPath);
            Process losetup = java.lang.Runtime.getRuntime().exec(
                    String.format("sudo losetup -f --show %s", containerPath)
            );
            loopDevice = readProcessOutput(losetup);
            if (loopDevice == null || loopDevice.isEmpty()) {
                throw new IOException("Не удалось создать loop-устройство");
            }
            loopDevice = loopDevice.trim();
            logger.info("Создано loop-устройство: " + loopDevice);

            // 3. Инициализация cryptsetup
            logger.info("Инициализация cryptsetup");
            int result = crypt.crypt_init(cdRef, loopDevice);
            if (result < 0) {
                throw new IOException("Ошибка при инициализации cryptsetup: " + result);
            }
            cd = cdRef.getValue();

            // 4. Загрузка заголовка LUKS
            logger.info("Загрузка заголовка LUKS");
            result = crypt.crypt_load(cd, "LUKS2", null); // Указываем тип устройства
            if (result < 0) {
                throw new IOException("Ошибка при загрузке заголовка LUKS: " + result);
            }

            // 5. Активация контейнера
            logger.info("Активация контейнера");
            String mappedName = "crypt_" + name;
            result = crypt.crypt_activate_by_passphrase(
                    cd,
                    mappedName,         // имя устройства
                    -1,                 // CRYPT_ANY_SLOT
                    password,           // пароль
                    password.length(),  // длина пароля
                    0                   // флаги
            );
            if (result < 0) {
                throw new IOException("Ошибка при активации контейнера: " + result);
            }
            logger.info("Контейнер активирован как: " + mappedName);

            // 6. Монтирование файловой системы
            logger.info("Монтирование файловой системы");
            String devicePath = "/dev/mapper/" + mappedName;

            // Проверяем, что устройство существует
            File device = new File(devicePath);
            if (!device.exists()) {
                throw new IOException("Устройство не найдено: " + devicePath);
            }

            // Создаем точку монтирования, если не существует
            File mountDir = new File(mountPoint);
            if (!mountDir.exists()) {
                mountDir.mkdirs();
            }

            Process mount = java.lang.Runtime.getRuntime().exec(
                    String.format("sudo mount %s %s", devicePath, mountPoint)
            );
            if (mount.waitFor(30, TimeUnit.SECONDS) && mount.exitValue() != 0) {
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(mount.getErrorStream()))) {
                    String error = errorReader.readLine();
                    if (error != null) {
                        throw new IOException("Ошибка при монтировании: " + error);
                    }
                }
                throw new IOException("Ошибка при монтировании файловой системы");
            }

            // 7. Изменение прав на файловой системе
            logger.info("Изменение прав на файловой системе");
            Process chown = java.lang.Runtime.getRuntime().exec(
                    String.format("sudo chown -R %s:%s %s", System.getProperty("user.name"), System.getProperty("user.name"), mountPoint)
            );
            if (chown.waitFor(30, TimeUnit.SECONDS) && chown.exitValue() != 0) {
                throw new IOException("Ошибка при изменении прав");
            }

            logger.info("Контейнер успешно смонтирован в " + mountPoint);
        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        } finally {
            if (cd != null) {
                crypt.crypt_free(cd);
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
            logger.info("Размонтирование файловой системы из " + mountPoint);
            Process umount = java.lang.Runtime.getRuntime().exec(
                    String.format("sudo umount %s", mountPoint)
            );
            if (umount.waitFor(30, TimeUnit.SECONDS) && umount.exitValue() != 0) {
                throw new IOException("Ошибка при размонтировании файловой системы");
            }

            // 2. Закрытие зашифрованного контейнера
            logger.info("Закрытие зашифрованного контейнера");
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
            logger.info("Отключение loop-устройства");
            Process losetup = java.lang.Runtime.getRuntime().exec(
                    "sudo losetup -d $(losetup -j /home/.maksimka | cut -d':' -f1)"
            );
            if (losetup.waitFor(30, TimeUnit.SECONDS) && losetup.exitValue() != 0) {
                throw new IOException("Ошибка при отключении loop-устройства");
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

    private static void createFileSystem(String name, String fsType) throws IOException {
        String devicePath = "/dev/mapper/crypt_" + name;
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

        ProcessBuilder processBuilder = new ProcessBuilder(mkfsCommand);
        Process process = processBuilder.start();
        try {
            int exitCode = process.waitFor();
            /*if (exitCode != 0) {
                throw new IOException("Ошибка при создании файловой системы: " + exitCode);
            }*/
        } catch (InterruptedException e) {
            throw new IOException("Процесс прерван: " + e.getMessage());
        }
    }

    public static ObservableList<Partition> getContainersList() {
        ObservableList<Partition> containers = FXCollections.observableArrayList();
        String containersDir = "/root/containers";
        File containerDir = new File(containersDir);

        if (containerDir.exists() && containerDir.isDirectory()) {
            File[] files = containerDir.listFiles((dir, name) -> name.endsWith(".container"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().replace(".container", "");
                    long size = file.length() / (1024 * 1024); // Размер в MB
                    String algorithm = getContainerAlgorithm(file.getAbsolutePath());
                    containers.add(new Partition(
                            name,
                            file.getAbsolutePath(),
                            String.valueOf(size),
                            algorithm
                    ));
                }
            }
        }
        return containers;
    }

    private static String getContainerAlgorithm(String containerPath) {
        PointerByReference cdRef = new PointerByReference();
        Pointer cd = null;
        try {
            int result = crypt.crypt_init(cdRef, containerPath);
            if (result != 0) {
                return "Неизвестный";
            }
            cd = cdRef.getValue();

            //result = crypt.crypt_load(cd, 0, null);
            if (result != 0) {
                return "Неизвестный";
            }

            // Получаем информацию о шифре
            String cipher = crypt.crypt_get_cipher(cd);
            String cipherMode = crypt.crypt_get_cipher_mode(cd);

            if (cipher != null && cipherMode != null) {
                return cipher.toUpperCase() + " (" + cipherMode.toUpperCase() + ")";
            }
            return "Неизвестный";
        } catch (Exception e) {
            logger.warning("Ошибка при определении алгоритма: " + e.getMessage());
            return "Неизвестный";
        } finally {
            if (cd != null) {
                crypt.crypt_free(cd);
            }
        }
    }
}