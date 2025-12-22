package com.votopia.votopiabackendspringboot.repositories;

import com.votopia.votopiabackendspringboot.entities.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
}
