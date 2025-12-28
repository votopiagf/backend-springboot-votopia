package com.votopia.votopiabackendspringboot.services;

import com.votopia.votopiabackendspringboot.dtos.candidate.CandidateCreateDto;
import com.votopia.votopiabackendspringboot.dtos.candidate.CandidateSummaryDto;
import com.votopia.votopiabackendspringboot.exceptions.BadRequestException;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;

import java.util.Set;

public interface CandidateService {
    /**
     * Crea un nuovo candidato all'interno di una lista specifica previa validazione dei permessi e dei vincoli di business.
     * <p>
     * Il processo di creazione segue questi step:
     * 1. Verifica l'esistenza della lista target all'interno dell'organizzazione dell'utente autenticato (Multi-tenancy).
     * 2. Valida i permessi dell'operatore (richiesto 'create_candidate_organization' o 'create_candidate_list').
     * 3. Verifica l'esistenza del file multimediale per la foto e dell'utente da candidare.
     * 4. Controlla che l'utente target possieda il permesso 'can_be_candidate'.
     * 5. Impedisce duplicazioni (un utente non può essere candidato più volte nella stessa lista).
     * </p>
     *
     * @param dto        Oggetto {@link CandidateCreateDto} contenente i dati del candidato,
     * inclusi listId, userId, photoFileId, bio e classe.
     * @param authUserId ID dell'utente che esegue l'operazione (estratto dal Security Context).
     * @return           Un {@link CandidateSummaryDto} contenente i dettagli del candidato appena creato.
     * @throws NotFoundException   Se la lista, il file o l'utente target non esistono.
     * @throws ForbiddenException  Se l'operatore non ha i permessi o se l'utente target non è candidabile.
     * @throws BadRequestException Se l'utente è già presente come candidato nella lista specificata.
     */
    CandidateSummaryDto create(CandidateCreateDto dto, Long authUserId);

    /**
     * Recupera tutti i candidati associati a una specifica lista.
     * <p>
     * Il metodo garantisce la sicurezza dei dati tramite:
     * 1. Controllo Multi-tenancy: la lista deve appartenere all'organizzazione dell'utente richiedente.
     * 2. Controllo Permessi: l'utente deve avere il permesso di visualizzazione globale o specifico per la lista.
     * </p>
     *
     * @param listId     ID della lista di cui recuperare i candidati.
     * @param authUserId ID dell'utente autenticato che effettua la richiesta.
     * @return           Un set di {@link CandidateSummaryDto} rappresentanti i candidati della lista.
     * @throws NotFoundException  Se la lista non esiste o non appartiene all'organizzazione dell'utente.
     * @throws ForbiddenException Se l'utente non ha i permessi di visualizzazione necessari.
     */
    Set<CandidateSummaryDto> getAllByList(Long listId, Long authUserId);
}
