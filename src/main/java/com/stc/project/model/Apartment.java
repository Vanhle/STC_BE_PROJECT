package com.stc.project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;


@Entity
@Table(name = "apartment")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Apartment extends IdEntity {

    String code;
    String name;
    Integer atFloor;
    Float totalArea;
    Float price;
    String description;

    @Transient // trường này không được lưu xuống DB, chỉ dùng tạm thời trong Java để nhận dữ liệu.
//    @JsonProperty("building_id") // Khi nào cần nạp data thì mới baatj dòng này
    private Long buildingId; // Trường phụ, chỉ dùng để nhận từ client

    @PostLoad
    public void fillBuildingId() {
        if (building != null) {
            this.buildingId = building.getId();
        }
    }

    // Mối quan hệ với BuildingEntity (1 apt thuộc 1 building)
    @JsonIgnore // khi serialize Apartment, không bao gồm building => cắt được vòng lặp.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    Building building;

    // Cột này dùng cho fe
    @JsonProperty("buildingName")
    public String getBuildingName() {
        return building != null ? building.getName() : null;
    }

}
