package com.votopia.votopiabackendspringboot.repositories.log;

import com.votopia.votopiabackendspringboot.entities.log.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {
}
