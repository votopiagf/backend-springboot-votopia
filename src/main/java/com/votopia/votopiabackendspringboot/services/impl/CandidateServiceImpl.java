package com.votopia.votopiabackendspringboot.services.impl;

import com.votopia.votopiabackendspringboot.dtos.candidate.CandidateCreateDto;
import com.votopia.votopiabackendspringboot.dtos.candidate.CandidateSummaryDto;
import com.votopia.votopiabackendspringboot.entities.auth.User;
import com.votopia.votopiabackendspringboot.entities.campaigns.Candidate;
import com.votopia.votopiabackendspringboot.entities.files.File;
import com.votopia.votopiabackendspringboot.entities.lists.List;
import com.votopia.votopiabackendspringboot.exceptions.BadRequestException;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import com.votopia.votopiabackendspringboot.repositories.auth.UserRepository;
import com.votopia.votopiabackendspringboot.repositories.campaigns.CandidateRepository;
import com.votopia.votopiabackendspringboot.repositories.files.FileRepository;
import com.votopia.votopiabackendspringboot.repositories.lists.ListRepository;
import com.votopia.votopiabackendspringboot.services.CandidateService;
import com.votopia.votopiabackendspringboot.services.auth.AuthService;
import com.votopia.votopiabackendspringboot.services.auth.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CandidateServiceImpl implements CandidateService {
    @Autowired private CandidateRepository candidateRepository;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private PermissionService permissionService;
    @Autowired private ListRepository listRepository;
    @Autowired private FileRepository fileRepository;

    @Override
    @Transactional
    public CandidateSummaryDto create(CandidateCreateDto dto, Long authUserId){
        User authUser = authService.getAuthenticatedUser(authUserId);

        List listTarget = listRepository.findByIdAndOrgId(dto.listId(), authUser.getOrg().getId())
                        .orElseThrow(() -> new NotFoundException("Lista non trovata nell'organizzazione"));

        permissionService.validatePermission(authUserId, dto.listId(), "create_candidate_organization", "create_candidate_list", "Non hai i permessi per creare un candidato");

        File filePhoto = fileRepository.findById(dto.photoFileId())
                .orElseThrow(() -> new NotFoundException("File della foto non trovato"));

        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new NotFoundException("L'utente associato al candidato non trovato"));

        if (!permissionService.hasPermission(dto.userId(), "can_be_candidate")) throw new ForbiddenException("Questo utente non può essere un candidato");

        if (candidateRepository.existsByUserIdAndListId(dto.userId(), dto.listId())) throw new BadRequestException("Esiste già questo candidato in questa lista");

        Candidate candidate = new Candidate();
        candidate.setUser(user);
        candidate.setSchoolClass(dto.schoolClass());
        candidate.setBio(dto.bio());
        candidate.setList(listTarget);
        candidate.setPhotoFileId(filePhoto);

        Candidate created = candidateRepository.save(candidate);
        return new CandidateSummaryDto(created);
    }

    @Override
    @Transactional
    public Set<CandidateSummaryDto> getAllByList(Long listId, Long authUserId){
        User user = authService.getAuthenticatedUser(authUserId);
        List listTarget = listRepository.findByIdAndOrgId(listId, user.getOrg().getId())
                .orElseThrow(() -> new NotFoundException("Lista non trovata nell'organizzazione"));
        permissionService.validatePermission(authUserId, listId, "view_all_candidate_organization", "view_all_candidate_list", "Non hai i permessi per vedere tutti i candidati");

        Set<Candidate> candidates = candidateRepository.findAllByListId(listId);

        return candidates.stream()
                .map(CandidateSummaryDto::new)
                .collect(Collectors.toSet());
    }
}
