package com.votopia.votopiabackendspringboot.services.auth;

import com.votopia.votopiabackendspringboot.dtos.user.UserCreateDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserDetailDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserUpdateDto;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import com.votopia.votopiabackendspringboot.exceptions.ConflictException;
import jakarta.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.util.Set;

public interface UserService {
    /**
     * Crea un nuovo utente nel sistema, associandolo all'organizzazione del richiedente,
     * alle liste specificate e ai ruoli indicati.
     * <p>
     * Il processo di registrazione segue queste regole di sicurezza:
     * <ul>
     * <li><b>Autorizzazione:</b> Richiede il permesso {@code create_user_for_organization} (per gestione globale)
     * o {@code create_user_for_list} (per gestione limitata a una lista).</li>
     * <li><b>Gerarchia:</b> L'amministratore può assegnare solo ruoli con un livello {@code <=} al proprio.</li>
     * <li><b>Integrità:</b> Viene verificata l'unicità dell'email all'interno dell'organizzazione.</li>
     * </ul>
     * </p>
     *
     * @param userToCreate        Oggetto {@link UserCreateDto} contenente i dati del nuovo utente (nome, cognome, email, password, liste e ruoli).
     * @param authUserId ID dell'utente amministratore che esegue l'operazione (estratto dal JWT).
     * @return Un oggetto {@link UserSummaryDto} rappresentante l'utente appena creato e salvato.
     * @throws ConflictException  Se l'email fornita è già registrata nell'organizzazione (HTTP 409).
     * @throws ForbiddenException Se il richiedente non ha i permessi necessari o tenta di assegnare
     * ruoli/liste superiori ai propri privilegi (HTTP 403).
     * @throws NotFoundException  Se l'utente autenticato o le risorse collegate (liste/ruoli) non esistono (HTTP 404).
     */
    UserSummaryDto register(UserCreateDto userToCreate, Long authUserId);

    /**
     * Restituisce i dettagli di un utente specifico o del profilo dell'utente autenticato (self).
     * <p>
     * Il metodo applica le seguenti regole di visibilità e sicurezza:
     * <ul>
     * <li>La visualizzazione è limitata agli utenti appartenenti alla stessa organizzazione.</li>
     * <li>Per visualizzare altri utenti è richiesto il permesso {@code view_all_user_organization} (livello Org)
     * o {@code view_all_user_list} (limitato alle liste condivise).</li>
     * <li>Se l'ID target non è fornito, il metodo restituisce le informazioni del richiedente stesso.</li>
     * </ul>
     * </p>
     *
     * @param authUserId ID dell'utente autenticato che effettua la richiesta (estratto dal contesto di sicurezza).
     * @param userId     ID opzionale dell'utente da visualizzare. Se {@code null}, viene restituito l'utente richiedente.
     * @return Un oggetto {@link UserSummaryDto} con i dettagli dell'utente richiesto.
     * @throws NotFoundException  Se l'utente richiedente o l'utente target non vengono trovati nel database.
     * @throws ForbiddenException Se il richiedente tenta di visualizzare un utente di un'altra organizzazione
     *                            o non dispone dei permessi necessari per la visualizzazione esterna.
     */
    UserDetailDto getUserInformation(@Nullable Long userId, Long authUserId);

    /**
     * Recupera la lista degli utenti visibili in base ai permessi del richiedente e al filtro applicato.
     * <p>
     * Il metodo gestisce due scenari di filtraggio:
     * <ul>
     * <li><b>Filtro Organizzazione:</b> Se {@code listId} è {@code null}, restituisce tutti gli utenti
     * della stessa organizzazione. Richiede il permesso {@code view_all_user_organization}.</li>
     * <li><b>Filtro Lista:</b> Se {@code listId} è fornito, restituisce gli utenti appartenenti a
     * quella lista. Richiede il permesso {@code view_all_user_list} sulla lista specificata.</li>
     * </ul>
     * </p>
     *
     * @param authUserId ID dell'utente autenticato che esegue la richiesta.
     * @param listId     ID opzionale della lista per filtrare i risultati.
     * @return Una collezione di {@link UserSummaryDto} contenente i dati degli utenti trovati.
     * @throws ForbiddenException Se l'utente non ha i permessi necessari o tenta di accedere a una lista non autorizzata.
     * @throws NotFoundException  Se l'utente autenticato non viene trovato nel sistema.
     */
    Set<UserDetailDto> getAllVisibleUsers(Long authUserId, @Nullable Long listId);

    /**
     * Esegue la cancellazione logica (soft delete) di un utente nel sistema.
     * <p>
     * Il metodo non rimuove fisicamente il record dal database, ma imposta il flag {@code deleted} a {@code true}.
     * Segue le seguenti regole di business e sicurezza:
     * <ul>
     * <li><b>Autorizzazione:</b> Richiede il permesso {@code delete_user_organization}.</li>
     * <li><b>Restrizione Organizzazione:</b> L'operazione è consentita solo se l'utente target appartiene
     * alla stessa organizzazione dell'utente autenticato.</li>
     * <li><b>Integrità:</b> Il metodo verifica che l'utente target esista e non sia già stato eliminato.</li>
     * </ul>
     * </p>
     *
     * @param authUserId   ID dell'utente autenticato che richiede l'eliminazione (estratto dal JWT).
     * @param targetUserIdToDelete ID dell'utente da sottoporre a soft delete.
     * @throws NotFoundException  Se l'utente target non esiste o è già stato eliminato (HTTP 404).
     * @throws ForbiddenException Se l'utente autenticato non ha il permesso richiesto o tenta di
     * eliminare un utente di un'altra organizzazione (HTTP 403).
     */
    void delete(Long targetUserIdToDelete, Long authUserId);

    /**
     * Aggiorna i dati di un utente e gestisce le sue affiliazioni alle liste.
     * <p>
     * Regole di sicurezza:
     * <ul>
     * <li>Self-update: Consentito sempre per i campi base.</li>
     * <li>Org-update: Richiede 'update_user_organization' per modificare chiunque nella stessa Org.</li>
     * <li>List-update: Richiede 'update_user_list' per modificare utenti nelle proprie liste.</li>
     * </ul>
     * </p>
     *
     * @param authUserId ID dell'utente che esegue l'azione.
     * @param dto Dati di aggiornamento.
     * @return DTO dell'utente aggiornato.
     */
    UserSummaryDto update(Long authUserId, UserUpdateDto dto);

    Set<UserSummaryDto> registerListUsers(Set<UserCreateDto> users, Long authUserId);

    Set<UserSummaryDto> updateListUsers(Set<UserUpdateDto> users, Long authUserId);

    void deleteList(Set<Long> targetUsers, Long authUserId);

    ByteArrayInputStream createExcelAllVisibleUsers(Long authUserId, @Nullable Long targetListId);
}
