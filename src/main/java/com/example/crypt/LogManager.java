package com.example.crypt;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.*;
import java.util.stream.Collectors;

public class LogManager {
    private static final Logger logger = Logger.getLogger(LogManager.class.getName());
    private static final String LOG_FILE = "crypt_manager.log";
    private static final String AUDIT_LOG_FILE = "crypt_audit.log";
    
    static {
        try {
            // Создаем директорию для логов, если её нет
            new File("logs").mkdirs();
            
            // Настройка основного логгера
            FileHandler fileHandler = new FileHandler("logs/" + LOG_FILE, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            
            // Настройка аудит-логгера
            Logger auditLogger = Logger.getLogger("AuditLogger");
            FileHandler auditFileHandler = new FileHandler("logs/" + AUDIT_LOG_FILE, true);
            auditFileHandler.setFormatter(new SimpleFormatter());
            auditLogger.addHandler(auditFileHandler);
            
            // Добавляем вывод в консоль
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(consoleHandler);
            auditLogger.addHandler(consoleHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void logInfo(String message) {
        logger.info(message);
    }
    
    public static void logWarning(String message) {
        logger.warning(message);
    }
    
    public static void logError(String message, Throwable thrown) {
        logger.log(Level.SEVERE, message, thrown);
    }
    
    public static void logAudit(String action, String user, String details) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String auditMessage = String.format("[%s] User: %s, Action: %s, Details: %s", 
            timestamp, user, action, details);
        Logger.getLogger("AuditLogger").info(auditMessage);
    }
    
    public static void logContainerAccess(String containerName, String action, boolean success) {
        String status = success ? "SUCCESS" : "FAILED";
        logAudit("CONTAINER_ACCESS", System.getProperty("user.name"), 
            String.format("Container: %s, Action: %s, Status: %s", containerName, action, status));
    }
    
    public static String getRecentLogs(int lines) {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("logs/" + LOG_FILE))) {
            List<String> logLines = reader.lines().collect(Collectors.toList());
            int start = Math.max(0, logLines.size() - lines);
            for (int i = start; i < logLines.size(); i++) {
                result.append(logLines.get(i)).append("\n");
            }
        } catch (IOException e) {
            logError("Ошибка при чтении логов", e);
        }
        return result.toString();
    }
    
    public static String getRecentAuditLogs(int lines) {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("logs/" + AUDIT_LOG_FILE))) {
            List<String> logLines = reader.lines().collect(java.util.stream.Collectors.toList());
            int start = Math.max(0, logLines.size() - lines);
            for (int i = start; i < logLines.size(); i++) {
                result.append(logLines.get(i)).append("\n");
            }
        } catch (IOException e) {
            logError("Ошибка при чтении аудит-логов", e);
        }
        return result.toString();
    }
} 