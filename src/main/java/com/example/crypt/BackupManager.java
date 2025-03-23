package com.example.crypt;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackupManager {
    private static final Logger logger = Logger.getLogger(BackupManager.class.getName());
    private static final String BACKUP_DIR = System.getProperty("user.home") + "/.crypt/backups";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Создание резервной копии контейнера
     */
    public static void backupContainer(String containerPath) {
        try {
            // Создаем директорию для резервных копий если её нет
            Files.createDirectories(Paths.get(BACKUP_DIR));

            // Формируем имя файла резервной копии
            String containerName = new File(containerPath).getName();
            String backupFileName = containerName + "_" + 
                                  LocalDateTime.now().format(DATE_FORMATTER) + ".backup";
            Path backupPath = Paths.get(BACKUP_DIR, backupFileName);

            // Копируем файл контейнера с использованием буферизированного потока
            try (BufferedInputStream bis = new BufferedInputStream(
                    Files.newInputStream(Paths.get(containerPath)));
                 BufferedOutputStream bos = new BufferedOutputStream(
                    Files.newOutputStream(backupPath))) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                bos.flush();
            }

            // Создаем метаданные резервной копии
            createBackupMetadata(containerPath, backupPath);

            logger.info("Создана резервная копия: " + backupPath);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при создании резервной копии", e);
        }
    }

    /**
     * Восстановление контейнера из резервной копии
     */
    public static void restoreContainer(String backupPath, String targetPath) {
        try {
            // Проверяем существование резервной копии
            if (!Files.exists(Paths.get(backupPath))) {
                throw new FileNotFoundException("Резервная копия не найдена: " + backupPath);
            }

            // Копируем файл из резервной копии с использованием буферизированного потока
            try (BufferedInputStream bis = new BufferedInputStream(
                    Files.newInputStream(Paths.get(backupPath)));
                 BufferedOutputStream bos = new BufferedOutputStream(
                    Files.newOutputStream(Paths.get(targetPath)))) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                bos.flush();
            }

            // Проверяем целостность восстановленного контейнера
            verifyContainerIntegrity(targetPath);

            logger.info("Контейнер восстановлен из резервной копии: " + backupPath);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при восстановлении контейнера", e);
        }
    }

    /**
     * Настройка расписания резервного копирования
     */
    public static void scheduleBackup(String containerPath, LocalDateTime scheduleTime) {
        try {
            // Сохраняем настройки расписания
            Properties schedule = new Properties();
            schedule.setProperty("containerPath", containerPath);
            schedule.setProperty("scheduleTime", scheduleTime.toString());

            // Сохраняем в файл расписания
            Path schedulePath = Paths.get(BACKUP_DIR, "schedule.properties");
            try (OutputStream out = Files.newOutputStream(schedulePath)) {
                schedule.store(out, "Backup Schedule");
            }

            // Вычисляем задержку до следующего выполнения
            long delay = calculateDelay(scheduleTime);
            
            // Планируем задачу
            scheduler.scheduleAtFixedRate(
                () -> backupContainer(containerPath),
                delay,
                24 * 60 * 60 * 1000, // 24 часа
                TimeUnit.MILLISECONDS
            );

            logger.info("Настроено резервное копирование по расписанию для: " + containerPath);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при настройке расписания", e);
        }
    }

    /**
     * Создание метаданных резервной копии
     */
    private static void createBackupMetadata(String containerPath, Path backupPath) 
            throws IOException {
        Properties metadata = new Properties();
        metadata.setProperty("containerPath", containerPath);
        metadata.setProperty("backupDate", LocalDateTime.now().toString());
        metadata.setProperty("backupSize", String.valueOf(Files.size(backupPath)));
        metadata.setProperty("checksum", calculateChecksum(backupPath));

        // Сохраняем метаданные
        Path metadataPath = backupPath.resolveSibling(backupPath.getFileName() + ".meta");
        try (OutputStream out = Files.newOutputStream(metadataPath)) {
            metadata.store(out, "Backup Metadata");
        }
    }

    /**
     * Проверка целостности контейнера
     */
    private static void verifyContainerIntegrity(String containerPath) 
            throws IOException {
        if (!LUKSManager.isLUKSContainer(containerPath)) {
            throw new IOException("Контейнер поврежден или не является LUKS");
        }
    }

    /**
     * Вычисление контрольной суммы файла
     */
    private static String calculateChecksum(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            try (InputStream is = Files.newInputStream(file)) {
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IOException("Ошибка при вычислении контрольной суммы", e);
        }
    }

    /**
     * Вычисление задержки до следующего выполнения
     */
    private static long calculateDelay(LocalDateTime scheduleTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = scheduleTime;
        
        // Если время уже прошло, планируем на следующий день
        if (nextRun.isBefore(now)) {
            nextRun = nextRun.plusDays(1);
        }
        
        return java.time.Duration.between(now, nextRun).toMillis();
    }
} 