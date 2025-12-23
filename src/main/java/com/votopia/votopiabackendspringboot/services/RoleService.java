package com.votopia.votopiabackendspringboot.services;

import com.votopia.votopiabackendspringboot.dtos.role.RoleInfoResponse;
import com.votopia.votopiabackendspringboot.dtos.role.RoleCreateDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleUpdateDto;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import io.micrometer.common.lang.Nullable;

import java.util.Set;

public interface RoleService {
    /**
     * Crea un nuovo ruolo nel sistema, gestendo la separazione tra ruoli di Organizzazione e di Lista.
     * <p>
     * Il metodo applica rigorosi controlli di sicurezza basati su:
     * <ul>
     * <li><b>Contesto:</b> Il ruolo viene creato come ruolo globale (Org) se {@code listId} è nullo,
     * altrimenti viene associato a una lista specifica.</li>
     * <li><b>Possessione Permessi:</b> L'utente autenticato può assegnare al nuovo ruolo esclusivamente
     * permessi che possiede personalmente (controllato tramite {@code userPermIds}).</li>
     * <li><b>Gerarchia:</b> Il livello del nuovo ruolo deve essere strettamente inferiore al massimo
     * livello posseduto dall'utente nel contesto di riferimento (Org o Lista).</li>
     * </ul>
     * </p>
     *
     * @param role        Oggetto contenente i dati del ruolo (nome, colore, livello, lista e permessi).
     * @param authUserId ID dell'utente autenticato che esegue l'operazione.
     * @return           Un {@link RoleSummaryDto} che rappresenta il ruolo appena creato e salvato.
     * @throws NotFoundException  Se l'utente autenticato, la lista target o uno dei permessi specificati non esistono.
     * @throws ForbiddenException Se l'utente non ha i permessi necessari, tenta di assegnare permessi che non possiede,
     * viola la gerarchia dei livelli o tenta di agire fuori dalla propria organizzazione.
     */
    RoleSummaryDto create(RoleCreateDto role, Long authUserId);

    /**
     * Elimina definitivamente un ruolo dal sistema (Hard Delete).
     * <p>
     * Il metodo esegue una serie di controlli di sicurezza stringenti prima di procedere:
     * <ul>
     * <li><b>Validazione Organizzazione:</b> Verifica che il ruolo appartenga alla stessa
     * organizzazione dell'utente autenticato per garantire l'isolamento dei dati (Multi-tenancy).</li>
     * <li><b>Verifica Permessi:</b>
     * <ul>
     * <li>Per ruoli di Organizzazione: richiede il permesso {@code delete_role_organization}.</li>
     * <li>Per ruoli di Lista: richiede {@code delete_role_organization} oppure il permesso
     * specifico {@code delete_role_list} sulla lista associata al ruolo.</li>
     * </ul>
     * </li>
     * <li><b>Controllo Gerarchico:</b> L'utente può eliminare esclusivamente ruoli il cui
     * livello ({@code level}) sia <b>strettamente inferiore</b> al proprio massimo livello
     * di autorità calcolato nel contesto specifico (Organizzazione o Lista).</li>
     * </ul>
     * </p>
     *
     * @param roleId     ID univoco del ruolo da eliminare.
     * @param authUserId ID dell'utente autenticato che richiede l'operazione.
     * @throws NotFoundException  Se il ruolo target o l'utente autenticato non vengono trovati nel database.
     * @throws ForbiddenException Se viene violata l'integrità dell'organizzazione, se i permessi
     * sono insufficienti o se si tenta di eliminare un ruolo di livello
     * pari o superiore al proprio (violazione gerarchica).
     */
    void delete(Long roleId, Long authUserId);

    /**
     * Recupera l'elenco dei ruoli visibili all'utente autenticato, applicando filtri di
     * organizzazione o di lista in base ai parametri forniti.
     * <p>
     * Il metodo implementa la seguente logica di autorizzazione:
     * <ul>
     * <li><b>Visualizzazione Globale:</b> Se {@code listId} è {@code null}, l'utente deve possedere
     * il permesso {@code view_all_role_organization}. Vengono restituiti tutti i ruoli (sia globali che di lista)
     * appartenenti all'organizzazione dell'utente.</li>
     * <li><b>Visualizzazione per Lista:</b> Se {@code listId} è fornito, il sistema verifica che la lista
     * appartenga all'organizzazione dell'utente. L'accesso è consentito se l'utente ha il permesso
     * {@code view_all_role_organization} oppure se possiede {@code view_all_role_list}
     * specificamente per la lista richiesta.</li>
     * </ul>
     * </p>
     *
     * @param authUserId ID dell'utente autenticato che effettua la richiesta.
     * @param listId     (Opzionale) ID della lista specifica di cui si vogliono recuperare i ruoli.
     * Se {@code null}, la ricerca è estesa a tutta l'organizzazione.
     * @return           Una {@link Set} di {@link RoleSummaryDto} contenente i dati dei ruoli visibili.
     * @throws NotFoundException  Se l'utente autenticato o la lista target (se specificata) non esistono
     * o non appartengono all'organizzazione dell'utente.
     * @throws ForbiddenException Se l'utente non dispone dei permessi necessari per il contesto richiesto
     * (Org o Lista specifica).
     */
    Set<RoleSummaryDto> getAllVisible(Long authUserId, @Nullable Long listId);

    /**
     * Fornisce informazioni dettagliate su un ruolo specifico o sui ruoli dell'utente corrente.
     * <p>
     * Logica di autorizzazione:
     * <ul>
     * <li>Se {@code roleId} è {@code null}: restituisce i ruoli associati all'utente {@code authUserId}.</li>
     * <li>Se {@code roleId} è presente:
     * <ul>
     * <li>Il ruolo deve appartenere alla stessa organizzazione dell'utente.</li>
     * <li>L'accesso è garantito dal permesso {@code view_all_role_organization}.</li>
     * <li>Per i ruoli di lista, l'accesso è garantito anche dal permesso {@code view_all_role_list}
     * se posseduto sulla lista specifica del ruolo.</li>
     * </ul>
     * </li>
     * </ul>
     * </p>
     *
     * @param authUserId ID dell'utente che richiede le informazioni.
     * @param roleId     (Opzionale) ID del ruolo da visualizzare.
     * @return           {@link RoleInfoResponse} contenente i ruoli filtrati e dati di diagnostica.
     * @throws ForbiddenException Se l'utente tenta di accedere a ruoli di altre Org o non ha i permessi necessari.
     * @throws NotFoundException  Se l'utente o il ruolo target non esistono.
     */
    RoleInfoResponse getRoleInformation(Long authUserId, Long roleId);

    /**
     * Aggiorna i dettagli di un ruolo esistente, applicando regole di sicurezza gerarchiche.
     * <p>
     * Il processo di validazione segue questi step:
     * <ul>
     * <li><b>Isolamento:</b> Verifica che il ruolo appartenga all'Organizzazione dell'utente.</li>
     * <li><b>Autorità sul Target:</b> L'utente deve avere un livello gerarchico
     * <i>strettamente superiore</i> al livello attuale del ruolo da modificare.</li>
     * <li><b>Autorità sul Nuovo Livello:</b> Se il livello viene modificato, il nuovo valore
     * non può superare il livello massimo dell'utente nel contesto (Org o Lista).</li>
     * <li><b>Vincolo Permessi:</b> L'utente può assegnare solo permessi che possiede
     * nel proprio set di autorità.</li>
     * </ul>
     * </p>
     *
     * @param dto        Dati di aggiornamento (ID obbligatorio, altri campi opzionali).
     * @param authUserId ID dell'utente che esegue l'operazione.
     * @return           {@link RoleSummaryDto} con i dati aggiornati.
     * @throws ForbiddenException Se viene violata la gerarchia o mancano i permessi di possesso.
     * @throws NotFoundException  Se il ruolo o l'utente non esistono.
     */
    RoleSummaryDto update(RoleUpdateDto dto, Long authUserId);
}
