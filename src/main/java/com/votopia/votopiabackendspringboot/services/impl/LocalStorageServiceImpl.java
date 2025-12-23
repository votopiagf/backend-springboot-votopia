package com.votopia.votopiabackendspringboot.services.impl;

import com.votopia.votopiabackendspringboot.services.StorageService;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class LocalStorageServiceImpl implements StorageService {

    // Definito in application.properties (es. storage.location=./uploads)
    @Value("${storage.location}")
    private String rootLocation;

    @Override
    public void save(byte[] content, String filePath) {
        try {
            // 1. Definiamo il percorso completo (root + path relativo del file)
            Path fullPath = Paths.get(rootLocation).resolve(filePath);

            // 2. Creiamo le cartelle genitrici se non esistono (mkdir -p)
            Files.createDirectories(fullPath.getParent());

            // 3. Scriviamo i byte sul file
            Files.write(fullPath, content);

            log.info("File salvato correttamente in: {}", fullPath);
        } catch (IOException e) {
            log.error("Errore durante il salvataggio fisico del file: {}", e.getMessage());
            throw new RuntimeException("Impossibile salvare il file su disco", e);
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            Path path = Paths.get(rootLocation).resolve(filePath);
            Files.deleteIfExists(path);
            log.info("File eliminato: {}", path);
        } catch (IOException e) {
            log.error("Errore durante l'eliminazione del file: {}", e.getMessage());
        }
    }
}