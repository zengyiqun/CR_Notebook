package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.DailyNoteDTO;
import com.cr.notebook.entity.DailyNote;
import com.cr.notebook.mapper.DailyNoteMapper;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyNoteServiceTest {

    @Mock
    private DailyNoteMapper dailyNoteMapper;

    @InjectMocks
    private DailyNoteService dailyNoteService;

    @BeforeEach
    void setUp() {
        TenantContext.set(1L, TenantType.PERSONAL);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getDailyNote_existing_shouldReturn() {
        LocalDate date = LocalDate.of(2026, 2, 21);
        DailyNote note = buildDailyNote(1L, date, "Today's thoughts");
        when(dailyNoteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(note);

        DailyNoteDTO result = dailyNoteService.getDailyNote(date);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("Today's thoughts");
    }

    @Test
    void getDailyNote_notExisting_shouldCreateNew() {
        LocalDate date = LocalDate.of(2026, 2, 22);
        when(dailyNoteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doAnswer(inv -> {
            DailyNote n = inv.getArgument(0);
            n.setId(2L);
            n.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(dailyNoteMapper).insert(any(DailyNote.class));

        DailyNoteDTO result = dailyNoteService.getDailyNote(date);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void updateDailyNote_shouldModifyContent() {
        LocalDate date = LocalDate.of(2026, 2, 21);
        DailyNote note = buildDailyNote(1L, date, "Old");
        when(dailyNoteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(note);
        when(dailyNoteMapper.updateById(any(DailyNote.class))).thenReturn(1);

        DailyNoteDTO update = new DailyNoteDTO();
        update.setContent("Updated content");

        DailyNoteDTO result = dailyNoteService.updateDailyNote(date, update);

        assertThat(result.getContent()).isEqualTo("Updated content");
        verify(dailyNoteMapper).updateById(any(DailyNote.class));
    }

    @Test
    void updateDailyNote_notFound_shouldThrow() {
        LocalDate date = LocalDate.of(2026, 12, 31);
        when(dailyNoteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> dailyNoteService.updateDailyNote(date, new DailyNoteDTO()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Daily note not found");
    }

    private DailyNote buildDailyNote(Long id, LocalDate date, String content) {
        DailyNote note = DailyNote.builder().noteDate(date).content(content).build();
        note.setId(id);
        note.setTenantId(1L);
        note.setTenantType(TenantType.PERSONAL);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return note;
    }
}
