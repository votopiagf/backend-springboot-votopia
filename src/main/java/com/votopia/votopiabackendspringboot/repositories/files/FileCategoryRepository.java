package com.votopia.votopiabackendspringboot.repositories.files;

import com.votopia.votopiabackendspringboot.entities.files.FileCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileCategoryRepository extends JpaRepository<FileCategory, Long> {
}
