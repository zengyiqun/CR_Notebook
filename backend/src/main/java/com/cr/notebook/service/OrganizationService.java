package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.OrgMemberDTO;
import com.cr.notebook.dto.OrganizationDTO;
import com.cr.notebook.entity.OrgMember;
import com.cr.notebook.entity.Organization;
import com.cr.notebook.entity.User;
import com.cr.notebook.mapper.OrgMemberMapper;
import com.cr.notebook.mapper.OrganizationMapper;
import com.cr.notebook.mapper.UserMapper;
import com.cr.notebook.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationMapper organizationMapper;
    private final OrgMemberMapper orgMemberMapper;
    private final UserMapper userMapper;

    public List<OrganizationDTO> listMyOrganizations() {
        Long currentUserId = getCurrentUserId();
        List<OrgMember> memberships = orgMemberMapper.selectList(
                new LambdaQueryWrapper<OrgMember>().eq(OrgMember::getUserId, currentUserId));

        List<Long> orgIds = memberships.stream().map(OrgMember::getOrganizationId).toList();
        if (orgIds.isEmpty()) return Collections.emptyList();

        List<Organization> orgs = organizationMapper.selectBatchIds(orgIds);

        Map<Long, String> roleMap = memberships.stream()
                .collect(Collectors.toMap(OrgMember::getOrganizationId, OrgMember::getRole));

        Set<Long> ownerIds = orgs.stream().map(Organization::getOwnerId).collect(Collectors.toSet());
        Map<Long, String> ownerNameMap = userMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getDisplayName));

        return orgs.stream().map(org -> {
            OrganizationDTO dto = new OrganizationDTO();
            dto.setId(org.getId());
            dto.setName(org.getName());
            dto.setOwnerId(org.getOwnerId());
            dto.setOwnerName(ownerNameMap.getOrDefault(org.getOwnerId(), ""));
            dto.setRole(roleMap.getOrDefault(org.getId(), "MEMBER"));
            dto.setCreatedAt(org.getCreatedAt());
            dto.setMemberCount(orgMemberMapper.selectCount(
                    new LambdaQueryWrapper<OrgMember>().eq(OrgMember::getOrganizationId, org.getId())).intValue());
            dto.setAvatarUrl(org.getAvatarUrl());
            return dto;
        }).toList();
    }

    @Transactional
    public OrganizationDTO createOrganization(String name) {
        Long currentUserId = getCurrentUserId();

        Organization org = Organization.builder()
                .name(name)
                .ownerId(currentUserId)
                .build();
        organizationMapper.insert(org);

        OrgMember ownerMember = OrgMember.builder()
                .organizationId(org.getId())
                .userId(currentUserId)
                .role("OWNER")
                .build();
        orgMemberMapper.insert(ownerMember);

        User owner = userMapper.selectById(currentUserId);

        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(org.getId());
        dto.setName(org.getName());
        dto.setOwnerId(org.getOwnerId());
        dto.setOwnerName(owner.getDisplayName());
        dto.setRole("OWNER");
        dto.setCreatedAt(org.getCreatedAt());
        dto.setMemberCount(1);
        dto.setAvatarUrl(org.getAvatarUrl());
        return dto;
    }

    public OrganizationDTO getOrganization(Long id) {
        Long currentUserId = getCurrentUserId();

        Organization org = organizationMapper.selectById(id);
        if (org == null) throw new IllegalArgumentException("组织不存在");

        OrgMember membership = orgMemberMapper.selectOne(
                new LambdaQueryWrapper<OrgMember>()
                        .eq(OrgMember::getOrganizationId, id)
                        .eq(OrgMember::getUserId, currentUserId));
        if (membership == null) throw new IllegalArgumentException("您不是该组织的成员");

        User owner = userMapper.selectById(org.getOwnerId());

        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(org.getId());
        dto.setName(org.getName());
        dto.setOwnerId(org.getOwnerId());
        dto.setOwnerName(owner.getDisplayName());
        dto.setRole(membership.getRole());
        dto.setCreatedAt(org.getCreatedAt());
        dto.setMemberCount(orgMemberMapper.selectCount(
                new LambdaQueryWrapper<OrgMember>().eq(OrgMember::getOrganizationId, id)).intValue());
        dto.setAvatarUrl(org.getAvatarUrl());
        return dto;
    }

    @Transactional
    public OrganizationDTO updateOrganization(Long id, String name) {
        Long currentUserId = getCurrentUserId();

        Organization org = organizationMapper.selectById(id);
        if (org == null) throw new IllegalArgumentException("组织不存在");
        if (!org.getOwnerId().equals(currentUserId)) {
            throw new IllegalArgumentException("只有组织所有者才能修改组织信息");
        }

        org.setName(name);
        organizationMapper.updateById(org);

        User owner = userMapper.selectById(org.getOwnerId());

        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(org.getId());
        dto.setName(org.getName());
        dto.setOwnerId(org.getOwnerId());
        dto.setOwnerName(owner.getDisplayName());
        dto.setRole("OWNER");
        dto.setCreatedAt(org.getCreatedAt());
        dto.setMemberCount(orgMemberMapper.selectCount(
                new LambdaQueryWrapper<OrgMember>().eq(OrgMember::getOrganizationId, id)).intValue());
        dto.setAvatarUrl(org.getAvatarUrl());
        return dto;
    }

    @Transactional
    public void deleteOrganization(Long id) {
        Long currentUserId = getCurrentUserId();

        Organization org = organizationMapper.selectById(id);
        if (org == null) throw new IllegalArgumentException("组织不存在");
        if (!org.getOwnerId().equals(currentUserId)) {
            throw new IllegalArgumentException("只有组织所有者才能删除组织");
        }

        orgMemberMapper.delete(
                new LambdaQueryWrapper<OrgMember>().eq(OrgMember::getOrganizationId, id));
        organizationMapper.deleteById(id);
    }

    public List<OrgMemberDTO> listMembers(Long orgId) {
        Long currentUserId = getCurrentUserId();

        OrgMember self = orgMemberMapper.selectOne(
                new LambdaQueryWrapper<OrgMember>()
                        .eq(OrgMember::getOrganizationId, orgId)
                        .eq(OrgMember::getUserId, currentUserId));
        if (self == null) throw new IllegalArgumentException("您不是该组织的成员");

        List<OrgMember> members = orgMemberMapper.selectList(
                new LambdaQueryWrapper<OrgMember>().eq(OrgMember::getOrganizationId, orgId));

        Set<Long> userIds = members.stream().map(OrgMember::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return members.stream().map(m -> {
            OrgMemberDTO dto = new OrgMemberDTO();
            dto.setId(m.getId());
            dto.setUserId(m.getUserId());
            User user = userMap.get(m.getUserId());
            dto.setUsername(user != null ? user.getUsername() : "");
            dto.setDisplayName(user != null ? user.getDisplayName() : "");
            dto.setRole(m.getRole());
            dto.setJoinedAt(m.getCreatedAt());
            return dto;
        }).toList();
    }

    @Transactional
    public OrgMemberDTO addMember(Long orgId, Long userId, String role) {
        Long currentUserId = getCurrentUserId();

        Organization org = organizationMapper.selectById(orgId);
        if (org == null) throw new IllegalArgumentException("组织不存在");

        OrgMember currentMember = orgMemberMapper.selectOne(
                new LambdaQueryWrapper<OrgMember>()
                        .eq(OrgMember::getOrganizationId, orgId)
                        .eq(OrgMember::getUserId, currentUserId));
        if (currentMember == null) throw new IllegalArgumentException("您不是该组织的成员");
        if (!"OWNER".equals(currentMember.getRole()) && !"ADMIN".equals(currentMember.getRole())) {
            throw new IllegalArgumentException("只有组织所有者或管理员才能添加成员");
        }

        Long existCount = orgMemberMapper.selectCount(
                new LambdaQueryWrapper<OrgMember>()
                        .eq(OrgMember::getOrganizationId, orgId)
                        .eq(OrgMember::getUserId, userId));
        if (existCount > 0) throw new IllegalArgumentException("该用户已是组织成员");

        User user = userMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        OrgMember member = OrgMember.builder()
                .organizationId(orgId)
                .userId(userId)
                .role(role != null ? role : "MEMBER")
                .build();
        orgMemberMapper.insert(member);

        OrgMemberDTO dto = new OrgMemberDTO();
        dto.setId(member.getId());
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getDisplayName());
        dto.setRole(member.getRole());
        dto.setJoinedAt(member.getCreatedAt());
        return dto;
    }

    @Transactional
    public OrgMemberDTO updateMemberRole(Long orgId, Long userId, String newRole) {
        Long currentUserId = getCurrentUserId();

        Organization org = organizationMapper.selectById(orgId);
        if (org == null) throw new IllegalArgumentException("组织不存在");
        if (!org.getOwnerId().equals(currentUserId)) {
            throw new IllegalArgumentException("只有组织所有者才能修改成员角色");
        }
        if (org.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("不能修改组织所有者的角色");
        }

        OrgMember targetMember = orgMemberMapper.selectOne(
                new LambdaQueryWrapper<OrgMember>()
                        .eq(OrgMember::getOrganizationId, orgId)
                        .eq(OrgMember::getUserId, userId));
        if (targetMember == null) throw new IllegalArgumentException("该用户不是组织成员");

        targetMember.setRole(newRole);
        orgMemberMapper.updateById(targetMember);

        User user = userMapper.selectById(userId);
        OrgMemberDTO dto = new OrgMemberDTO();
        dto.setId(targetMember.getId());
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getDisplayName());
        dto.setRole(targetMember.getRole());
        dto.setJoinedAt(targetMember.getCreatedAt());
        return dto;
    }

    @Transactional
    public void removeMember(Long orgId, Long userId) {
        Long currentUserId = getCurrentUserId();

        Organization org = organizationMapper.selectById(orgId);
        if (org == null) throw new IllegalArgumentException("组织不存在");
        if (org.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("不能移除组织所有者");
        }

        OrgMember currentMember = orgMemberMapper.selectOne(
                new LambdaQueryWrapper<OrgMember>()
                        .eq(OrgMember::getOrganizationId, orgId)
                        .eq(OrgMember::getUserId, currentUserId));
        if (currentMember == null) throw new IllegalArgumentException("您不是该组织的成员");
        if (!"OWNER".equals(currentMember.getRole()) && !"ADMIN".equals(currentMember.getRole())) {
            throw new IllegalArgumentException("只有组织所有者或管理员才能移除成员");
        }

        OrgMember targetMember = orgMemberMapper.selectOne(
                new LambdaQueryWrapper<OrgMember>()
                        .eq(OrgMember::getOrganizationId, orgId)
                        .eq(OrgMember::getUserId, userId));
        if (targetMember == null) throw new IllegalArgumentException("该用户不是组织成员");

        orgMemberMapper.deleteById(targetMember.getId());
    }

    @Transactional
    public void updateOrgAvatar(Long orgId, String avatarUrl) {
        Long currentUserId = getCurrentUserId();
        Organization org = organizationMapper.selectById(orgId);
        if (org == null) throw new IllegalArgumentException("组织不存在");

        OrgMember membership = orgMemberMapper.selectOne(
                new LambdaQueryWrapper<OrgMember>()
                        .eq(OrgMember::getOrganizationId, orgId)
                        .eq(OrgMember::getUserId, currentUserId));
        if (membership == null || (!"OWNER".equals(membership.getRole()) && !"ADMIN".equals(membership.getRole()))) {
            throw new IllegalArgumentException("只有组织所有者或管理员才能修改组织头像");
        }

        org.setAvatarUrl(avatarUrl);
        organizationMapper.updateById(org);
    }

    private Long getCurrentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getId();
    }
}
