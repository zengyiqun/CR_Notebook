package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.CalendarEventDTO;
import com.cr.notebook.entity.CalendarEvent;
import com.cr.notebook.mapper.CalendarEventMapper;
import com.cr.notebook.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventMapper calendarEventMapper;

    public List<CalendarEventDTO> listEvents(LocalDate startDate, LocalDate endDate) {
        List<CalendarEvent> events = calendarEventMapper.selectList(
                new LambdaQueryWrapper<CalendarEvent>()
                        .eq(CalendarEvent::getTenantId, TenantContext.getTenantId())
                        .eq(CalendarEvent::getTenantType, TenantContext.getTenantType())
                        // 命中条件：
                        // 1) 起始日期在查询区间内；或
                        // 2) 跨天事件与查询区间有重叠（[eventDate, endDate] ∩ [startDate, endDate] 非空）。
                        .and(w -> w
                                .between(CalendarEvent::getEventDate, startDate, endDate)
                                .or(o -> o
                                        .isNotNull(CalendarEvent::getEndDate)
                                        .le(CalendarEvent::getEventDate, endDate)
                                        .ge(CalendarEvent::getEndDate, startDate)))
                        .orderByAsc(CalendarEvent::getEventDate)
                        .orderByAsc(CalendarEvent::getEventTime));
        return events.stream().map(this::toDTO).toList();
    }

    @Transactional
    public CalendarEventDTO createEvent(CalendarEventDTO dto) {
        CalendarEvent event = CalendarEvent.builder()
                .title(dto.getTitle())
                .eventDate(dto.getEventDate())
                .eventTime(dto.getEventTime())
                .endDate(dto.getEndDate())
                .endTime(dto.getEndTime())
                .description(dto.getDescription())
                .color(dto.getColor() != null ? dto.getColor() : "#6366f1")
                .build();
        event.setTenantId(TenantContext.getTenantId());
        event.setTenantType(TenantContext.getTenantType());
        calendarEventMapper.insert(event);
        return toDTO(event);
    }

    @Transactional
    public CalendarEventDTO updateEvent(Long id, CalendarEventDTO dto) {
        CalendarEvent event = calendarEventMapper.selectById(id);
        if (event == null) throw new IllegalArgumentException("Calendar event not found");
        checkTenant(event);

        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        event.setEventTime(dto.getEventTime());
        event.setEndDate(dto.getEndDate());
        event.setEndTime(dto.getEndTime());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getColor() != null) event.setColor(dto.getColor());

        calendarEventMapper.updateById(event);
        return toDTO(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        CalendarEvent event = calendarEventMapper.selectById(id);
        if (event == null) throw new IllegalArgumentException("Calendar event not found");
        checkTenant(event);
        calendarEventMapper.deleteById(id);
    }

    private void checkTenant(CalendarEvent event) {
        if (!event.getTenantId().equals(TenantContext.getTenantId())
                || event.getTenantType() != TenantContext.getTenantType()) {
            throw new SecurityException("Access denied");
        }
    }

    private CalendarEventDTO toDTO(CalendarEvent e) {
        CalendarEventDTO dto = new CalendarEventDTO();
        dto.setId(e.getId());
        dto.setTitle(e.getTitle());
        dto.setEventDate(e.getEventDate());
        dto.setEventTime(e.getEventTime());
        dto.setEndDate(e.getEndDate());
        dto.setEndTime(e.getEndTime());
        dto.setDescription(e.getDescription());
        dto.setColor(e.getColor());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }
}
