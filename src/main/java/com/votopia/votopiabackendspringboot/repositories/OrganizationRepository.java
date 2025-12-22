package com.votopia.votopiabackendspringboot.repositories;

import com.votopia.votopiabackendspringboot.entities.Organization;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.lang.ScopedValue;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findOrganizationByCode(String code);

    Organization findOrganizationById(Long id);
}
