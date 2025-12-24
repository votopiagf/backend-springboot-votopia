package com.votopia.votopiabackendspringboot.services;

import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignCreateDto;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignSummaryDto;
import com.votopia.votopiabackendspringboot.exceptions.BadRequestException;
import com.votopia.votopiabackendspringboot.exceptions.ConflictException;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import io.micrometer.common.lang.Nullable;

import java.util.Set;

public interface CampaignService {
    /**
     * Crea una nuova Campagna elettorale o informativa all'interno di una Lista specifica.
     * <p>
     * Requisiti di validazione e sicurezza:
     * <ul>
     * <li><b>Integrità:</b> La lista indicata deve appartenere all'organizzazione dell'utente.</li>
     * <li><b>Cronologia:</b> La {@code startDate} deve precedere rigorosamente la {@code endDate}.</li>
     * <li><b>Autorizzazione:</b> Richiede il permesso globale {@code create_campaign_organization}
     * o il permesso specifico sulla lista {@code create_campaign_list}.</li>
     * </ul>
     * </p>
     *
     * @param dto        Dati della campagna (nome, date, lista associata).
     * @param authUserId ID dell'utente autenticato estratto dal token JWT.
     * @return           {@link CampaignSummaryDto} con i dettagli della campagna creata.
     * @throws BadRequestException Se le date non sono valide o mancano campi obbligatori.
     * @throws ForbiddenException  Se l'utente non ha i permessi necessari.
     * @throws ConflictException   Se si verifica una violazione di unicità nel database.
     */
    CampaignSummaryDto create(CampaignCreateDto dto, Long authUserId);

    /**
     * Recupera l'elenco delle campagne filtrato in base ai permessi dell'utente e all'ambito richiesto.
     * <p>
     * La logica di recupero segue queste regole di priorità:
     * <ul>
     * <li>Se {@code listId} è nullo: richiede il permesso globale {@code view_all_campaign_organization}
     * e restituisce tutte le campagne dell'intera organizzazione.</li>
     * <li>Se {@code listId} è fornito: verifica che la lista appartenga all'organizzazione dell'utente.
     * L'accesso è consentito se l'utente ha il permesso globale o il permesso specifico
     * {@code view_all_campaign_list} sulla lista indicata.</li>
     * </ul>
     * </p>
     *
     * @param listId     (Opzionale) ID della lista per filtrare le campagne. Se {@code null},
     * tenta il recupero globale per l'organizzazione.
     * @param authUserId ID dell'utente autenticato che effettua la richiesta.
     * @return           Un {@link Set} di {@link CampaignSummaryDto} contenente le campagne visibili.
     * @throws NotFoundException   Se l'utente non esiste o se la lista specificata non appartiene
     * all'organizzazione dell'utente.
     * @throws ForbiddenException  Se l'utente non è associato a un'organizzazione o non dispone
     * dei permessi necessari per l'ambito richiesto.
     */
    Set<CampaignSummaryDto> getAll(@Nullable Long listId, Long authUserId);

    /**
     * Recupera i dettagli sintetici di una singola campagna previa verifica dei permessi di accesso.
     * <p>
     * Il metodo applica una politica di accesso a cascata (short-circuit):
     * <ol>
     * <li><b>Livello Organizzazione:</b> Accesso consentito se l'utente possiede il permesso globale {@code view_all_campaign_organization}.</li>
     * <li><b>Livello Lista:</b> Accesso consentito se l'utente possiede il permesso {@code view_all_campaign_list} sulla lista associata alla campagna.</li>
     * <li><b>Livello Candidato:</b> Accesso consentito se l'utente è registrato come candidato all'interno della campagna stessa.</li>
     * </ol>
     * </p>
     * <p>
     * Viene inoltre effettuato un controllo di integrità per garantire che la campagna appartenga
     * alla stessa organizzazione dell'utente autenticato, prevenendo tentativi di accesso cross-org.
     * </p>
     *
     * @param campaignId  L'identificativo univoco della campagna da recuperare.
     * @param authUserId  L'identificativo dell'utente che richiede l'operazione.
     * @return            Un {@link CampaignSummaryDto} contenente i dati della campagna.
     * @throws NotFoundException   Se l'utente o la campagna non vengono trovati nel sistema.
     * @throws ForbiddenException  Se l'utente non appartiene a un'organizzazione, se tenta di accedere
     * a una campagna di un'altra organizzazione, o se non soddisfa
     * nessuno dei criteri di autorizzazione sopra elencati.
     */
    CampaignSummaryDto get(Long campaignId, Long authUserId);
}
