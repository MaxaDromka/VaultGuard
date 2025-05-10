package com.example.crypt;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContainerMonitor {
    private static final Map<String, ContainerStats> containerStats = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public static class ContainerStats {
        private long totalSize;
        private long usedSpace;
        private long lastAccessTime;
        private int accessCount;
        private List<String> recentActivities;
        
        public ContainerStats(long totalSize) {
            this.totalSize = totalSize;
            this.usedSpace = 0;
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount = 0;
            this.recentActivities = new ArrayList<>();
        }
        
        public void updateStats(long usedSpace) {
            this.usedSpace = usedSpace;
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount++;
        }
        
        public void addActivity(String activity) {
            recentActivities.add(0, activity);
            if (recentActivities.size() > 10) {
                recentActivities.remove(recentActivities.size() - 1);
            }
        }
        
        public double getUsagePercentage() {
            return (double) usedSpace / totalSize * 100;
        }
        
        // Геттеры
        public long getTotalSize() { return totalSize; }
        public long getUsedSpace() { return usedSpace; }
        public long getLastAccessTime() { return lastAccessTime; }
        public int getAccessCount() { return accessCount; }
        public List<String> getRecentActivities() { return Collections.unmodifiableList(recentActivities); }
    }
    
    public static void startMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateContainerStats();
            } catch (Exception e) {
                LogManager.logError("Ошибка при обновлении статистики контейнеров", e);
            }
        }, 0, 1, TimeUnit.MINUTES);
    }
    
    private static void updateContainerStats() {
        for (Map.Entry<String, ContainerStats> entry : containerStats.entrySet()) {
            String containerPath = entry.getKey();
            File containerFile = new File(containerPath);
            
            if (containerFile.exists()) {
                ContainerStats stats = entry.getValue();
                stats.updateStats(containerFile.length());
                
                // Логируем статистику
                LogManager.logInfo(String.format(
                    "Контейнер: %s, Использовано: %.2f%%, Последний доступ: %s",
                    containerFile.getName(),
                    stats.getUsagePercentage(),
                    new Date(stats.getLastAccessTime())
                ));
            }
        }
    }
    
    public static void addContainer(String containerPath) {
        File containerFile = new File(containerPath);
        if (containerFile.exists()) {
            containerStats.put(containerPath, new ContainerStats(containerFile.length()));
            LogManager.logInfo("Добавлен контейнер для мониторинга: " + containerPath);
        }
    }
    
    public static void removeContainer(String containerPath) {
        containerStats.remove(containerPath);
        LogManager.logInfo("Удален контейнер из мониторинга: " + containerPath);
    }
    
    public static ContainerStats getContainerStats(String containerPath) {
        return containerStats.get(containerPath);
    }
    
    public static Map<String, ContainerStats> getAllContainerStats() {
        return Collections.unmodifiableMap(containerStats);
    }
    
    public static void stopMonitoring() {
        scheduler.shutdown();
    }
    
    public static void recordContainerAccess(String containerPath, String action) {
        ContainerStats stats = containerStats.get(containerPath);
        if (stats != null) {
            stats.addActivity(action);
            LogManager.logContainerAccess(containerPath, action, true);
        }
    }
} 