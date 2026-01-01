package com.votopia.votopiabackendspringboot.dtos.candidate;

import com.votopia.votopiabackendspringboot.dtos.file.FileSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.list.ListSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserSummaryDto;
import com.votopia.votopiabackendspringboot.entities.campaigns.Candidate;

import java.time.LocalDateTime;

public record CandidateDetailDto(
        Long id,
        ListSummaryDto list,
        UserSummaryDto user,
        String schoolClass,
        FileSummaryDto photoFileId,
        String bio,
        LocalDateTime createdAt
){
    public CandidateDetailDto(Candidate c){
        this(
                c.getId(),
                new ListSummaryDto(c.getList()),
                new UserSummaryDto(c.getUser()),
                c.getSchoolClass(),
                new FileSummaryDto(c.getPhotoFileId()),
                c.getBio(),
                c.getCreatedAt()
        );
    }
}
