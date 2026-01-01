package com.votopia.votopiabackendspringboot.dtos.candidate;

import com.votopia.votopiabackendspringboot.dtos.user.UserSummaryDto;
import com.votopia.votopiabackendspringboot.entities.campaigns.Candidate;
import com.votopia.votopiabackendspringboot.entities.files.File;

public record CandidateSummaryDto(
        Long id,
        UserSummaryDto user,
        String schoolClass,
        File photoFileId
) {
    public CandidateSummaryDto(Candidate c){
        this(
                c.getId(),
                new UserSummaryDto(c.getUser()),
                c.getSchoolClass(),
                c.getPhotoFileId()
        );
    }
}