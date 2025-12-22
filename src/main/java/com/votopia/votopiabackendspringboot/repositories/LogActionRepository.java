package com.votopia.votopiabackendspringboot.repositories;

import com.votopia.votopiabackendspringboot.entities.LogAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogActionRepository extends JpaRepository<LogAction, Long> {
}
