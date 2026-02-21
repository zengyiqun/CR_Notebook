package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.WhiteboardDTO;
import com.cr.notebook.entity.Whiteboard;
import com.cr.notebook.mapper.WhiteboardMapper;
import com.cr.notebook.tenant.TenantContext;
import com.cr.notebook.tenant.TenantType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhiteboardServiceTest {

    @Mock
    private WhiteboardMapper whiteboardMapper;

    @InjectMocks
    private WhiteboardService whiteboardService;

    @BeforeEach
    void setUp() {
        TenantContext.set(1L, TenantType.PERSONAL);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listWhiteboards_shouldReturnAll() {
        Whiteboard wb = buildWhiteboard(1L, "Design Board");
        when(whiteboardMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(wb));

        List<WhiteboardDTO> result = whiteboardService.listWhiteboards();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Design Board");
    }

    @Test
    void createWhiteboard_shouldInsert() {
        when(whiteboardMapper.insert(any(Whiteboard.class))).thenAnswer(inv -> {
            Whiteboard w = inv.getArgument(0);
            w.setId(1L);
            w.setCreatedAt(LocalDateTime.now());
            return 1;
        });

        WhiteboardDTO input = new WhiteboardDTO();
        input.setTitle("My Board");

        WhiteboardDTO result = whiteboardService.createWhiteboard(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("My Board");
    }

    @Test
    void createWhiteboard_nullTitle_shouldUseDefault() {
        when(whiteboardMapper.insert(any(Whiteboard.class))).thenAnswer(inv -> {
            Whiteboard w = inv.getArgument(0);
            w.setId(2L);
            return 1;
        });

        WhiteboardDTO input = new WhiteboardDTO();

        WhiteboardDTO result = whiteboardService.createWhiteboard(input);

        assertThat(result.getTitle()).isEqualTo("新白板");
    }

    @Test
    void updateWhiteboard_shouldModify() {
        Whiteboard existing = buildWhiteboard(1L, "Old Board");
        when(whiteboardMapper.selectById(1L)).thenReturn(existing);
        when(whiteboardMapper.updateById(any(Whiteboard.class))).thenReturn(1);

        WhiteboardDTO update = new WhiteboardDTO();
        update.setTitle("Updated Board");
        update.setData("{\"elements\":[]}");

        WhiteboardDTO result = whiteboardService.updateWhiteboard(1L, update);

        assertThat(result.getTitle()).isEqualTo("Updated Board");
        assertThat(result.getData()).isEqualTo("{\"elements\":[]}");
    }

    @Test
    void deleteWhiteboard_shouldCallMapper() {
        Whiteboard existing = buildWhiteboard(1L, "To Delete");
        when(whiteboardMapper.selectById(1L)).thenReturn(existing);
        when(whiteboardMapper.deleteById(1L)).thenReturn(1);

        whiteboardService.deleteWhiteboard(1L);

        verify(whiteboardMapper).deleteById(1L);
    }

    private Whiteboard buildWhiteboard(Long id, String title) {
        Whiteboard wb = Whiteboard.builder().title(title).build();
        wb.setId(id);
        wb.setTenantId(1L);
        wb.setTenantType(TenantType.PERSONAL);
        wb.setCreatedAt(LocalDateTime.now());
        wb.setUpdatedAt(LocalDateTime.now());
        return wb;
    }
}
