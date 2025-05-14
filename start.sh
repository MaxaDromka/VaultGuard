#!/bin/bash

# Получаем реальное имя пользователя
REAL_USER=$(id -un)

# Получаем абсолютный путь к директории скрипта
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

# Запускаем jar-файл с правами root
sudo java -jar --module-path /home/maksimka/Загрузки/javafx-sdk-17.0.14/lib --add-modules=javafx.controls,javafx.fxml "$SCRIPT_DIR/out/artifacts/Crypt_jar/Crypt.jar" "$REAL_USER"