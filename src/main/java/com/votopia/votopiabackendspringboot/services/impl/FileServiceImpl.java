package com.votopia.votopiabackendspringboot.services.impl;

import com.votopia.votopiabackendspringboot.dtos.file.FileSummaryDto;
import com.votopia.votopiabackendspringboot.entities.File;
import com.votopia.votopiabackendspringboot.entities.Organization;
import com.votopia.votopiabackendspringboot.entities.User;
import com.votopia.votopiabackendspringboot.exceptions.BadRequestException;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import com.votopia.votopiabackendspringboot.exceptions.InternalServerException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import com.votopia.votopiabackendspringboot.repositories.FileRepository;
import com.votopia.votopiabackendspringboot.repositories.ListRepository;
import com.votopia.votopiabackendspringboot.repositories.UserRepository;
import com.votopia.votopiabackendspringboot.services.FileService;
import com.votopia.votopiabackendspringboot.services.PermissionService;
import com.votopia.votopiabackendspringboot.services.StorageService;
import io.micrometer.common.lang.Nullable;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    private final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB esempio

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private ListRepository listRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private StorageService storageService;

    @Override
    @Transactional
    public FileSummaryDto uploadFile(MultipartFile file, @Nullable Long listId, Long categoryId, Long authUserId) {
        // 1. Validazione base
        if (file.isEmpty()) throw new BadRequestException("File mancante");
        if (file.getSize() > MAX_FILE_SIZE_BYTES) throw new BadRequestException("Dimensione file eccessiva");

        User user = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        Organization org = user.getOrg();
        if (org == null) throw new ForbiddenException("Utente senza organizzazione");

        // 2. Controllo Permessi (Logica simile a Role/List)
        boolean canOrg = permissionService.hasPermission(authUserId, "add_file_organization");
        boolean canList = permissionService.hasPermission(authUserId, "add_file_list");

        com.votopia.votopiabackendspringboot.entities.List listTarget = null;
        if (listId != null) {
            listTarget = listRepository.findByIdAndOrgId(listId, org.getId())
                    .orElseThrow(() -> new NotFoundException("Lista non trovata"));

            if (!canOrg && (!canList || !permissionService.hasPermissionOnList(authUserId, listId, "add_file_list"))) {
                throw new ForbiddenException("Non hai permessi per aggiungere file a questa lista");
            }
        } else if (!canOrg) {
            throw new ForbiddenException("Permesso add_file_organization richiesto per caricamento root");
        }

        // 3. Salvataggio Fisico
        String fileNameOriginal = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(fileNameOriginal);
        String uniqueName = UUID.randomUUID().toString() + "." + (extension != null ? extension : "bin");

        // Path logico: uploads/{orgId}/{listId or "org"}/{catId}/{uuid}
        String storageFolder = String.format("uploads/%d/%s/%s",
                org.getId(),
                listId != null ? listId : "org",
                categoryId != null ? categoryId : "0");

        String finalPath = storageFolder + "/" + uniqueName;

        try {
            // Qui invocheresti un servizio di storage (S3 o FileSystem locale)
            storageService.save(file.getBytes(), finalPath);
        } catch (IOException e) {
            log.error("Errore I/O durante salvataggio file: {}", e.getMessage());
            throw new InternalServerException("Impossibile salvare il file fisicamente");
        }

        // 4. Registrazione Database
        File newFile = new File();
        newFile.setName(fileNameOriginal);
        newFile.setOrg(org);
        newFile.setList(listTarget);
        newFile.setUser(user);
        newFile.setFilePath(finalPath);
        newFile.setMimeType(file.getContentType());
        // Se hai una categoria, la setti qui

        return new FileSummaryDto(fileRepository.save(newFile));
    }
}