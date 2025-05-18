#!/bin/bash

# Получаем имя текущего пользователя
USERNAME=$(whoami)

# Проверяем наличие файла политики
if [ ! -f "/usr/share/polkit-1/actions/com.example.crypt.policy" ]; then
    echo "Политика Polkit не установлена. Установите её с помощью:"
    echo "sudo ./install-policy.sh"
    exit 1
fi

# Запускаем приложение с правами текущего пользователя
java --module-path=/home/$USERNAME/Загрузки/openjfx-17.0.15_linux-x64_bin-sdk/javafx-sdk-17.0.15/lib --add-modules=javafx.controls,javafx.fxml -jar out/artifacts/Crypt_jar/Crypt.jar $USERNAME