package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.WhiteboardDTO;
import com.cr.notebook.entity.Whiteboard;
import com.cr.notebook.mapper.WhiteboardMapper;
import com.cr.notebook.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WhiteboardService {

    private final WhiteboardMapper whiteboardMapper;

    public List<WhiteboardDTO> listWhiteboards() {
        List<Whiteboard> whiteboards = whiteboardMapper.selectList(
                new LambdaQueryWrapper<Whiteboard>()
                        .eq(Whiteboard::getTenantId, TenantContext.getTenantId())
                        .eq(Whiteboard::getTenantType, TenantContext.getTenantType())
                        .orderByDesc(Whiteboard::getUpdatedAt));
        return whiteboards.stream().map(this::toDTO).toList();
    }

    @Transactional
    public WhiteboardDTO createWhiteboard(WhiteboardDTO dto) {
        Whiteboard whiteboard = Whiteboard.builder()
                .title(dto.getTitle() != null ? dto.getTitle() : "新白板")
                .data(dto.getData())
                .build();
        whiteboard.setTenantId(TenantContext.getTenantId());
        whiteboard.setTenantType(TenantContext.getTenantType());
        whiteboardMapper.insert(whiteboard);
        return toDTO(whiteboard);
    }

    @Transactional
    public WhiteboardDTO updateWhiteboard(Long id, WhiteboardDTO dto) {
        Whiteboard whiteboard = whiteboardMapper.selectById(id);
        if (whiteboard == null) throw new IllegalArgumentException("Whiteboard not found");
        checkTenant(whiteboard);

        if (dto.getTitle() != null) whiteboard.setTitle(dto.getTitle());
        if (dto.getData() != null) whiteboard.setData(dto.getData());

        whiteboardMapper.updateById(whiteboard);
        return toDTO(whiteboard);
    }

    @Transactional
    public void deleteWhiteboard(Long id) {
        Whiteboard whiteboard = whiteboardMapper.selectById(id);
        if (whiteboard == null) throw new IllegalArgumentException("Whiteboard not found");
        checkTenant(whiteboard);
        whiteboardMapper.deleteById(id);
    }

    private void checkTenant(Whiteboard whiteboard) {
        if (!whiteboard.getTenantId().equals(TenantContext.getTenantId())
                || whiteboard.getTenantType() != TenantContext.getTenantType()) {
            throw new SecurityException("Access denied");
        }
    }

    private WhiteboardDTO toDTO(Whiteboard w) {
        WhiteboardDTO dto = new WhiteboardDTO();
        dto.setId(w.getId());
        dto.setTitle(w.getTitle());
        dto.setData(w.getData());
        dto.setCreatedAt(w.getCreatedAt());
        dto.setUpdatedAt(w.getUpdatedAt());
        return dto;
    }
}
