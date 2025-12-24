package com.votopia.votopiabackendspringboot.services.impl;

import com.votopia.votopiabackendspringboot.dtos.organization.OrganizationSummaryDto;
import com.votopia.votopiabackendspringboot.exceptions.BadRequestException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import com.votopia.votopiabackendspringboot.repositories.organizations.OrganizationRepository;
import com.votopia.votopiabackendspringboot.services.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public OrganizationSummaryDto getOrganizationByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Il codice organizzazione è obbligatorio.");
        }

        return organizationRepository.findOrganizationByCode(code)
                .map(OrganizationSummaryDto::new)
                .orElseThrow(() -> new NotFoundException("Organizzazione non trovata per il codice: " + code));
    }
}