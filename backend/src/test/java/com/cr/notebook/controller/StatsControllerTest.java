package com.cr.notebook.controller;

import com.cr.notebook.dto.StatsDTO;
import com.cr.notebook.service.StatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StatsControllerTest {

    private MockMvc mockMvc;

    @Mock private StatsService statsService;
    @InjectMocks private StatsController statsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(statsController).build();
    }

    @Test
    void getPersonalStats_shouldReturn200() throws Exception {
        StatsDTO stats = new StatsDTO();
        stats.setNoteCount(5);
        stats.setFolderCount(2);
        stats.setTaskCount(8);
        stats.setTaskCompletedCount(3);
        stats.setDailyNoteCount(4);
        stats.setCalendarEventCount(1);
        stats.setWhiteboardCount(1);

        when(statsService.getPersonalStats(7)).thenReturn(stats);

        mockMvc.perform(get("/api/stats/personal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noteCount").value(5))
                .andExpect(jsonPath("$.folderCount").value(2))
                .andExpect(jsonPath("$.taskCount").value(8))
                .andExpect(jsonPath("$.taskCompletedCount").value(3))
                .andExpect(jsonPath("$.dailyNoteCount").value(4))
                .andExpect(jsonPath("$.calendarEventCount").value(1))
                .andExpect(jsonPath("$.whiteboardCount").value(1));
    }

    @Test
    void getOrgStats_shouldReturn200() throws Exception {
        StatsDTO stats = new StatsDTO();
        stats.setOrgId(10L);
        stats.setOrgName("My Org");
        stats.setNoteCount(12);
        stats.setMemberCount(3);
        stats.setMemberStats(List.of());

        when(statsService.getOrgStats(10L, 7)).thenReturn(stats);

        mockMvc.perform(get("/api/stats/org/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgId").value(10))
                .andExpect(jsonPath("$.orgName").value("My Org"))
                .andExpect(jsonPath("$.noteCount").value(12))
                .andExpect(jsonPath("$.memberCount").value(3));
    }

    @Test
    void getOrgStats_notFound_shouldThrow() throws Exception {
        when(statsService.getOrgStats(999L, 7)).thenThrow(new IllegalArgumentException("组织不存在"));

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/stats/org/999")));
    }
}
