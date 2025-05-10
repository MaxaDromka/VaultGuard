package com.example.crypt;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

public class LogManager {
    private static final Logger logger = Logger.getLogger(LogManager.class.getName());
    private static final String LOG_FILE = "crypt_manager.log";
    private static final String AUDIT_LOG_FILE = "crypt_audit.log";
    
    static {
        try {
            // Настройка основного логгера
            FileHandler fileHandler = new FileHandler(LOG_FILE, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            
            // Настройка аудит-логгера
            Logger auditLogger = Logger.getLogger("AuditLogger");
            FileHandler auditFileHandler = new FileHandler(AUDIT_LOG_FILE, true);
            auditFileHandler.setFormatter(new SimpleFormatter());
            auditLogger.addHandler(auditFileHandler);
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
} 