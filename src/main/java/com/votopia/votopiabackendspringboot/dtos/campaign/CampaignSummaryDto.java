package com.votopia.votopiabackendspringboot.dtos.campaign;

import com.votopia.votopiabackendspringboot.entities.Campaign;
import java.time.LocalDate;

/**
 * DTO per la visualizzazione sintetica di una Campagna.
 */
public record CampaignSummaryDto(
        Long id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        Long listId,
        String listName // Utile per il frontend per evitare join manuali
) {
    /**
     * Costruttore compatto per mappare l'entità Campaign nel DTO.
     * * @param campaign L'entità Campaign da convertire.
     */
    public CampaignSummaryDto(Campaign campaign) {
        this(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                campaign.getList() != null ? campaign.getList().getId() : null,
                campaign.getList() != null ? campaign.getList().getName() : null
        );
    }
}