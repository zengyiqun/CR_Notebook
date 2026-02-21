package com.cr.notebook.dto;

import lombok.Data;

import java.util.List;

@Data
public class StatsDTO {
    private long noteCount;
    private long folderCount;
    private long taskCount;
    private long taskCompletedCount;
    private long dailyNoteCount;
    private long calendarEventCount;
    private long whiteboardCount;

    private Long orgId;
    private String orgName;
    private int memberCount;
    private List<MemberStatsDTO> memberStats;
    private List<DailyStatsDTO> dailyStats;

    @Data
    public static class MemberStatsDTO {
        private Long userId;
        private String username;
        private String displayName;
        private String role;
        private long noteCount;
        private long taskCount;
    }

    @Data
    public static class DailyStatsDTO {
        private String date;
        private long noteCount;
        private long taskCount;
        private long taskCompletedCount;
    }
}

