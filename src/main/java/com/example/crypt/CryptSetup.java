package com.example.crypt;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.Out;
import jnr.ffi.types.size_t;

public interface CryptSetup {
    int crypt_init(Pointer cd, String path);
    int crypt_init_by_name(Pointer cd, String name); // Инициализация по имени устройства
    int crypt_format(Pointer cd, int type, String cipher, String cipherMode, String uuid, int volumeKeySize);
    int crypt_keyslot_add_by_volume_key(Pointer cd, int keyslot, Pointer volumeKey, @size_t long volumeKeySize, String password, @size_t long passwordSize);
    int crypt_load(Pointer cd, int type, Pointer params); // Загрузка заголовка LUKS
    int crypt_activate_by_passphrase(Pointer cd, String name, int keyslot, String passphrase, @size_t long passphraseSize, int flags); // Активация контейнера
    int crypt_deactivate(Pointer cd, String name); // Деактивация контейнера
    void crypt_free(Pointer cd);

    // Загружаем библиотеку
    static CryptSetup load() {
        return LibraryLoader.create(CryptSetup.class).load("cryptsetup");
    }
}