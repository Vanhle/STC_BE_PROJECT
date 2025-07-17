package com.stc.project.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "project")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Project extends IdEntity {

    String code;
    String name;
    String address;
    Integer numberOfBlocks;
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate constructionStartDateFrom;
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate expectedCompletionDate;
    String image;
    String description;

    // Một Project có nhiều Building
    @JsonIgnore // trường được đánh dấu @JsonIgnore sẽ không xuất hiện trong JSON.
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Building> buildings;
}
