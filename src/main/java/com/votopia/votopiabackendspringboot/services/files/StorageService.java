package com.votopia.votopiabackendspringboot.services.files;

public interface StorageService {
    /**
     * Salva i byte di un file nel percorso specificato.
     */
    void save(byte[] content, String filePath);

    /**
     * Elimina un file fisicamente dal supporto.
     */
    void delete(String filePath);
}