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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock private OrganizationMapper organizationMapper;
    @Mock private OrgMemberMapper orgMemberMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        UserPrincipal principal = new UserPrincipal(1L, "testuser", "test@example.com", "hashed");
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listMyOrganizations_noMemberships_shouldReturnEmpty() {
        when(orgMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<OrganizationDTO> result = organizationService.listMyOrganizations();

        assertThat(result).isEmpty();
    }

    @Test
    void listMyOrganizations_withMemberships_shouldReturnOrgs() {
        OrgMember membership = buildOrgMember(1L, 10L, 1L, "OWNER");
        when(orgMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(membership));

        Organization org = buildOrg(10L, "My Team", 1L);
        when(organizationMapper.selectBatchIds(anyCollection())).thenReturn(List.of(org));

        User owner = buildUser(1L, "testuser", "Test User");
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(owner));

        when(orgMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        List<OrganizationDTO> result = organizationService.listMyOrganizations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("My Team");
        assertThat(result.get(0).getRole()).isEqualTo("OWNER");
        assertThat(result.get(0).getMemberCount()).isEqualTo(2);
    }

    @Test
    void createOrganization_shouldInsertOrgAndMember() {
        doAnswer(inv -> {
            Organization o = inv.getArgument(0);
            o.setId(10L);
            o.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(organizationMapper).insert(any(Organization.class));

        doAnswer(inv -> {
            OrgMember m = inv.getArgument(0);
            m.setId(1L);
            return 1;
        }).when(orgMemberMapper).insert(any(OrgMember.class));

        User owner = buildUser(1L, "testuser", "Test User");
        when(userMapper.selectById(1L)).thenReturn(owner);

        OrganizationDTO result = organizationService.createOrganization("New Org");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("New Org");
        assertThat(result.getRole()).isEqualTo("OWNER");
        assertThat(result.getMemberCount()).isEqualTo(1);
        verify(organizationMapper).insert(any(Organization.class));
        verify(orgMemberMapper).insert(any(OrgMember.class));
    }

    @Test
    void getOrganization_asMember_shouldReturn() {
        Organization org = buildOrg(10L, "Team", 1L);
        when(organizationMapper.selectById(10L)).thenReturn(org);

        OrgMember membership = buildOrgMember(1L, 10L, 1L, "OWNER");
        when(orgMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(membership);

        User owner = buildUser(1L, "testuser", "Test User");
        when(userMapper.selectById(1L)).thenReturn(owner);

        when(orgMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        OrganizationDTO result = organizationService.getOrganization(10L);

        assertThat(result.getName()).isEqualTo("Team");
        assertThat(result.getMemberCount()).isEqualTo(3);
    }

    @Test
    void getOrganization_notFound_shouldThrow() {
        when(organizationMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> organizationService.getOrganization(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("组织不存在");
    }

    @Test
    void getOrganization_notMember_shouldThrow() {
        Organization org = buildOrg(10L, "Team", 2L);
        when(organizationMapper.selectById(10L)).thenReturn(org);
        when(orgMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> organizationService.getOrganization(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("您不是该组织的成员");
    }

    @Test
    void updateOrganization_asOwner_shouldUpdate() {
        Organization org = buildOrg(10L, "Old Name", 1L);
        when(organizationMapper.selectById(10L)).thenReturn(org);
        when(organizationMapper.updateById(any(Organization.class))).thenReturn(1);

        User owner = buildUser(1L, "testuser", "Test User");
        when(userMapper.selectById(1L)).thenReturn(owner);
        when(orgMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        OrganizationDTO result = organizationService.updateOrganization(10L, "New Name");

        assertThat(result.getName()).isEqualTo("New Name");
        verify(organizationMapper).updateById(any(Organization.class));
    }

    @Test
    void updateOrganization_notOwner_shouldThrow() {
        Organization org = buildOrg(10L, "Team", 999L);
        when(organizationMapper.selectById(10L)).thenReturn(org);

        assertThatThrownBy(() -> organizationService.updateOrganization(10L, "New"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("只有组织所有者才能修改组织信息");
    }

    @Test
    void deleteOrganization_asOwner_shouldDelete() {
        Organization org = buildOrg(10L, "Team", 1L);
        when(organizationMapper.selectById(10L)).thenReturn(org);
        when(orgMemberMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);
        when(organizationMapper.deleteById(10L)).thenReturn(1);

        organizationService.deleteOrganization(10L);

        verify(orgMemberMapper).delete(any(LambdaQueryWrapper.class));
        verify(organizationMapper).deleteById(10L);
    }

    @Test
    void deleteOrganization_notOwner_shouldThrow() {
        Organization org = buildOrg(10L, "Team", 999L);
        when(organizationMapper.selectById(10L)).thenReturn(org);

        assertThatThrownBy(() -> organizationService.deleteOrganization(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("只有组织所有者才能删除组织");
    }

    @Test
    void listMembers_asMember_shouldReturnList() {
        OrgMember self = buildOrgMember(1L, 10L, 1L, "OWNER");
        OrgMember other = buildOrgMember(2L, 10L, 2L, "MEMBER");

        when(orgMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(self);
        when(orgMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(self, other));

        User user1 = buildUser(1L, "user1", "User One");
        User user2 = buildUser(2L, "user2", "User Two");
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(user1, user2));

        List<OrgMemberDTO> result = organizationService.listMembers(10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo("OWNER");
        assertThat(result.get(1).getRole()).isEqualTo("MEMBER");
    }

    @Test
    void listMembers_notMember_shouldThrow() {
        when(orgMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> organizationService.listMembers(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("您不是该组织的成员");
    }

    @Test
    void addMember_asOwner_shouldAdd() {
        Organization org = buildOrg(10L, "Team", 1L);
        when(organizationMapper.selectById(10L)).thenReturn(org);

        OrgMember currentMember = buildOrgMember(1L, 10L, 1L, "OWNER");
        when(orgMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(currentMember);
        when(orgMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        User newUser = buildUser(3L, "newuser", "New User");
        when(userMapper.selectById(3L)).thenReturn(newUser);

        doAnswer(inv -> {
            OrgMember m = inv.getArgument(0);
            m.setId(5L);
            m.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(orgMemberMapper).insert(any(OrgMember.class));

        OrgMemberDTO result = organizationService.addMember(10L, 3L, "MEMBER");

        assertThat(result.getUserId()).isEqualTo(3L);
        assertThat(result.getRole()).isEqualTo("MEMBER");
        assertThat(result.getUsername()).isEqualTo("newuser");
    }

    @Test
    void addMember_notOwnerOrAdmin_shouldThrow() {
        Organization org = buildOrg(10L, "Team", 2L);
        when(organizationMapper.selectById(10L)).thenReturn(org);

        OrgMember currentMember = buildOrgMember(1L, 10L, 1L, "MEMBER");
        when(orgMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(currentMember);

        assertThatThrownBy(() -> organizationService.addMember(10L, 3L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("只有组织所有者或管理员才能添加成员");
    }

    @Test
    void addMember_alreadyMember_shouldThrow() {
        Organization org = buildOrg(10L, "Team", 1L);
        when(organizationMapper.selectById(10L)).thenReturn(org);

        OrgMember currentMember = buildOrgMember(1L, 10L, 1L, "OWNER");
        when(orgMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(currentMember);
        when(orgMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> organizationService.addMember(10L, 3L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("该用户已是组织成员");
    }

    @Test
    void removeMember_asOwner_shouldRemove() {
        Organization org = buildOrg(10L, "Team", 1L);
        when(organizationMapper.selectById(10L)).thenReturn(org);

        OrgMember currentMember = buildOrgMember(1L, 10L, 1L, "OWNER");
        OrgMember targetMember = buildOrgMember(2L, 10L, 3L, "MEMBER");
        when(orgMemberMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(currentMember)
                .thenReturn(targetMember);
        when(orgMemberMapper.deleteById(2L)).thenReturn(1);

        organizationService.removeMember(10L, 3L);

        verify(orgMemberMapper).deleteById(2L);
    }

    @Test
    void removeMember_removeOwner_shouldThrow() {
        Organization org = buildOrg(10L, "Team", 1L);
        when(organizationMapper.selectById(10L)).thenReturn(org);

        assertThatThrownBy(() -> organizationService.removeMember(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("不能移除组织所有者");
    }

    @Test
    void updateMemberRole_asOwner_shouldUpdate() {
        Organization org = buildOrg(10L, "Team", 1L);
        when(organizationMapper.selectById(10L)).thenReturn(org);

        OrgMember target = buildOrgMember(2L, 10L, 3L, "MEMBER");
        when(orgMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(target);
        when(orgMemberMapper.updateById(any(OrgMember.class))).thenReturn(1);

        User user = buildUser(3L, "user3", "User Three");
        when(userMapper.selectById(3L)).thenReturn(user);

        OrgMemberDTO result = organizationService.updateMemberRole(10L, 3L, "ADMIN");

        assertThat(result.getRole()).isEqualTo("ADMIN");
        assertThat(result.getUserId()).isEqualTo(3L);
        verify(orgMemberMapper).updateById(any(OrgMember.class));
    }

    @Test
    void updateMemberRole_notOwner_shouldThrow() {
        Organization org = buildOrg(10L, "Team", 999L);
        when(organizationMapper.selectById(10L)).thenReturn(org);

        assertThatThrownBy(() -> organizationService.updateMemberRole(10L, 3L, "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("只有组织所有者才能修改成员角色");
    }

    @Test
    void updateMemberRole_targetIsOwner_shouldThrow() {
        Organization org = buildOrg(10L, "Team", 1L);
        when(organizationMapper.selectById(10L)).thenReturn(org);

        assertThatThrownBy(() -> organizationService.updateMemberRole(10L, 1L, "MEMBER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("不能修改组织所有者的角色");
    }

    @Test
    void updateMemberRole_targetNotMember_shouldThrow() {
        Organization org = buildOrg(10L, "Team", 1L);
        when(organizationMapper.selectById(10L)).thenReturn(org);
        when(orgMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> organizationService.updateMemberRole(10L, 5L, "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("该用户不是组织成员");
    }

    private Organization buildOrg(Long id, String name, Long ownerId) {
        Organization org = Organization.builder().name(name).ownerId(ownerId).build();
        org.setId(id);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        return org;
    }

    private OrgMember buildOrgMember(Long id, Long orgId, Long userId, String role) {
        OrgMember member = OrgMember.builder().organizationId(orgId).userId(userId).role(role).build();
        member.setId(id);
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        return member;
    }

    private User buildUser(Long id, String username, String displayName) {
        User user = User.builder().username(username).email(username + "@example.com")
                .passwordHash("hashed").displayName(displayName).build();
        user.setId(id);
        return user;
    }
}
