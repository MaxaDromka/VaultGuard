#!/bin/bash

# Проверяем, запущен ли скрипт с правами root
if [ "$EUID" -ne 0 ]; then 
    echo "Этот скрипт должен быть запущен с правами root"
    exit 1
fi

# Копируем файл политики
cp com.example.crypt.policy /usr/share/polkit-1/actions/

# Устанавливаем правильные права
chmod 644 /usr/share/polkit-1/actions/com.example.crypt.policy

# Перезапускаем службу polkit
systemctl restart polkit

echo "Политика Polkit успешно установлена и служба перезапущена" 