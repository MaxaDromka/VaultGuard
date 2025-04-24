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
            // 1. Создание файла-контейнера
            Process truncate = Runtime.getRuntime().exec(
                    String.format("sudo truncate -s %dM %s", sizeMB, containerPath)
            );
            if (!truncate.waitFor(30, TimeUnit.SECONDS) || truncate.exitValue() != 0) {
                throw new IOException("Ошибка при создании файла контейнера");
            }

            // 2. Создание loop-устройства
            Process losetup = Runtime.getRuntime().exec(
                    String.format("sudo losetup -f --show %s", containerPath)
            );
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

            // 4. Форматирование LUKS2
            result = crypt.crypt_format(
                    cd,
                    "LUKS2",
                    "aes",
                    "xts-plain64",
                    null,
                    null,
                    64,
                    null
            );
            if (result < 0) {
                throw new IOException("Ошибка при форматировании LUKS2: " + result);
            }

            // 5. Добавление ключа
            result = crypt.crypt_keyslot_add_by_volume_key(
                    cd,
                    -1,
                    null,
                    0,
                    password,
                    password.length()
            );
            if (result < 0) {
                throw new IOException("Ошибка при добавлении ключа: " + result);
            }
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
            Process losetup = Runtime.getRuntime().exec(
                    String.format("sudo losetup -f --show %s", containerPath)
            );
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
            result = crypt.crypt_activate_by_passphrase(
                    cd,
                    mappedName,
                    -1,
                    password,
                    password.length(),
                    0
            );
            /*if (result < 0) {
                throw new IOException("Ошибка при активации контейнера: " + result);
            }*/

            // 6. Монтирование файловой системы
            String devicePath = "/dev/mapper/" + mappedName;
            File device = new File(devicePath);
            if (!device.exists()) {
                throw new IOException("Устройство не найдено: " + devicePath);
            }

            File mountDir = new File(mountPoint);
            if (!mountDir.exists()) {
                mountDir.mkdirs();
            }

            Process mount = Runtime.getRuntime().exec(
                    String.format("sudo mount %s %s", devicePath, mountPoint)
            );
            if (!mount.waitFor(30, TimeUnit.SECONDS) || mount.exitValue() != 0) {
                throw new IOException("Ошибка при монтировании файловой системы");
            }
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
            Process umount = Runtime.getRuntime().exec(
                    String.format("sudo umount %s", mountPoint)
            );
            if (!umount.waitFor(30, TimeUnit.SECONDS) || umount.exitValue() != 0) {
                throw new IOException("Ошибка при размонтировании файловой системы");
            }

            // 2. Закрытие зашифрованного контейнера
            int result = crypt.crypt_init_by_name(cdRef, mappedName);
            if (result < 0) {
                throw new IOException("Ошибка при инициализации: " + result);
            }
            cd = cdRef.getValue();

            result = crypt.crypt_deactivate(cd, mappedName);
            if (result < 0) {
                throw new IOException("Ошибка при деактивации контейнера: " + result);
            }
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
        String containersDir = "/home";
        File containerDir = new File(containersDir);

        if (containerDir.exists() && containerDir.isDirectory()) {
            File[] files = containerDir.listFiles((dir, name) -> name.startsWith(".maksimka"));
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

            result = crypt.crypt_load(cd, "LUKS2", null);
            if (result != 0) {
                return "Неизвестный";
            }

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