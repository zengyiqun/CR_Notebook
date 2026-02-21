package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.CalendarEventDTO;
import com.cr.notebook.entity.CalendarEvent;
import com.cr.notebook.mapper.CalendarEventMapper;
import com.cr.notebook.tenant.TenantContext;
import com.cr.notebook.tenant.TenantType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarEventServiceTest {

    @Mock
    private CalendarEventMapper calendarEventMapper;

    @InjectMocks
    private CalendarEventService calendarEventService;

    @BeforeEach
    void setUp() {
        TenantContext.set(1L, TenantType.PERSONAL);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listEvents_shouldReturnEventsInRange() {
        CalendarEvent event = buildEvent(1L, "Meeting", LocalDate.of(2026, 3, 1));
        when(calendarEventMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(event));

        List<CalendarEventDTO> result = calendarEventService.listEvents(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Meeting");
    }

    @Test
    void createEvent_shouldInsertAndReturn() {
        when(calendarEventMapper.insert(any(CalendarEvent.class))).thenAnswer(inv -> {
            CalendarEvent e = inv.getArgument(0);
            e.setId(1L);
            e.setCreatedAt(LocalDateTime.now());
            return 1;
        });

        CalendarEventDTO input = new CalendarEventDTO();
        input.setTitle("Birthday Party");
        input.setEventDate(LocalDate.of(2026, 6, 15));
        input.setEventTime(LocalTime.of(18, 0));

        CalendarEventDTO result = calendarEventService.createEvent(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Birthday Party");
        assertThat(result.getColor()).isEqualTo("#6366f1");
    }

    @Test
    void updateEvent_shouldModify() {
        CalendarEvent existing = buildEvent(1L, "Old", LocalDate.of(2026, 3, 1));
        when(calendarEventMapper.selectById(1L)).thenReturn(existing);
        when(calendarEventMapper.updateById(any(CalendarEvent.class))).thenReturn(1);

        CalendarEventDTO update = new CalendarEventDTO();
        update.setTitle("New Title");
        update.setColor("#ff0000");

        CalendarEventDTO result = calendarEventService.updateEvent(1L, update);

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getColor()).isEqualTo("#ff0000");
    }

    @Test
    void deleteEvent_notFound_shouldThrow() {
        when(calendarEventMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> calendarEventService.deleteEvent(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteEvent_wrongTenant_shouldThrow() {
        CalendarEvent event = buildEvent(1L, "Event", LocalDate.now());
        event.setTenantId(999L);
        when(calendarEventMapper.selectById(1L)).thenReturn(event);

        assertThatThrownBy(() -> calendarEventService.deleteEvent(1L))
                .isInstanceOf(SecurityException.class);
    }

    private CalendarEvent buildEvent(Long id, String title, LocalDate date) {
        CalendarEvent event = CalendarEvent.builder()
                .title(title).eventDate(date).color("#6366f1").build();
        event.setId(id);
        event.setTenantId(1L);
        event.setTenantType(TenantType.PERSONAL);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        return event;
    }
}
