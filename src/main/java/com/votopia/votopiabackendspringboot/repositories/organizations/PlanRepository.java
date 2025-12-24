package com.votopia.votopiabackendspringboot.repositories.organizations;

import com.votopia.votopiabackendspringboot.entities.organizations.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
}
