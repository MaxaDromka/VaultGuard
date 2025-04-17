package com.example.crypt;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.types.size_t;

public interface CryptSetup {
    int crypt_init(PointerByReference cd, String path);
    int crypt_init_by_name(PointerByReference cd, String name); // Инициализация по имени устройства
    int crypt_format(
            Pointer cd,
            String type,          // Тип как строка ("luks2")
            String cipher,        // Полный шифр (например, "aes-xts-plain64")
            String cipherMode,    // Режим шифра (может быть null, если включён в cipher)
            String uuid,
            Pointer volumeKey,
            @size_t long volumeKeySize,
            Pointer params
    );
    int crypt_keyslot_add_by_volume_key(Pointer cd, int keyslot, Pointer volumeKey, @size_t long volumeKeySize, String password, @size_t long passwordSize);
    int crypt_keyslot_add_by_passphrase(Pointer cd, int keyslot, String password, @size_t long passwordSize);
    int crypt_load(Pointer cd, int type, Pointer params); // Загрузка заголовка LUKS
    int crypt_activate_by_passphrase(Pointer cd, String name, int keyslot, String passphrase, @size_t long passphraseSize, int flags); // Активация контейнера
    int crypt_deactivate(Pointer cd, String name); // Деактивация контейнера
    void crypt_free(Pointer cd);

    // Новые методы для получения информации о шифре
    String crypt_get_cipher(Pointer cd);
    String crypt_get_cipher_mode(Pointer cd);

    // Загружаем библиотеку
    static CryptSetup load() {
        return LibraryLoader.create(CryptSetup.class).load("cryptsetup");
    }
}