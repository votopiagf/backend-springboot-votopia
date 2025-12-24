package com.votopia.votopiabackendspringboot.repositories.organizations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.votopia.votopiabackendspringboot.entities.organizations.Module;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {
}
