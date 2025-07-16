package com.stc.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "invalidated_token")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class InvalidatedToken {
    @Id
    String id;
    Date expiredTime;

}
