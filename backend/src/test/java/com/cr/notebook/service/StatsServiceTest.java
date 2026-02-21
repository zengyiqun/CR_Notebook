package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.StatsDTO;
import com.cr.notebook.entity.*;
import com.cr.notebook.mapper.*;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock private NoteMapper noteMapper;
    @Mock private FolderMapper folderMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private DailyNoteMapper dailyNoteMapper;
    @Mock private CalendarEventMapper calendarEventMapper;
    @Mock private WhiteboardMapper whiteboardMapper;
    @Mock private OrganizationMapper organizationMapper;
    @Mock private OrgMemberMapper orgMemberMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private StatsService statsService;

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
    void getPersonalStats_shouldReturnAllCounts() {
        when(noteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
        when(folderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
        when(taskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L, 4L);
        when(dailyNoteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(7L);
        when(calendarEventMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(whiteboardMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        StatsDTO stats = statsService.getPersonalStats(7);

        assertThat(stats.getNoteCount()).isEqualTo(5);
        assertThat(stats.getFolderCount()).isEqualTo(3);
        assertThat(stats.getTaskCount()).isEqualTo(10);
        assertThat(stats.getTaskCompletedCount()).isEqualTo(4);
        assertThat(stats.getDailyNoteCount()).isEqualTo(7);
        assertThat(stats.getCalendarEventCount()).isEqualTo(2);
        assertThat(stats.getWhiteboardCount()).isEqualTo(1);
        assertThat(stats.getOrgId()).isNull();
    }

    @Test
    void getOrgStats_asMember_shouldReturnStatsWithMembers() {
        Organization org = Organization.builder().name("Team").ownerId(1L).build();
        org.setId(10L);
        when(organizationMapper.selectById(10L)).thenReturn(org);

        OrgMember membership = OrgMember.builder().organizationId(10L).userId(1L).role("OWNER").build();
        membership.setId(1L);
        when(orgMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(membership);

        when(noteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
        when(folderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(taskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L, 2L);
        when(dailyNoteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(calendarEventMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(whiteboardMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        OrgMember m1 = OrgMember.builder().organizationId(10L).userId(1L).role("OWNER").build();
        m1.setId(1L);
        OrgMember m2 = OrgMember.builder().organizationId(10L).userId(2L).role("MEMBER").build();
        m2.setId(2L);
        when(orgMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(m1, m2));

        User u1 = User.builder().username("user1").displayName("User One").build();
        u1.setId(1L);
        User u2 = User.builder().username("user2").displayName("User Two").build();
        u2.setId(2L);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(u1, u2));

        StatsDTO stats = statsService.getOrgStats(10L, 7);

        assertThat(stats.getOrgId()).isEqualTo(10L);
        assertThat(stats.getOrgName()).isEqualTo("Team");
        assertThat(stats.getNoteCount()).isEqualTo(3);
        assertThat(stats.getMemberCount()).isEqualTo(2);
        assertThat(stats.getMemberStats()).hasSize(2);
    }

    @Test
    void getOrgStats_orgNotFound_shouldThrow() {
        when(organizationMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> statsService.getOrgStats(999L, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("组织不存在");
    }

    @Test
    void getOrgStats_notMember_shouldThrow() {
        Organization org = Organization.builder().name("Team").ownerId(2L).build();
        org.setId(10L);
        when(organizationMapper.selectById(10L)).thenReturn(org);
        when(orgMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> statsService.getOrgStats(10L, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("您不是该组织的成员");
    }
}
