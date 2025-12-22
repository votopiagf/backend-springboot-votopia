package com.votopia.votopiabackendspringboot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "log_actions")
@NoArgsConstructor
@AllArgsConstructor
public class LogAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 100, nullable = false)
    private String label;

    @Column(nullable = false)
    private Level level = Level.INFO;

    public enum Level{
        INFO,
        WARNING,
        ERROR
    }
}
