package com.votopia.votopiabackendspringboot.repositories;

import aj.org.objectweb.asm.commons.Remapper;
import com.votopia.votopiabackendspringboot.entities.Organization;
import com.votopia.votopiabackendspringboot.entities.Permission;
import com.votopia.votopiabackendspringboot.entities.User;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.lang.ScopedValue;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> { ;
    boolean existsByIdAndDeletedFalse(Long userId);

    @Query("SELECT COUNT(p) > 0 FROM User u " +
            "JOIN u.roles r " +
            "JOIN r.permissions p " +
            "WHERE u.id = :userId AND p.name = :permissionName AND u.deleted = false")
    boolean hasPermission(@Param("userId") Long userId, @Param("permissionName") String permissionName);

    // Recupera tutti i permessi (Sostituisce get_user_permissions)
    @Query("SELECT DISTINCT p FROM User u " +
            "JOIN u.roles r " +
            "JOIN r.permissions p " +
            "WHERE u.id = :userId AND u.deleted = false")
    List<Permission> findAllPermissionsByUserId(@Param("userId") Long userId);

    Optional<Object> findUsersByEmailAndOrg(String email, Organization org);

    <Optional>User findUsersByEmail(String email);

    boolean existsByEmailAndOrgAndDeletedFalse(@NonNull String email, Organization org);

    Collection<User> findAllByOrg(Organization org);

    Collection<User> findAllByOrgId(Long orgId);

    Collection<User> findAllByListsId(Long listsId);
}