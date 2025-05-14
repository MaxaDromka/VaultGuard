#!/bin/bash

# Копируем файл политики в нужную директорию
sudo cp com.example.crypt.policy /usr/share/polkit-1/actions/

# Устанавливаем правильные права
sudo chmod 644 /usr/share/polkit-1/actions/com.example.crypt.policy

echo "Polkit policy installed successfully" 