package com.votopia.votopiabackendspringboot.services;

import com.votopia.votopiabackendspringboot.dtos.list.ListCreateDto;
import com.votopia.votopiabackendspringboot.dtos.list.ListOptionDto;
import com.votopia.votopiabackendspringboot.dtos.list.ListSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.list.ListUpdateDto;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import com.votopia.votopiabackendspringboot.exceptions.ConflictException;
import com.votopia.votopiabackendspringboot.exceptions.BadRequestException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;

import java.util.Set;

public interface ListService {
    /**
     * Crea una nuova Lista all'interno dell'organizzazione dell'utente autenticato.
     * <p>
     * Il metodo applica le seguenti regole di business:
     * <ul>
     * <li><b>Sicurezza:</b> Verifica che l'utente abbia il permesso globale {@code create_list}.</li>
     * <li><b>Multi-tenancy:</b> La lista viene automaticamente associata all'organizzazione dell'autore.</li>
     * <li><b>Quota:</b> Impedisce la creazione se l'organizzazione ha superato il valore {@code maxLists}.</li>
     * <li><b>Validazione:</b> I codici colore devono rispettare il formato esadecimale standard (#RRGGBB).</li>
     * </ul>
     * </p>
     *
     * @param dto        Dati della lista (nome, descrizione, colori, logo).
     * @param authUserId ID dell'utente che invoca la creazione (estratto dal token).
     * @return           Un {@link ListSummaryDto} contenente i dati della lista creata.
     * @throws ForbiddenException Se mancano i permessi o il limite di liste è stato raggiunto.
     * @throws ConflictException  Se esiste già una lista con lo stesso nome nell'organizzazione (Unique Constraint).
     * @throws BadRequestException Se i dati forniti (es. colori) non sono validi.
     */
    ListSummaryDto create(ListCreateDto dto, Long authUserId);

    /**
     * Aggiorna i dettagli di una Lista esistente, verificando i permessi di accesso e l'appartenenza all'organizzazione.
     * <p>
     * Regole di autorizzazione:
     * <ul>
     * <li>L'utente deve appartenere alla stessa organizzazione della lista.</li>
     * <li>L'utente deve possedere {@code update_list_organization} a livello globale</li>
     * <li><b>OPPURE</b> possedere {@code update_list_list} specificamente per la lista indicata.</li>
     * </ul>
     * </p>
     *
     * @param dto        Oggetto contenente l'ID della lista e i campi da aggiornare.
     * @param authUserId ID dell'utente autenticato (estratto dal contesto di sicurezza).
     * @return           Un {@link ListSummaryDto} con i dati della lista aggiornati.
     * @throws NotFoundException  Se la lista o l'utente non esistono.
     * @throws ForbiddenException Se l'utente tenta di accedere a una lista fuori dalla sua Org o non ha i permessi.
     * @throws ConflictException  Se l'aggiornamento viola vincoli di unicità (es. nome duplicato).
     * @throws BadRequestException Se i formati dei colori non sono validi.
     */
    ListSummaryDto update(ListUpdateDto dto, Long authUserId);

    /**
     * Restituisce l'elenco delle liste visibili all'utente autenticato all'interno della propria organizzazione.
     * <p>
     * Il filtro di visibilità segue queste regole:
     * <ul>
     * <li>Se l'utente possiede il permesso {@code view_all_lists}, vengono restituite
     * tutte le liste appartenenti alla sua organizzazione.</li>
     * <li>In assenza di tale permesso, vengono restituite solo le liste a cui l'utente
     * è esplicitamente associato (relazione Many-to-Many).</li>
     * </ul>
     * </p>
     *
     * @param authUserId ID dell'utente autenticato che richiede l'elenco.
     * @return           Una lista di {@link ListSummaryDto} rappresentanti le liste visibili.
     * @throws ForbiddenException Se l'utente non appartiene a un'organizzazione.
     * @throws NotFoundException  Se l'utente non viene trovato nel sistema.
     */
    Set<ListSummaryDto> getAllVisibleLists(Long authUserId);

    /**
     * Restituisce le liste che l'utente autenticato può assegnare durante la creazione di un utente,
     * rispettando i suoi permessi specifici.
     * <p>
     * Se l'utente ha {@code create_user_for_organization}, vede tutte le liste dell'organizzazione.
     * Se ha solo {@code create_user_for_list}, vede solo le liste su cui ha il permesso {@code create_user_for_list}.
     * </p>
     *
     * @param authUserId ID dell'utente autenticato.
     * @return           Un {@link Set} di {@link ListOptionDto} con le liste assegnabili.
     * @throws ForbiddenException Se l'utente non ha permessi di creazione utenti.
     * @throws NotFoundException  Se l'utente non viene trovato.
     */
    Set<ListOptionDto> getAssignableListsForUserCreation(Long authUserId);
}
