package com.votopia.votopiabackendspringboot.repositories;

import com.votopia.votopiabackendspringboot.entities.List;
import com.votopia.votopiabackendspringboot.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Trova il livello massimo tra i ruoli posseduti dall'utente in una specifica LISTA.
     * Utilizziamo COALESCE per restituire 0 invece di null se l'utente non ha ruoli.
     */
    @Query("SELECT COALESCE(MAX(r.level), 0) FROM User u JOIN u.roles r " +
            "WHERE u.id = :userId AND r.list.id = :listId")
    int findMaxLevelByUserIdAndListId(@Param("userId") Long userId, @Param("listId") Long listId);

    /**
     * Trova il livello massimo tra i ruoli posseduti dall'utente a livello ORGANIZZAZIONE.
     * Filtriamo i ruoli dove la lista è NULL (ruoli globali).
     */
    @Query("SELECT COALESCE(MAX(r.level), 0) FROM User u JOIN u.roles r " +
            "WHERE u.id = :userId AND r.organization.id = :orgId AND r.list IS NULL")
    int findMaxLevelByUserIdAndOrgId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    Set<Role> findAllByOrganizationId(Long organizationId);

    Set<Role> findAllByListId(Long listId);
}