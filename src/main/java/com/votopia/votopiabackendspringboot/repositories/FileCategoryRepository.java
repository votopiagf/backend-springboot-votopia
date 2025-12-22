package com.votopia.votopiabackendspringboot.repositories;

import com.votopia.votopiabackendspringboot.entities.FileCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileCategoryRepository extends JpaRepository<FileCategory, Long> {
}
