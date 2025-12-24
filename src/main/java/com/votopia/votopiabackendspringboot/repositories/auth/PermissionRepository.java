package com.votopia.votopiabackendspringboot.repositories.auth;

import com.votopia.votopiabackendspringboot.entities.auth.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
}
