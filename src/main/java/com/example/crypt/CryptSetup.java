package com.example.crypt;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.types.size_t;

public interface CryptSetup {
    // Константы
    static final String CRYPT_LUKS2 = "LUKS2";
    static final String CRYPT_LUKS1 = "LUKS1";
    static final int CRYPT_ANY_SLOT = -1;
    static final int CRYPT_ACTIVATE_READONLY = 1 << 0;

    // Основные методы
    int crypt_init(PointerByReference cd, String path);
    int crypt_init_by_name(PointerByReference cd, String name);
    void crypt_free(Pointer cd);

    int crypt_format(
            Pointer cd,
            String type,          // Тип устройства ("luks2")
            String cipher,        // Шифр (например, "aes")
            String cipherMode,    // Режим шифра (например, "xts-plain64")
            String uuid,          // UUID или null для автогенерации
            Pointer volumeKey,    // Указатель на ключ тома или null
            @size_t long volumeKeySize, // Размер ключа тома в байтах
            Pointer params         // Дополнительные параметры или null
    );

    int crypt_keyslot_add_by_volume_key(Pointer cd, int keyslot, Pointer volumeKey, @size_t long volumeKeySize, String password, @size_t long passwordSize);
    int crypt_keyslot_add_by_passphrase(Pointer cd, int keyslot, String password, @size_t long passwordSize);

    int crypt_load(Pointer cd, String requestedType, Pointer params);
    int crypt_activate_by_passphrase(Pointer cd, String name, int keyslot, String passphrase, @size_t long passphraseSize, int flags);
    int crypt_deactivate(Pointer cd, String name);

    int crypt_set_uuid(Pointer cd, String uuid);
    int crypt_set_label(Pointer cd, String label, String subsystem);

    int crypt_suspend(Pointer cd, String name);
    int crypt_resume_by_passphrase(Pointer cd, String name, int keyslot, String passphrase, @size_t long passphraseSize);

    // Методы для получения информации
    String crypt_get_cipher(Pointer cd);
    String crypt_get_cipher_mode(Pointer cd);
    String crypt_get_error();

    // Загружаем библиотеку
    static CryptSetup load() {
        return LibraryLoader.create(CryptSetup.class).load("cryptsetup");
    }
}