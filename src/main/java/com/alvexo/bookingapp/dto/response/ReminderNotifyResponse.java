package com.alvexo.bookingapp.dto.response;

import java.util.List;

public record ReminderNotifyResponse(
        int notifiedCount,
        int skipped,
        int failed,
        List<ReminderNotifyItemResult> reminders
) {}
