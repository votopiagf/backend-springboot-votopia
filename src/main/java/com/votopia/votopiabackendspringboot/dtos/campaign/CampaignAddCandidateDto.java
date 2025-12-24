package com.votopia.votopiabackendspringboot.dtos.campaign;

import jakarta.validation.constraints.NotNull;

public record CampaignAddCandidateDto (
    @NotNull(message = "L'id del candidato non può essere null")
    Long candidateId,

    @NotNull(message = "L'id della campagna non può essere null")
    Long campaignId,

    Integer positionInList,

    Integer positonId
) {}