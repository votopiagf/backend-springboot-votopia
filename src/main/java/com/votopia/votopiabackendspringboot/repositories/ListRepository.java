package com.votopia.votopiabackendspringboot.repositories;

import com.votopia.votopiabackendspringboot.entities.List;
import com.votopia.votopiabackendspringboot.entities.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface ListRepository extends JpaRepository<List, Long> {

    Optional<List> findByIdAndOrgId(Long id, Long orgId);

    java.util.List<List> findByOrg(Organization org);

    Set<List> findListsByOrg(Organization org);
}
