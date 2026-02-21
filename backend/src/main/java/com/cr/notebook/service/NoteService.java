package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.NoteDTO;
import com.cr.notebook.entity.Note;
import com.cr.notebook.mapper.NoteMapper;
import com.cr.notebook.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 笔记业务服务层。
 * 提供笔记的 CRUD、搜索、反向链接查询和知识图谱数据构建功能。
 * 所有查询均基于多租户隔离（tenant_id + tenant_type）。
 */
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteMapper noteMapper;

    public List<NoteDTO> listNotes(Long folderId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getTenantId, TenantContext.getTenantId())
                .eq(Note::getTenantType, TenantContext.getTenantType())
                .eq(folderId != null, Note::getFolderId, folderId)
                .orderByDesc(Note::getIsPinned)
                .orderByDesc(Note::getUpdatedAt);
        return noteMapper.selectList(wrapper).stream().map(this::toDTO).toList();
    }

    public NoteDTO getNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) throw new IllegalArgumentException("Note not found");
        checkTenant(note);
        return toDTO(note);
    }

    @Transactional
    public NoteDTO createNote(NoteDTO dto) {
        Note note = Note.builder()
                .folderId(dto.getFolderId())
                .title(dto.getTitle() != null ? dto.getTitle() : "")
                .content(dto.getContent())
                .excerpt(dto.getExcerpt() != null ? dto.getExcerpt() : "")
                .isPinned(dto.getIsPinned() != null && dto.getIsPinned())
                .tags(dto.getTags() != null ? dto.getTags() : List.of())
                .build();
        note.setTenantId(TenantContext.getTenantId());
        note.setTenantType(TenantContext.getTenantType());
        noteMapper.insert(note);
        return toDTO(note);
    }

    @Transactional
    public NoteDTO updateNote(Long id, NoteDTO dto) {
        Note note = noteMapper.selectById(id);
        if (note == null) throw new IllegalArgumentException("Note not found");
        checkTenant(note);

        if (dto.getTitle() != null) note.setTitle(dto.getTitle());
        if (dto.getContent() != null) note.setContent(dto.getContent());
        if (dto.getExcerpt() != null) note.setExcerpt(dto.getExcerpt());
        if (dto.getIsPinned() != null) note.setIsPinned(dto.getIsPinned());
        if (dto.getFolderId() != null) note.setFolderId(dto.getFolderId());
        if (dto.getTags() != null) note.setTags(dto.getTags());

        noteMapper.updateById(note);
        return toDTO(note);
    }

    @Transactional
    public void deleteNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) throw new IllegalArgumentException("Note not found");
        checkTenant(note);
        noteMapper.deleteById(id);
    }

    public List<NoteDTO> searchNotes(String query) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getTenantId, TenantContext.getTenantId())
                .eq(Note::getTenantType, TenantContext.getTenantType())
                .and(w -> w.like(Note::getTitle, query).or().like(Note::getExcerpt, query));
        return noteMapper.selectList(wrapper).stream().map(this::toDTO).toList();
    }

    /**
     * 查找所有引用了指定笔记的反向链接。
     * 通过在 content 字段中搜索 [[noteId| 模式来定位双链引用。
     */
    public List<NoteDTO> getBacklinks(Long noteId) {
        String pattern = "[[" + noteId + "|";
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getTenantId, TenantContext.getTenantId())
                .eq(Note::getTenantType, TenantContext.getTenantType())
                .like(Note::getContent, pattern)
                .ne(Note::getId, noteId)
                .orderByDesc(Note::getUpdatedAt);
        return noteMapper.selectList(wrapper).stream().map(this::toDTO).toList();
    }

    /** 匹配双链笔记格式 [[noteId|noteTitle]] */
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[\\[(\\d+)\\|([^\\]]+)\\]\\]");

    /**
     * 构建知识图谱数据：遍历当前租户所有笔记，解析内容中的 [[id|title]] 链接，
     * 返回 {nodes: [...], edges: [...]} 结构供前端 D3.js 力导向图渲染。
     * 自动过滤自引用和指向已删除笔记的边，同一方向的重复边会被去重。
     */
    public Map<String, Object> getGraphData() {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getTenantId, TenantContext.getTenantId())
                .eq(Note::getTenantType, TenantContext.getTenantType());
        List<Note> allNotes = noteMapper.selectList(wrapper);

        Set<Long> noteIds = allNotes.stream().map(Note::getId).collect(Collectors.toSet());

        List<Map<String, Object>> nodes = allNotes.stream().map(n -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", n.getId());
            node.put("title", n.getTitle());
            node.put("folderId", n.getFolderId());
            node.put("tags", n.getTags() != null ? n.getTags() : List.of());
            node.put("updatedAt", n.getUpdatedAt());
            return node;
        }).toList();

        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Note note : allNotes) {
            if (note.getContent() == null) continue;
            Matcher matcher = LINK_PATTERN.matcher(note.getContent());
            while (matcher.find()) {
                Long targetId = Long.parseLong(matcher.group(1));
                if (noteIds.contains(targetId) && !targetId.equals(note.getId())) {
                    String edgeKey = note.getId() + "->" + targetId;
                    if (seen.add(edgeKey)) {
                        Map<String, Object> edge = new LinkedHashMap<>();
                        edge.put("source", note.getId());
                        edge.put("target", targetId);
                        edges.add(edge);
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    private void checkTenant(Note note) {
        if (!note.getTenantId().equals(TenantContext.getTenantId())
                || note.getTenantType() != TenantContext.getTenantType()) {
            throw new SecurityException("Access denied");
        }
    }

    private NoteDTO toDTO(Note n) {
        NoteDTO dto = new NoteDTO();
        dto.setId(n.getId());
        dto.setFolderId(n.getFolderId());
        dto.setTitle(n.getTitle());
        dto.setContent(n.getContent());
        dto.setExcerpt(n.getExcerpt());
        dto.setIsPinned(n.getIsPinned());
        dto.setTags(n.getTags() != null ? n.getTags() : List.of());
        dto.setCreatedAt(n.getCreatedAt());
        dto.setUpdatedAt(n.getUpdatedAt());
        return dto;
    }
}
