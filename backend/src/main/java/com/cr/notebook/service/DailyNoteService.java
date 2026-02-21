package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.DailyNoteDTO;
import com.cr.notebook.entity.DailyNote;
import com.cr.notebook.mapper.DailyNoteMapper;
import com.cr.notebook.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyNoteService {

    private final DailyNoteMapper dailyNoteMapper;

    @Transactional
    public DailyNoteDTO getDailyNote(LocalDate date) {
        DailyNote note = dailyNoteMapper.selectOne(
                new LambdaQueryWrapper<DailyNote>()
                        .eq(DailyNote::getTenantId, TenantContext.getTenantId())
                        .eq(DailyNote::getTenantType, TenantContext.getTenantType())
                        .eq(DailyNote::getNoteDate, date));

        if (note == null) {
            note = DailyNote.builder()
                    .noteDate(date)
                    .content("")
                    .build();
            note.setTenantId(TenantContext.getTenantId());
            note.setTenantType(TenantContext.getTenantType());
            dailyNoteMapper.insert(note);
        }
        return toDTO(note);
    }

    @Transactional
    public DailyNoteDTO updateDailyNote(LocalDate date, DailyNoteDTO dto) {
        DailyNote note = dailyNoteMapper.selectOne(
                new LambdaQueryWrapper<DailyNote>()
                        .eq(DailyNote::getTenantId, TenantContext.getTenantId())
                        .eq(DailyNote::getTenantType, TenantContext.getTenantType())
                        .eq(DailyNote::getNoteDate, date));
        if (note == null) throw new IllegalArgumentException("Daily note not found");
        checkTenant(note);

        if (dto.getContent() != null) note.setContent(dto.getContent());
        if (dto.getWeather() != null) note.setWeather(dto.getWeather());
        if (dto.getMood() != null) note.setMood(dto.getMood());

        dailyNoteMapper.updateById(note);
        return toDTO(note);
    }

    public List<String> listDatesWithContent(LocalDate from, LocalDate to) {
        List<DailyNote> notes = dailyNoteMapper.selectList(
                new LambdaQueryWrapper<DailyNote>()
                        .eq(DailyNote::getTenantId, TenantContext.getTenantId())
                        .eq(DailyNote::getTenantType, TenantContext.getTenantType())
                        .ge(DailyNote::getNoteDate, from)
                        .le(DailyNote::getNoteDate, to)
                        .isNotNull(DailyNote::getContent)
                        .ne(DailyNote::getContent, "")
                        .select(DailyNote::getNoteDate));
        return notes.stream()
                .map(n -> n.getNoteDate().toString())
                .collect(Collectors.toList());
    }

    private void checkTenant(DailyNote note) {
        if (!note.getTenantId().equals(TenantContext.getTenantId())
                || note.getTenantType() != TenantContext.getTenantType()) {
            throw new SecurityException("Access denied");
        }
    }

    private DailyNoteDTO toDTO(DailyNote n) {
        DailyNoteDTO dto = new DailyNoteDTO();
        dto.setId(n.getId());
        dto.setNoteDate(n.getNoteDate());
        dto.setContent(n.getContent());
        dto.setWeather(n.getWeather());
        dto.setMood(n.getMood());
        dto.setCreatedAt(n.getCreatedAt());
        dto.setUpdatedAt(n.getUpdatedAt());
        return dto;
    }
}
