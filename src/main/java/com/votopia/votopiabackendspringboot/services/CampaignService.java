package com.votopia.votopiabackendspringboot.services;

import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignAddCandidateDto;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignCreateDto;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignUpdateDto;
import com.votopia.votopiabackendspringboot.entities.campaigns.Campaign;
import com.votopia.votopiabackendspringboot.entities.campaigns.CandidateCampaign;
import com.votopia.votopiabackendspringboot.entities.campaigns.CandidatePositionCampaign;
import com.votopia.votopiabackendspringboot.exceptions.BadRequestException;
import com.votopia.votopiabackendspringboot.exceptions.ConflictException;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.NotNull;

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

    /**
     * Associa un candidato esistente a una campagna elettorale, gestendo opzionalmente la sua posizione.
     * <p>
     * Il metodo esegue i seguenti controlli di integrità e sicurezza:
     * <ol>
     * <li>Verifica l'esistenza dell'utente autenticato e della campagna.</li>
     * <li>Assicura che la campagna appartenga all'organizzazione dell'utente (Isolamento Org).</li>
     * <li>Verifica i permessi (globale {@code add_candidate_in_campaign_organization}
     * o specifico per lista {@code add_candidate_in_campaign_list}).</li>
     * <li>Controlla che il candidato non sia già associato alla campagna (Prevenzione duplicati).</li>
     * </ol>
     * </p>
     * <p>
     * Se nel {@code dto} è presente una {@code positionInList}, viene creato e persistito un record
     * nell'entità {@link CandidatePositionCampaign} collegato all'associazione appena creata.
     * </p>
     *
     * @param dto         Oggetto di trasporto dati contenente l'ID della campagna, l'ID del candidato
     * e l'eventuale posizione in lista.
     * @param authUserId  ID dell'utente amministratore/gestore che esegue l'operazione.
     * @throws NotFoundException    Se la campagna o il candidato specificati non esistono.
     * @throws ForbiddenException   Se l'utente non ha i permessi necessari o se tenta di operare
     * su una campagna di un'altra organizzazione.
     * @throws BadRequestException  Se il candidato è già presente nella campagna indicata.
     */
    void addCandidateInCampaign(@NotNull CampaignAddCandidateDto dto, Long authUserId);

    /**
     * Rimuove un candidato da una campagna elettorale esistente.
     * <p>
     * L'operazione segue questo flusso di validazione:
     * <ol>
     * <li>Verifica l'esistenza dell'utente operatore e della campagna.</li>
     * <li>Assicura l'isolamento multi-tenant controllando che la campagna appartenga all'organizzazione dell'operatore.</li>
     * <li>Verifica i privilegi di gestione ({@code manager_candidate_in_campaign_organization} o {@code manager_candidate_in_campaign_list}).</li>
     * <li>Identifica l'associazione specifica {@link CandidateCampaign} tra il candidato e la campagna.</li>
     * </ol>
     * </p>
     * <p><b>Effetti collaterali:</b> La rimozione comporta la cancellazione automatica di eventuali record
     * di posizione associati nella tabella {@code candidate_position_campaign}.</p>
     *
     * @param candidateId ID del candidato da rimuovere.
     * @param campaignId  ID della campagna da cui rimuovere il candidato.
     * @param authUserId  ID dell'utente autenticato che richiede l'operazione.
     * @throws NotFoundException    Se la campagna non esiste o se il candidato non è associato ad essa.
     * @throws ForbiddenException   Se l'operatore non ha i permessi o tenta di accedere a una campagna fuori dalla propria Org.
     */
    void removeCandidateFromCampaign(@NotNull Long candidateId, @NotNull Long campaignId, @NotNull Long authUserId);

    /**
     * Elimina definitivamente una campagna e tutte le entità correlate dal sistema.
     * <p>
     * Il metodo garantisce l'integrità referenziale del database rimuovendo manualmente
     * le dipendenze in ordine gerarchico inverso (Nipoti -> Figli -> Padre):
     * <ol>
     * <li>Recupera ed elimina tutte le posizioni in lista ({@link CandidatePositionCampaign})
     * associate ai candidati della campagna.</li>
     * <li>Rimuove le associazioni intermedie tra candidati e campagna ({@link CandidateCampaign}).</li>
     * <li>Elimina infine l'entità {@link Campaign}.</li>
     * </ol>
     * </p>
     * <p><b>Sicurezza:</b> Verifica che la campagna appartenga all'organizzazione dell'utente
     * e che quest'ultimo possieda i permessi {@code delete_campaign_organization} o {@code delete_campaign_list}.</p>
     *
     * @param campaignId ID della campagna da eliminare.
     * @param authUserId ID dell'utente che richiede l'operazione.
     * @throws NotFoundException    Se l'utente o la campagna non esistono.
     * @throws ForbiddenException   Se l'utente non ha i permessi o tenta di eliminare
     * una campagna di un'altra organizzazione.
     */
    void deleteCampaign(@NotNull Long campaignId, Long authUserId);

    /**
     * Aggiorna i dettagli di una campagna esistente applicando modifiche parziali.
     * <p>
     * Il metodo esegue i seguenti controlli di sicurezza:
     * 1. Verifica l'esistenza e l'appartenenza della campagna all'organizzazione dell'utente (Multi-tenancy).
     * 2. Valida i permessi gerarchici: l'utente deve possedere 'update_campaign_organization'
     * a livello globale o 'update_campaign_list' sulla lista specifica.
     * </p>
     *
     * @param dto        Oggetto {@link CampaignUpdateDto} contenente l'ID della campagna e i campi da aggiornare.
     * @param authUserId ID dell'utente autenticato ricavato dal Security Context.
     * @return           Un {@link CampaignSummaryDto} che rappresenta lo stato aggiornato della campagna.
     * @throws NotFoundException   Se la campagna o l'utente non vengono trovati.
     * @throws ForbiddenException  In caso di violazione della multi-tenancy o permessi insufficienti.
     */
    CampaignSummaryDto update(CampaignUpdateDto dto, Long authUserId);
}
