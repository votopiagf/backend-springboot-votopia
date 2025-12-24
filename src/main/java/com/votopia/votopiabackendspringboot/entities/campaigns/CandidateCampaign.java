package com.votopia.votopiabackendspringboot.entities.campaigns;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "candidates_campaigns")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CandidateCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @OneToMany(mappedBy = "candidateCampaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CandidatePositionCampaign> positions = new HashSet<>();
}