package com.votopia.votopiabackendspringboot.repositories;

import com.votopia.votopiabackendspringboot.entities.CandidateCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandidateCampaignRepository extends JpaRepository<CandidateCampaign, Long> {
}
