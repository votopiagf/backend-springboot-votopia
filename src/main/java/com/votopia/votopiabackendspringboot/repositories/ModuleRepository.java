package com.votopia.votopiabackendspringboot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.votopia.votopiabackendspringboot.entities.Module;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {
}
