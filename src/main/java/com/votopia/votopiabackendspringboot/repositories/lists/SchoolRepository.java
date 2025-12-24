package com.votopia.votopiabackendspringboot.repositories.lists;

import com.votopia.votopiabackendspringboot.entities.lists.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {
}
