package com.votopia.votopiabackendspringboot.repositories.campaigns;

import com.votopia.votopiabackendspringboot.entities.campaigns.CandidateCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandidateCampaignRepository extends JpaRepository<CandidateCampaign, Long> {
}
