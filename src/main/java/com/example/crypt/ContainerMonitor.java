package com.example.crypt;

import javafx.collections.ObservableList;

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
        ObservableList<Partition> containers = EncryptionManager.getContainersList();
        for (Partition container : containers) {
            String containerPath = container.getPath();
            File containerFile = new File(containerPath);
            
            if (!containerStats.containsKey(containerPath)) {
                containerStats.put(containerPath, new ContainerStats(containerFile.length()));
            }
            
            ContainerStats stats = containerStats.get(containerPath);
            stats.updateStats(containerFile.length());
            
            // Логируем статистику
            LogManager.logInfo(String.format(
                "Контейнер: %s, Использовано: %.2f%%, Последний доступ: %s",
                container.getName(),
                stats.getUsagePercentage(),
                new Date(stats.lastAccessTime)
            ));
        }
    }
    
    public static ContainerStats getContainerStats(String containerPath) {
        return containerStats.get(containerPath);
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