package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.NoteDTO;
import com.cr.notebook.entity.Note;
import com.cr.notebook.mapper.NoteMapper;
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
class NoteServiceTest {

    @Mock
    private NoteMapper noteMapper;

    @InjectMocks
    private NoteService noteService;

    @BeforeEach
    void setUp() {
        TenantContext.set(1L, TenantType.PERSONAL);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listNotes_shouldReturnAllNotesForTenant() {
        Note note = buildNote(1L, "Test Note", "content");
        when(noteMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(note));

        List<NoteDTO> result = noteService.listNotes(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Note");
        verify(noteMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void listNotes_withFolderId_shouldFilterByFolder() {
        Note note = buildNote(1L, "Folder Note", "content");
        note.setFolderId(10L);
        when(noteMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(note));

        List<NoteDTO> result = noteService.listNotes(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFolderId()).isEqualTo(10L);
    }

    @Test
    void getNote_shouldReturnNote() {
        Note note = buildNote(1L, "Test", "content");
        when(noteMapper.selectById(1L)).thenReturn(note);

        NoteDTO result = noteService.getNote(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test");
    }

    @Test
    void getNote_notFound_shouldThrow() {
        when(noteMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> noteService.getNote(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Note not found");
    }

    @Test
    void getNote_wrongTenant_shouldThrowSecurityException() {
        Note note = buildNote(1L, "Test", "content");
        note.setTenantId(999L);
        when(noteMapper.selectById(1L)).thenReturn(note);

        assertThatThrownBy(() -> noteService.getNote(1L))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Access denied");
    }

    @Test
    void createNote_shouldInsertAndReturnDTO() {
        when(noteMapper.insert(any(Note.class))).thenAnswer(inv -> {
            Note n = inv.getArgument(0);
            n.setId(1L);
            n.setCreatedAt(LocalDateTime.now());
            n.setUpdatedAt(LocalDateTime.now());
            return 1;
        });

        NoteDTO input = new NoteDTO();
        input.setTitle("New Note");
        input.setContent("Hello");
        input.setExcerpt("Hello...");

        NoteDTO result = noteService.createNote(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("New Note");
        verify(noteMapper).insert(any(Note.class));
    }

    @Test
    void updateNote_shouldModifyAndReturn() {
        Note existing = buildNote(1L, "Old Title", "old content");
        when(noteMapper.selectById(1L)).thenReturn(existing);
        when(noteMapper.updateById(any(Note.class))).thenReturn(1);

        NoteDTO update = new NoteDTO();
        update.setTitle("New Title");

        NoteDTO result = noteService.updateNote(1L, update);

        assertThat(result.getTitle()).isEqualTo("New Title");
        verify(noteMapper).updateById(any(Note.class));
    }

    @Test
    void deleteNote_shouldCallMapper() {
        Note existing = buildNote(1L, "To Delete", "content");
        when(noteMapper.selectById(1L)).thenReturn(existing);
        when(noteMapper.deleteById(1L)).thenReturn(1);

        noteService.deleteNote(1L);

        verify(noteMapper).deleteById(1L);
    }

    @Test
    void searchNotes_shouldReturnMatchingNotes() {
        Note note = buildNote(1L, "Spring Boot Guide", "intro");
        when(noteMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(note));

        List<NoteDTO> result = noteService.searchNotes("Spring");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Spring Boot Guide");
    }

    @Test
    void getBacklinks_shouldReturnNotesLinkingToTarget() {
        Note linking = buildNote(2L, "Linking Note", "See [[1|Test Note]] for details");
        when(noteMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(linking));

        List<NoteDTO> result = noteService.getBacklinks(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void getGraphData_shouldReturnNodesAndEdges() {
        Note noteA = buildNote(1L, "Note A", "Link to [[2|Note B]]");
        Note noteB = buildNote(2L, "Note B", "No links");
        Note noteC = buildNote(3L, "Note C", "References [[1|Note A]] and [[2|Note B]]");
        when(noteMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(noteA, noteB, noteC));

        var result = noteService.getGraphData();

        @SuppressWarnings("unchecked")
        var nodes = (List<java.util.Map<String, Object>>) result.get("nodes");
        @SuppressWarnings("unchecked")
        var edges = (List<java.util.Map<String, Object>>) result.get("edges");

        assertThat(nodes).hasSize(3);
        assertThat(edges).hasSize(3); // A->B, C->A, C->B
    }

    @Test
    void getGraphData_shouldDeduplicateEdges() {
        Note note = buildNote(1L, "Repeat", "Link [[2|B]] and again [[2|B]]");
        Note noteB = buildNote(2L, "B", "content");
        when(noteMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(note, noteB));

        var result = noteService.getGraphData();

        @SuppressWarnings("unchecked")
        var edges = (List<java.util.Map<String, Object>>) result.get("edges");

        assertThat(edges).hasSize(1);
    }

    @Test
    void getGraphData_shouldIgnoreSelfLinks() {
        Note note = buildNote(1L, "Self", "Link to [[1|Self]]");
        when(noteMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(note));

        var result = noteService.getGraphData();

        @SuppressWarnings("unchecked")
        var edges = (List<java.util.Map<String, Object>>) result.get("edges");

        assertThat(edges).isEmpty();
    }

    @Test
    void getGraphData_shouldIgnoreLinksToDeletedNotes() {
        Note note = buildNote(1L, "Orphan", "Link to [[999|Deleted]]");
        when(noteMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(note));

        var result = noteService.getGraphData();

        @SuppressWarnings("unchecked")
        var edges = (List<java.util.Map<String, Object>>) result.get("edges");

        assertThat(edges).isEmpty();
    }

    @Test
    void getGraphData_emptyNotes_shouldReturnEmpty() {
        when(noteMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        var result = noteService.getGraphData();

        @SuppressWarnings("unchecked")
        var nodes = (List<java.util.Map<String, Object>>) result.get("nodes");
        @SuppressWarnings("unchecked")
        var edges = (List<java.util.Map<String, Object>>) result.get("edges");

        assertThat(nodes).isEmpty();
        assertThat(edges).isEmpty();
    }

    private Note buildNote(Long id, String title, String content) {
        Note note = Note.builder()
                .title(title)
                .content(content)
                .excerpt(content)
                .isPinned(false)
                .build();
        note.setId(id);
        note.setTenantId(1L);
        note.setTenantType(TenantType.PERSONAL);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return note;
    }
}
