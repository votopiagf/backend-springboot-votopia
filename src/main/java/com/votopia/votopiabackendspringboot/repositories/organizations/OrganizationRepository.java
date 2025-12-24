package com.votopia.votopiabackendspringboot.repositories.organizations;

import com.votopia.votopiabackendspringboot.entities.organizations.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findOrganizationByCode(String code);

    Organization findOrganizationById(Long id);
}
