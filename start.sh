#!/bin/bash

# Получаем реальное имя пользователя
REAL_USER=$(id -un)

# Получаем абсолютный путь к директории скрипта
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
sudo java -jar --module-path /home/$USERNAME/Загрузки/openjfx-17.0.15_linux-x64_bin-sdk/javafx-sdk-17.0.15/lib --add-modules=javafx.controls,javafx.fxml "$SCRIPT_DIR/out/artifacts/Crypt_jar/Crypt.jar" "$REAL_USER"