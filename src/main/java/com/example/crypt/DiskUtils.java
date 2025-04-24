package com.example.crypt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DiskUtils {
    /**
     * Получает список активных зашифрованных дисков.
     */
    public static List<String> getLuksDevices() throws IOException {
        List<String> luksDevices = new ArrayList<>();
        Process process = Runtime.getRuntime().exec("sudo dmsetup ls --target crypt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            // Формат вывода: имя_устройства (например, "user_home")
            String[] parts = line.split("\\s+");
            if (parts.length > 0) {
                luksDevices.add(parts[0].trim());
            }
        }
        return luksDevices;
    }
}