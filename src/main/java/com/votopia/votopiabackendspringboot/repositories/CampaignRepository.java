package com.votopia.votopiabackendspringboot.repositories;

import com.votopia.votopiabackendspringboot.entities.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    Set<Campaign> findAllByListOrgId(Long listOrgId);

    Set<Campaign> findAllByListId(Long listId);
}
