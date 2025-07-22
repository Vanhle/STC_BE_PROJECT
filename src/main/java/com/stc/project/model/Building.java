package com.stc.project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Table(name = "building")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Building extends IdEntity {

    String code;
    String name;
    Integer numberOfBasements;
    Integer numberOfLivingFloors;
    Integer numberOfApartments;
    String image;
    String description;

    @Transient // trường này không được lưu xuống DB, chỉ dùng tạm thời trong Java để nhận dữ liệu.
//    @JsonProperty("project_id") // Khi nào cần nạp data thì mới baatj dòng này
    Long projectId; // Trường phụ, chỉ dùng để nhận từ client\ không phải là một trường thật, mà là một @Transient,

    @PostLoad
    public void fillProjectId() {
        if (project != null) {
            this.projectId = project.getId();
        }
    }

    // (1 building thuộc 1 project)
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    Project project;

    // Mối quan hệ với Apartment (1 building có nhiều apt)
    @JsonIgnore
    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Apartment> apartments;

}
