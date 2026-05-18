package es.udc.fic.corpuslab.modules.project.shared.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProjectParticipantIaaGroup {
    GROUP_A,
    GROUP_B,
    @Deprecated
    GROUP_1,
    @Deprecated
    GROUP_2;

    @JsonCreator
    public static ProjectParticipantIaaGroup fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value.trim().toUpperCase()) {
            case "GROUP_A", "GROUP_1" -> GROUP_A;
            case "GROUP_B", "GROUP_2" -> GROUP_B;
            default -> ProjectParticipantIaaGroup.valueOf(value.trim().toUpperCase());
        };
    }

    @JsonValue
    public String jsonValue() {
        if (isGroupA()) {
            return GROUP_A.name();
        }
        if (isGroupB()) {
            return GROUP_B.name();
        }
        return name();
    }

    public boolean isGroupA() {
        return this == GROUP_A || this == GROUP_1;
    }

    public boolean isGroupB() {
        return this == GROUP_B || this == GROUP_2;
    }
}
