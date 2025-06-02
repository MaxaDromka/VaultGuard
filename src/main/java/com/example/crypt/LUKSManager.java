package com.example.crypt;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.logging.Logger;
import java.util.logging.Level;

public class LUKSManager {
    private static final Logger logger = Logger.getLogger(LUKSManager.class.getName());
    private static final int LUKS_HEADER_SIZE = 4096; // Размер заголовка LUKS в байтах
    private static final byte[] LUKS_MAGIC = "LUKS".getBytes();

    /**
     * Проверка, является ли файл LUKS-контейнером
     */
    public static boolean isLUKSContainer(String containerPath) {
        try (FileInputStream fis = new FileInputStream(containerPath)) {
            byte[] header = new byte[LUKS_HEADER_SIZE];
            if (fis.read(header) != LUKS_HEADER_SIZE) {
                return false;
            }

            // Проверяем магическое число LUKS
            for (int i = 0; i < LUKS_MAGIC.length; i++) {
                if (header[i] != LUKS_MAGIC[i]) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Ошибка при проверке LUKS-контейнера", e);
            return false;
        }
    }

    /**
     * Создание LUKS-контейнера
     */
    public static void createContainer(String containerPath, long size, String password) 
            throws IOException {
        // Создаем файл нужного размера
        try (FileOutputStream fos = new FileOutputStream(containerPath)) {
            byte[] buffer = new byte[8192];
            long remaining = size;
            while (remaining > 0) {
                int toWrite = (int) Math.min(remaining, buffer.length);
                fos.write(buffer, 0, toWrite);
                remaining -= toWrite;
            }
        }

        // Инициализируем LUKS-заголовок
        initializeLUKSHeader(containerPath, password);
    }

    /**
     * Монтирование LUKS-контейнера
     */
    public static void mountContainer(String containerPath, String password, String mountPoint) 
            throws IOException {
        // Проверяем, является ли файл LUKS-контейнером
        if (!isLUKSContainer(containerPath)) {
            throw new IOException("Файл не является LUKS-контейнером");
        }

        // Проверяем пароль
        if (!verifyPassword(containerPath, password)) {
            throw new IOException("Неверный пароль");
        }

        // Создаем точку монтирования
        Files.createDirectories(Paths.get(mountPoint));
    }

    /**
     * Размонтирование LUKS-контейнера
     */
    public static void unmountContainer(String mountPoint) throws IOException {
    }

    /**
     * Инициализация LUKS-заголовка
     */
    private static void initializeLUKSHeader(String containerPath, String password) 
            throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(containerPath, "rw")) {
            // Записываем магическое число LUKS
            raf.write(LUKS_MAGIC);

            // Генерируем соль
            byte[] salt = generateSalt();
            raf.write(salt);

            // Генерируем ключ из пароля и соли
            byte[] key = deriveKey(password, salt);
            raf.write(key);
        }
    }

    /**
     * Проверка пароля
     */
    private static boolean verifyPassword(String containerPath, String password) 
            throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(containerPath, "r")) {
            // Пропускаем магическое число
            raf.seek(LUKS_MAGIC.length);

            // Читаем соль
            byte[] salt = new byte[32];
            raf.read(salt);

            // Генерируем ключ из пароля и соли
            byte[] key = deriveKey(password, salt);

            // Читаем сохраненный ключ
            byte[] storedKey = new byte[32];
            raf.read(storedKey);

            // Сравниваем ключи
            return MessageDigest.isEqual(key, storedKey);
        }
    }

    /**
     * Генерация соли
     */
    private static byte[] generateSalt() {
        byte[] salt = new byte[32];
        new java.security.SecureRandom().nextBytes(salt);
        return salt;
    }

    /**
     * Получение ключа из пароля и соли
     */
    private static byte[] deriveKey(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(password.getBytes());
            digest.update(salt);
            return digest.digest();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении ключа", e);
        }
    }
} 