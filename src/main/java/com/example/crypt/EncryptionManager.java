package com.example.crypt;

import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class EncryptionManager {
    private static final CryptSetup crypt = CryptSetup.load();
    private static final Runtime runtime = Runtime.getSystemRuntime();
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
            String path,
            int sizeMB,
            String name,
            String algorithm,
            String password,
            String fsType
    ) throws IOException {
        //String containerPath = path + File.separator + name + ".container";
        //String containerPath = System.getProperty("user.home") + File.separator + name + ".container";
        String userHome = System.getProperty("user.home");
        String containerPath = userHome + File.separator + name + ".container";
        String loopDevice = null;
        Pointer cd = null;

        try {
            // 1. Создание файла-контейнера
            logger.info("Создание файла-контейнера размером " + sizeMB + "MB");
            logger.info("Путь к контейнеру: " + containerPath);
            logger.info("Алгоритм шифрования: " + algorithm);
            Process truncate = java.lang.Runtime.getRuntime().exec(
                String.format("truncate -s %dM %s", sizeMB, containerPath)
            );
            if (truncate.waitFor(30, TimeUnit.SECONDS) && truncate.exitValue() != 0) {
                throw new IOException("Ошибка при создании файла-контейнера");
            }

            // 2. Создание loop-устройства
            logger.info("Создание loop-устройства");
            Process losetup = java.lang.Runtime.getRuntime().exec(
                String.format("losetup -f", containerPath)
            );
            loopDevice = readProcessOutput(losetup).trim();
            if (loopDevice.isEmpty()) {
                throw new IOException("Не удалось создать loop-устройство");
            }

            // 3. Инициализация шифрования
            logger.info("Инициализация шифрования");
            cd = runtime.getMemoryManager().allocateTemporary(java.lang.Runtime.getRuntime().availableProcessors() * 8, true);
            if (cd == null) {
                throw new IOException("Ошибка выделения памяти");
            }

            int result = crypt.crypt_init(cd, loopDevice);
            if (result < 0) {
                throw new IOException("Ошибка при инициализации: " + result);
            }

            // 4. Форматирование LUKS
            logger.info("Форматирование LUKS");
            result = crypt.crypt_format(cd, 2, algorithm, "xts-plain64", null, 512/8);
            if (result < 0) {
                throw new IOException("Ошибка при форматировании: " + result);
            }

            // 5. Добавление ключа
            logger.info("Добавление ключа шифрования");
            result = crypt.crypt_keyslot_add_by_volume_key(cd, -1, null, 0, password, password.length());
            if (result < 0) {
                throw new IOException("Ошибка при добавлении ключа: " + result);
            }

            // 6. Открытие устройства
            logger.info("Открытие зашифрованного устройства");
            result = crypt.crypt_activate_by_passphrase(cd, name, -1, password, password.length(), 0);
            if (result < 0) {
                throw new IOException("Ошибка при активации: " + result);
            }

            // 7. Создание файловой системы
            logger.info("Создание файловой системы");
            createFileSystem(name, fsType);

            logger.info("Контейнер успешно создан: " + containerPath);
        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        } finally {
            // Освобождение ресурсов
            if (cd != null) {
                crypt.crypt_free(cd);
            }
        }
    }

    private static String readProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            return reader.readLine();
        }
    }

    /**
     * Монтирует зашифрованный контейнер.
     */
    public static void mountContainer(String containerPath, String name, String password, String mountPoint) throws IOException {
        String loopDevice = null;
        Pointer cd = null;

        try {
            // 1. Создание loop-устройства
            logger.info("Создание loop-устройства для " + containerPath);
            Process losetup = java.lang.Runtime.getRuntime().exec(
                String.format("losetup -f --show %s", containerPath)
            );
            loopDevice = readProcessOutput(losetup).trim();
            if (loopDevice.isEmpty()) {
                throw new IOException("Не удалось создать loop-устройство");
            }

            // 2. Инициализация cryptsetup
            logger.info("Инициализация cryptsetup");
            cd = runtime.getMemoryManager().allocateTemporary(java.lang.Runtime.getRuntime().availableProcessors() * 8, true);
            int result = crypt.crypt_init(cd, loopDevice);
            if (result < 0) {
                throw new IOException("Ошибка при инициализации: " + result);
            }

            // 3. Загрузка заголовка LUKS
            logger.info("Загрузка заголовка LUKS");
            result = crypt.crypt_load(cd, 0, null);
            if (result < 0) {
                throw new IOException("Ошибка при загрузке заголовка: " + result);
            }

            // 4. Активация контейнера
            logger.info("Активация контейнера");
            String mappedName = "crypt_" + name;
            result = crypt.crypt_activate_by_passphrase(cd, mappedName, -1, password, password.length(), 0);
            if (result < 0) {
                throw new IOException("Ошибка при активации: " + result);
            }

            // 5. Монтирование файловой системы
            logger.info("Монтирование файловой системы");
            String devicePath = "/dev/mapper/" + mappedName;
            Process mount = java.lang.Runtime.getRuntime().exec(
                String.format("mount %s %s", devicePath, mountPoint)
            );
            if (mount.waitFor(30, TimeUnit.SECONDS) && mount.exitValue() != 0) {
                throw new IOException("Ошибка при монтировании файловой системы");
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
        Pointer cd = null;

        try {
            // 1. Размонтирование файловой системы
            logger.info("Размонтирование файловой системы из " + mountPoint);
            Process umount = java.lang.Runtime.getRuntime().exec(
                String.format("umount %s", mountPoint)
            );
            if (umount.waitFor(30, TimeUnit.SECONDS) && umount.exitValue() != 0) {
                throw new IOException("Ошибка при размонтировании файловой системы");
            }

            // 2. Закрытие зашифрованного контейнера
            logger.info("Закрытие зашифрованного контейнера");
            cd = runtime.getMemoryManager().allocateTemporary(java.lang.Runtime.getRuntime().availableProcessors() * 8, true);
            int result = crypt.crypt_init_by_name(cd, mappedName);
            if (result < 0) {
                throw new IOException("Ошибка при инициализации: " + result);
            }

            result = crypt.crypt_deactivate(cd, mappedName);
            if (result < 0) {
                throw new IOException("Ошибка при деактивации контейнера: " + result);
            }

            // 3. Отключение loop-устройства
            logger.info("Отключение loop-устройства");
            Process losetup = java.lang.Runtime.getRuntime().exec(
                "losetup -d $(losetup -j " + name + ".container | cut -d':' -f1)"
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

    /**
     * Создает файловую систему внутри контейнера.
     */
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
            if (exitCode != 0) {
                throw new IOException("Ошибка при создании файловой системы: " + exitCode);
            }
        } catch (InterruptedException e) {
            throw new IOException("Процесс прерван: " + e.getMessage());
        }
    }
}