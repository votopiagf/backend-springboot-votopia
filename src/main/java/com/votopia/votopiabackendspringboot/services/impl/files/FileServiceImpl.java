package com.votopia.votopiabackendspringboot.services.impl.files;

import com.votopia.votopiabackendspringboot.dtos.file.FileSummaryDto;
import com.votopia.votopiabackendspringboot.entities.files.File;
import com.votopia.votopiabackendspringboot.entities.lists.List;
import com.votopia.votopiabackendspringboot.entities.organizations.Organization;
import com.votopia.votopiabackendspringboot.entities.auth.User;
import com.votopia.votopiabackendspringboot.exceptions.BadRequestException;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import com.votopia.votopiabackendspringboot.exceptions.InternalServerException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import com.votopia.votopiabackendspringboot.repositories.files.FileRepository;
import com.votopia.votopiabackendspringboot.repositories.lists.ListRepository;
import com.votopia.votopiabackendspringboot.repositories.auth.UserRepository;
import com.votopia.votopiabackendspringboot.services.files.FileService;
import com.votopia.votopiabackendspringboot.services.auth.PermissionService;
import com.votopia.votopiabackendspringboot.services.files.StorageService;
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

        List listTarget = null;
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

    @Override
    @Transactional
    public void deleteFile(Long fileId, Long authUserId) {
        // 1. Recupero Utente e File
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        File fileTarget = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File non trovato"));

        // 2. Controllo Multi-tenancy
        if (!fileTarget.getOrg().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("Il file appartiene a un'altra organizzazione.");
        }

        // 3. Verifica Autorizzazione (3 Livelli)
        boolean authorized = false;

        // Livello 1: Permesso Org
        if (permissionService.hasPermission(authUserId, "delete_file_organization")) {
            authorized = true;
        }
        // Livello 2: Permesso Lista (se applicabile)
        else if (fileTarget.getList() != null && permissionService.hasPermission(authUserId, "delete_file_list")) {
            if (permissionService.hasPermissionOnList(authUserId, fileTarget.getList().getId(), "delete_file_list")) {
                authorized = true;
            }
        }
        // Livello 3: Proprietario
        else if (fileTarget.getUser().getId().equals(authUserId)) {
            authorized = true;
        }

        if (!authorized) {
            throw new ForbiddenException("Non hai i permessi per eliminare questo file.");
        }

        // 4. Cancellazione
        String pathToDelete = fileTarget.getFilePath();

        // 4.1 Rimuoviamo il record dal DB
        fileRepository.delete(fileTarget);

        // 4.2 Rimuoviamo il file fisico (StorageService)
        // Usiamo un blocco try-catch interno per non fare rollback del DB se il file fisico
        // è già sparito o il disco ha un problema momentaneo (evita file orfani nel DB)
        try {
            storageService.delete(pathToDelete);
        } catch (Exception e) {
            log.warn("Cancellazione fisica fallita per il percorso: {}. Errore: {}", pathToDelete, e.getMessage());
            // Nota: Il record DB è già segnato per l'eliminazione al commit della transazione.
        }
    }
}