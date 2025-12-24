package com.votopia.votopiabackendspringboot.repositories.campaigns;

import com.votopia.votopiabackendspringboot.entities.campaigns.CandidatePositionCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface CandidatePositionCampaignRepository extends JpaRepository<CandidatePositionCampaign, Long> {
    Set<CandidatePositionCampaign> findAllByCandidateCampaignId(Long id);
}
