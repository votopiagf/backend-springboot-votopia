package com.votopia.votopiabackendspringboot.services;

import com.votopia.votopiabackendspringboot.dtos.organization.OrganizationSummaryDto;
import com.votopia.votopiabackendspringboot.exceptions.BadRequestException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;

public interface OrganizationService {
    /**
     * Recupera i dettagli pubblici di un'organizzazione partendo dal suo codice univoco.
     * <p>
     * Questo metodo è progettato per essere utilizzato in scenari di pre-autenticazione,
     * consentendo al client di identificare l'organizzazione e caricare le personalizzazioni
     * grafiche (logo, colori) prima del login.
     * </p>
     *
     * @param code Il codice stringa univoco dell'organizzazione (es. "VOTOPIA_01").
     * @return Un {@link OrganizationSummaryDto} con i metadati dell'organizzazione.
     * @throws BadRequestException Se il parametro {@code code} è nullo o vuoto.
     * @throws NotFoundException   Se non esiste alcuna organizzazione associata al codice fornito.
     */
    OrganizationSummaryDto getOrganizationByCode(String code);
}
