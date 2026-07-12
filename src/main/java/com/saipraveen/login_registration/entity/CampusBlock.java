package com.saipraveen.login_registration.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "campus_blocks")
public class CampusBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    public CampusBlock() {}

    public CampusBlock(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
