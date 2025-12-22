package com.votopia.votopiabackendspringboot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "positions")
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @ManyToOne()
    @JoinColumn(name = "list_id")
    private List list;

    @ManyToOne()
    @JoinColumn(name = "org_id")
    private Organization org;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }
}
