package com.votopia.votopiabackendspringboot.repositories.files;

import com.votopia.votopiabackendspringboot.entities.files.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
}
