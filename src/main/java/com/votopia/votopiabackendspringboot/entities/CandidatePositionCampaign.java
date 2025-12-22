package com.votopia.votopiabackendspringboot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "candidate_positions_campaign")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CandidatePositionCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "candidate_campaign_id", nullable = false)
    private CandidateCampaign candidateCampaign;

    @Column(name = "position_in_list", nullable = false)
    private Integer positionInList;
}