package com.example.sesac_spring_boot_study.domain;


public class StudyMember {

    private final Long id;
    private final String name;
    private String status;

    public StudyMember(Long id, String name, String status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
