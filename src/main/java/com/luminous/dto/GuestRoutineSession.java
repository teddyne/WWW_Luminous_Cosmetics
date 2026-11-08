package com.luminous.dto;

import java.io.Serializable;

public record GuestRoutineSession(
        int quizId,
        String skinType,
        String goals,
        String budget,
        String routineJson) implements Serializable {

    public static final String SESSION_KEY = "guestRoutine";
}
