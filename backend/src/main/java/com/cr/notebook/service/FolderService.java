package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.FolderDTO;
import com.cr.notebook.entity.Folder;
import com.cr.notebook.mapper.FolderMapper;
import com.cr.notebook.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderMapper folderMapper;

    public List<FolderDTO> listFolders() {
        List<Folder> folders = folderMapper.selectList(
                new LambdaQueryWrapper<Folder>()
                        .eq(Folder::getTenantId, TenantContext.getTenantId())
                        .eq(Folder::getTenantType, TenantContext.getTenantType())
                        // 先按手动排序，再按创建时间兜底，保证列表稳定。
                        .orderByAsc(Folder::getSortOrder)
                        .orderByAsc(Folder::getCreatedAt));
        return folders.stream().map(this::toDTO).toList();
    }

    @Transactional
    public FolderDTO createFolder(FolderDTO dto) {
        Folder folder = Folder.builder()
                .name(dto.getName())
                .icon(dto.getIcon() != null ? dto.getIcon() : "📁")
                .parentId(dto.getParentId())
                .build();
        folder.setTenantId(TenantContext.getTenantId());
        folder.setTenantType(TenantContext.getTenantType());
        folderMapper.insert(folder);
        return toDTO(folder);
    }

    @Transactional
    public FolderDTO updateFolder(Long id, FolderDTO dto) {
        Folder folder = folderMapper.selectById(id);
        if (folder == null) throw new IllegalArgumentException("Folder not found");
        checkTenant(folder);
        if (dto.getName() != null) folder.setName(dto.getName());
        if (dto.getIcon() != null) folder.setIcon(dto.getIcon());
        folderMapper.updateById(folder);
        return toDTO(folder);
    }

    @Transactional
    public void deleteFolder(Long id) {
        Folder folder = folderMapper.selectById(id);
        if (folder == null) throw new IllegalArgumentException("Folder not found");
        checkTenant(folder);
        deleteRecursive(id);
    }

    private void deleteRecursive(Long parentId) {
        // 深度优先删除子节点，避免父节点先删导致孤儿数据。
        List<Folder> children = folderMapper.selectList(
                new LambdaQueryWrapper<Folder>().eq(Folder::getParentId, parentId));
        for (Folder child : children) {
            deleteRecursive(child.getId());
        }
        folderMapper.deleteById(parentId);
    }

    @Transactional
    public void reorderFolders(List<Map<String, Object>> items) {
        // 前端已给出最终顺序：这里按 id 批量落库 sortOrder，不调整父子关系。
        for (Map<String, Object> item : items) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
            Folder folder = folderMapper.selectById(id);
            if (folder == null) continue;
            checkTenant(folder);
            folder.setSortOrder(sortOrder);
            folderMapper.updateById(folder);
        }
    }

    private void checkTenant(Folder folder) {
        if (!folder.getTenantId().equals(TenantContext.getTenantId())
                || folder.getTenantType() != TenantContext.getTenantType()) {
            throw new SecurityException("Access denied");
        }
    }

    private FolderDTO toDTO(Folder f) {
        FolderDTO dto = new FolderDTO();
        dto.setId(f.getId());
        dto.setName(f.getName());
        dto.setIcon(f.getIcon());
        dto.setParentId(f.getParentId());
        dto.setSortOrder(f.getSortOrder());
        dto.setCreatedAt(f.getCreatedAt());
        dto.setUpdatedAt(f.getUpdatedAt());
        return dto;
    }
}
