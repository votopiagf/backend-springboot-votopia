package com.votopia.votopiabackendspringboot.repositories.campaigns;

import com.votopia.votopiabackendspringboot.entities.campaigns.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    boolean existsByUserIdAndListId(Long userId, Long listId);

    Set<Candidate> findAllByListId(Long listId);
}
