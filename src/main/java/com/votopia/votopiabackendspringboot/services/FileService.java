package com.votopia.votopiabackendspringboot.services;

import com.votopia.votopiabackendspringboot.dtos.file.FileSummaryDto;
import io.micrometer.common.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import com.votopia.votopiabackendspringboot.exceptions.BadRequestException;

public interface FileService {
    /**
     * Gestisce il caricamento e la registrazione di un file multimediale o documento.
     * <p>
     * Il processo segue questi criteri di sicurezza e integrità:
     * <ul>
     * <li><b>Controllo Quota:</b> Verifica che il file non superi la dimensione massima configurata.</li>
     * <li><b>Gerarchia Permessi:</b>
     * - Per caricamenti a livello Org: richiede {@code add_file_organization}.
     * - Per caricamenti su una Lista: richiede {@code add_file_organization} o il permesso locale {@code add_file_list}.
     * </li>
     * <li><b>Storage:</b> Genera un nome file univoco tramite {@link java.util.UUID} per prevenire sovrascritture.</li>
     * <li><b>Organizzazione:</b> Garantisce che il file sia isolato nel percorso dell'organizzazione dell'utente.</li>
     * </ul>
     * </p>
     *
     * @param file       Oggetto {@link MultipartFile} contenente il binario.
     * @param listId     (Opzionale) ID della lista di destinazione.
     * @param categoryId (Opzionale) ID della categoria file.
     * @param authUserId ID dell'utente che esegue l'upload.
     * @return           Un {@link FileSummaryDto} con i metadati del file salvato.
     * @throws BadRequestException Se il file è vuoto o troppo grande.
     * @throws ForbiddenException  Se l'utente non ha i permessi necessari per il contesto.
     * @throws NotFoundException   Se la lista o la categoria specificata non esistono nell'Org dell'utente.
     */
    FileSummaryDto uploadFile(MultipartFile file, @Nullable Long listId, Long categoryId, Long authUserId);
}
