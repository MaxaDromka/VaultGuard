package com.example.crypt;

import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import java.util.logging.Logger;

import java.io.IOException;

public class EncryptionManager {
    private static final CryptSetup crypt = CryptSetup.load();
    private static final Runtime runtime = Runtime.getRuntime(crypt);

    //private static final Logger logger = Logger.getLogger(EncryptionManager.class.getName());

    static {
        try {
            System.loadLibrary("cryptsetup");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Ошибка: libcryptsetup не найдена. Установите её с помощью 'sudo install libcryptsetup-dev'");
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
    ) {
        Pointer cd = runtime.getMemoryManager().allocateTemporary(8, true); // Указатель на crypt_device

        try {
            // Инициализация контейнера
            int result = crypt.crypt_init(cd, path);
            if (result < 0) {
                throw new IOException("Ошибка при инициализации: " + result);
            }

            // Форматирование контейнера
            result = crypt.crypt_format(cd, 2, algorithm, null, null, 0);
            if (result < 0) {
                throw new IOException("Ошибка при форматировании: " + result);
            }

            // Добавление ключа
            result = crypt.crypt_keyslot_add_by_volume_key(cd, -1, null, 0, password, password.length());
            if (result < 0) {
                throw new IOException("Ошибка при добавлении ключа: " + result);
            }

            // Создание файловой системы
            createFileSystem(name, fsType);

            System.out.println("Контейнер успешно создан: " + path);
        } catch (IOException e) {
            System.err.println("Ошибка: " + e.getMessage());
        } finally {
            crypt.crypt_free(cd); // Освобождаем ресурсы
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