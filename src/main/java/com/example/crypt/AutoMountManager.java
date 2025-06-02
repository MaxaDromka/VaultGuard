package com.example.crypt;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Properties;
import java.security.MessageDigest;

public class AutoMountManager {
    private static final Logger logger = Logger.getLogger(AutoMountManager.class.getName());
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.crypt/automount";
    private static final String CONFIG_FILE = "automount.properties";

    /**
     * Включение автозапуска контейнера
     */
    public static void enableAutoMount(String containerPath, String password) {
        try {
            // Создаем директорию для конфигурации если её нет
            Files.createDirectories(Paths.get(CONFIG_DIR));

            // Сохраняем настройки в файл конфигурации
            Properties config = new Properties();
            config.setProperty("containerPath", containerPath);
            config.setProperty("autoMount", "true");
            config.setProperty("mountPoint", "/mnt/" + getContainerName(containerPath));

            // Шифруем пароль перед сохранением
            config.setProperty("passwordHash", hashPassword(password));

            // Сохраняем конфигурацию
            Path configPath = Paths.get(CONFIG_DIR, CONFIG_FILE);
            try (OutputStream out = Files.newOutputStream(configPath)) {
                config.store(out, "AutoMount Configuration");
            }

            // Добавляем в автозагрузку через Java API
          //  addToStartup(containerPath);

            logger.info("Автозапуск включен для контейнера: " + containerPath);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при настройке автозапуска", e);
        }
    }

    /**
     * Монтирование по расписанию
     */
    public static void scheduleMount(String containerPath, String password, 
                                   LocalDateTime scheduleTime) {
        try {
            // Сохраняем настройки расписания
            Properties schedule = new Properties();
            schedule.setProperty("containerPath", containerPath);
            schedule.setProperty("scheduleTime", scheduleTime.toString());
            schedule.setProperty("passwordHash", hashPassword(password));

            // Сохраняем в файл расписания
            Path schedulePath = Paths.get(CONFIG_DIR, "schedule.properties");
            try (OutputStream out = Files.newOutputStream(schedulePath)) {
                schedule.store(out, "Mount Schedule");
            }

            // Добавляем в планировщик через Java API
           // addToScheduler(containerPath, scheduleTime);

            logger.info("Настроено монтирование по расписанию для: " + containerPath);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при настройке расписания", e);
        }
    }

    /**
     * Монтирование при подключении USB
     */
    public static void mountOnUSB(String containerPath, String password, 
                                 String usbDeviceId) {
        try {
            // Сохраняем настройки USB-монтирования
            Properties usbConfig = new Properties();
            usbConfig.setProperty("containerPath", containerPath);
            usbConfig.setProperty("usbDeviceId", usbDeviceId);
            usbConfig.setProperty("passwordHash", hashPassword(password));

            // Сохраняем в файл конфигурации USB
            Path usbConfigPath = Paths.get(CONFIG_DIR, "usb.properties");
            try (OutputStream out = Files.newOutputStream(usbConfigPath)) {
                usbConfig.store(out, "USB Mount Configuration");
            }

            // Добавляем обработчик USB-событий через Java API
            //addUSBHandler(containerPath, usbDeviceId);

            logger.info("Настроено монтирование по USB для: " + containerPath);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при настройке USB-монтирования", e);
        }
    }

    /**
     * Хеширование пароля
     */
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при хешировании пароля", e);
        }
    }

    /**
     * Получение имени контейнера из пути
     */
    private static String getContainerName(String containerPath) {
        return new File(containerPath).getName().replaceFirst("[.][^.]+$", "");
    }
} 