package com.votopia.votopiabackendspringboot.repositories.campaigns;

import com.votopia.votopiabackendspringboot.entities.campaigns.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findById(Integer integer);
}
