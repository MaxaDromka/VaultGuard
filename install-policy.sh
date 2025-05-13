#!/bin/bash

# Проверяем, запущен ли скрипт с правами root
if [ "$EUID" -ne 0 ]; then 
    echo "Этот скрипт должен быть запущен с правами root"
    exit 1
fi

# Копируем файл политики
cp com.example.crypt.policy /usr/share/polkit-1/actions/

# Копируем файл правил
cp 99-vaultguard.rules /etc/polkit-1/rules.d/

# Устанавливаем правильные права
chmod 644 /usr/share/polkit-1/actions/com.example.crypt.policy
chmod 644 /etc/polkit-1/rules.d/99-vaultguard.rules

# Перезапускаем polkit
systemctl restart polkit

echo "Правила polkit успешно установлены" 