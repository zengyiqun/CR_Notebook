package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.FolderDTO;
import com.cr.notebook.entity.Folder;
import com.cr.notebook.mapper.FolderMapper;
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
class FolderServiceTest {

    @Mock
    private FolderMapper folderMapper;

    @InjectMocks
    private FolderService folderService;

    @BeforeEach
    void setUp() {
        TenantContext.set(1L, TenantType.PERSONAL);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listFolders_shouldReturnRootFolders() {
        Folder folder = buildFolder(1L, "Work");
        when(folderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(folder));

        List<FolderDTO> result = folderService.listFolders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Work");
    }

    @Test
    void createFolder_shouldInsertAndReturn() {
        when(folderMapper.insert(any(Folder.class))).thenAnswer(inv -> {
            Folder f = inv.getArgument(0);
            f.setId(1L);
            f.setCreatedAt(LocalDateTime.now());
            f.setUpdatedAt(LocalDateTime.now());
            return 1;
        });

        FolderDTO input = new FolderDTO();
        input.setName("New Folder");

        FolderDTO result = folderService.createFolder(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("New Folder");
        assertThat(result.getIcon()).isEqualTo("📁");
        verify(folderMapper).insert(any(Folder.class));
    }

    @Test
    void updateFolder_shouldModifyName() {
        Folder existing = buildFolder(1L, "Old");
        when(folderMapper.selectById(1L)).thenReturn(existing);
        when(folderMapper.updateById(any(Folder.class))).thenReturn(1);

        FolderDTO update = new FolderDTO();
        update.setName("Updated");

        FolderDTO result = folderService.updateFolder(1L, update);

        assertThat(result.getName()).isEqualTo("Updated");
        verify(folderMapper).updateById(any(Folder.class));
    }

    @Test
    void updateFolder_notFound_shouldThrow() {
        when(folderMapper.selectById(999L)).thenReturn(null);

        FolderDTO update = new FolderDTO();
        update.setName("X");

        assertThatThrownBy(() -> folderService.updateFolder(999L, update))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Folder not found");
    }

    @Test
    void updateFolder_wrongTenant_shouldThrow() {
        Folder existing = buildFolder(1L, "Folder");
        existing.setTenantId(999L);
        when(folderMapper.selectById(1L)).thenReturn(existing);

        FolderDTO update = new FolderDTO();
        update.setName("X");

        assertThatThrownBy(() -> folderService.updateFolder(1L, update))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void deleteFolder_shouldCallMapper() {
        Folder existing = buildFolder(1L, "To Delete");
        when(folderMapper.selectById(1L)).thenReturn(existing);
        when(folderMapper.deleteById(1L)).thenReturn(1);

        folderService.deleteFolder(1L);

        verify(folderMapper).deleteById(1L);
    }

    private Folder buildFolder(Long id, String name) {
        Folder folder = Folder.builder().name(name).icon("📁").build();
        folder.setId(id);
        folder.setTenantId(1L);
        folder.setTenantType(TenantType.PERSONAL);
        folder.setCreatedAt(LocalDateTime.now());
        folder.setUpdatedAt(LocalDateTime.now());
        return folder;
    }
}
