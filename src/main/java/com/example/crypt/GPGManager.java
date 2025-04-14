package com.example.crypt;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GPGManager {
    private static final Logger logger = Logger.getLogger(GPGManager.class.getName());

    /**
     * Генерация ключевой пары GPG
     */
    public static void generateKeyPair(String name, String email, String password) throws IOException {
        // Создаем файл с параметрами для генерации ключа
        File batchFile = File.createTempFile("gpg-batch", ".txt");
        try (PrintWriter writer = new PrintWriter(batchFile)) {
            writer.println("Key-Type: RSA");
            writer.println("Key-Length: 3072");
            writer.println("Subkey-Type: RSA");
            writer.println("Subkey-Length: 3072");
            writer.println("Name-Real: " + name);
            writer.println("Name-Email: " + email);
            writer.println("Expire-Date: 0");
            writer.println("Passphrase: " + password);
            writer.println("%commit");
        }

        // Запускаем генерацию ключа
        ProcessBuilder pb = new ProcessBuilder(
            "gpg", "--batch", "--generate-key", batchFile.getAbsolutePath()
        );
        Process process = pb.start();
        
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Ошибка при генерации ключа: " + exitCode);
            }
            logger.info("Ключевая пара успешно создана для " + email);
        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        } finally {
            batchFile.delete();
        }
    }

    /**
     * Шифрование файла
     */
    public static void encryptFile(String sourceFile, String recipientKey) throws IOException {
        String outputFile = sourceFile + ".gpg";
        
        ProcessBuilder pb = new ProcessBuilder(
            "gpg", "--encrypt", "--recipient", recipientKey, 
            "--output", outputFile, sourceFile
        );
        Process process = pb.start();
        
        try {
            int exitCode = process.waitFor();
            /*if (exitCode != 0) {
                throw new IOException("Ошибка при шифровании файла: " + exitCode);
            }*/
            logger.info("Файл успешно зашифрован: " + outputFile);
        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        }
    }

    /**
     * Расшифровка файла
     */
    public static void decryptFile(String encryptedFile, String password) throws IOException {
        String outputFile = encryptedFile.replace(".gpg", "");
        
        ProcessBuilder pb = new ProcessBuilder(
            "gpg", "--decrypt", "--passphrase", password,
            "--output", outputFile, encryptedFile
        );
        Process process = pb.start();
        
        try {
            int exitCode = process.waitFor();
            /*if (exitCode != 0) {
                throw new IOException("Ошибка при расшифровке файла: " + exitCode);
            }*/
            logger.info("Файл успешно расшифрован: " + outputFile);
        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        }
    }

    /**
     * Экспорт публичного ключа
     */
    public static void exportPublicKey(String keyId, String outputFile) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
            "gpg", "--export", "--armor", "--output", outputFile, keyId
        );
        Process process = pb.start();
        
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Ошибка при экспорте ключа: " + exitCode);
            }
            logger.info("Публичный ключ экспортирован в " + outputFile);
        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        }
    }

    /**
     * Импорт публичного ключа
     */
    public static void importPublicKey(String keyFile) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
            "gpg", "--import", keyFile
        );
        Process process = pb.start();
        
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Ошибка при импорте ключа: " + exitCode);
            }
            logger.info("Публичный ключ импортирован из " + keyFile);
        } catch (InterruptedException e) {
            throw new IOException("Процесс был прерван", e);
        }
    }

    /**
     * Получение списка ключей
     */
    public static List<GPGKey> listKeys() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
            "gpg", "--list-keys", "--with-colons"
        );
        Process process = pb.start();
        
        List<GPGKey> keys = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            
            String line;
            GPGKey currentKey = null;
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts[0].equals("pub")) {
                    // Новый ключ
                    currentKey = new GPGKey();
                    currentKey.setId(parts[4]);
                    keys.add(currentKey);
                } else if (parts[0].equals("uid") && currentKey != null) {
                    // Информация о пользователе
                    String info = parts[9];
                    int emailStart = info.indexOf('<');
                    int emailEnd = info.indexOf('>');
                    if (emailStart > 0 && emailEnd > emailStart) {
                        currentKey.setName(info.substring(0, emailStart).trim());
                        currentKey.setEmail(info.substring(emailStart + 1, emailEnd));
                    }
                }
            }
        }
        
        return keys;
    }

    /**
     * Класс для хранения информации о ключе GPG
     */
    public static class GPGKey {
        private String id;
        private String name;
        private String email;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        @Override
        public String toString() {
            return name + " <" + email + ">";
        }
    }
} 