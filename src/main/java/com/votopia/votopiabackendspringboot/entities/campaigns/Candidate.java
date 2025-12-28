package com.votopia.votopiabackendspringboot.entities.campaigns;

import com.votopia.votopiabackendspringboot.entities.files.File;
import com.votopia.votopiabackendspringboot.entities.auth.User;
import com.votopia.votopiabackendspringboot.entities.lists.List;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "candidates",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"list_id", "user_id"})
        }
)
@Getter
@Setter
public class Candidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private List list;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "school_class", length = 10)
    private String schoolClass;

    @ManyToOne
    @JoinColumn(name = "photo_file_id")
    private File photoFileId;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CandidateCampaign> candidateCampaigns = new HashSet<>();

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }
}
