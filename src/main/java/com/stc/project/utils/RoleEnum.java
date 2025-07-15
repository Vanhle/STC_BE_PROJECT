package com.stc.project.utils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum RoleEnum {
    ADMIN("ADMIN SYSTEM"),
    USER("MANAGER SYSTEM");
    String description;
}
