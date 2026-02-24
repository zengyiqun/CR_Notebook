package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.StatsDTO;
import com.cr.notebook.entity.*;
import com.cr.notebook.mapper.*;
import com.cr.notebook.security.UserPrincipal;
import com.cr.notebook.tenant.TenantType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final NoteMapper noteMapper;
    private final FolderMapper folderMapper;
    private final TaskMapper taskMapper;
    private final DailyNoteMapper dailyNoteMapper;
    private final CalendarEventMapper calendarEventMapper;
    private final WhiteboardMapper whiteboardMapper;
    private final OrganizationMapper organizationMapper;
    private final OrgMemberMapper orgMemberMapper;
    private final UserMapper userMapper;

    public StatsDTO getPersonalStats(int days) {
        Long userId = getCurrentUserId();
        StatsDTO stats = new StatsDTO();
        stats.setNoteCount(noteMapper.selectCount(
                new LambdaQueryWrapper<Note>().eq(Note::getTenantId, userId).eq(Note::getTenantType, TenantType.PERSONAL)));
        stats.setFolderCount(folderMapper.selectCount(
                new LambdaQueryWrapper<Folder>().eq(Folder::getTenantId, userId).eq(Folder::getTenantType, TenantType.PERSONAL)));
        stats.setTaskCount(taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getTenantId, userId).eq(Task::getTenantType, TenantType.PERSONAL)));
        stats.setTaskCompletedCount(taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getTenantId, userId).eq(Task::getTenantType, TenantType.PERSONAL).eq(Task::getCompleted, true)));
        stats.setDailyNoteCount(dailyNoteMapper.selectCount(
                new LambdaQueryWrapper<DailyNote>().eq(DailyNote::getTenantId, userId).eq(DailyNote::getTenantType, TenantType.PERSONAL)));
        stats.setCalendarEventCount(calendarEventMapper.selectCount(
                new LambdaQueryWrapper<CalendarEvent>().eq(CalendarEvent::getTenantId, userId).eq(CalendarEvent::getTenantType, TenantType.PERSONAL)));
        stats.setWhiteboardCount(whiteboardMapper.selectCount(
                new LambdaQueryWrapper<Whiteboard>().eq(Whiteboard::getTenantId, userId).eq(Whiteboard::getTenantType, TenantType.PERSONAL)));
        stats.setDailyStats(buildDailyStats(userId, TenantType.PERSONAL, days));
        return stats;
    }

    public StatsDTO getOrgStats(Long orgId, int days) {
        Long currentUserId = getCurrentUserId();

        Organization org = organizationMapper.selectById(orgId);
        if (org == null) throw new IllegalArgumentException("组织不存在");

        OrgMember membership = orgMemberMapper.selectOne(
                new LambdaQueryWrapper<OrgMember>()
                        .eq(OrgMember::getOrganizationId, orgId)
                        .eq(OrgMember::getUserId, currentUserId));
        // 仅组织成员可查看组织统计，避免跨组织数据泄露。
        if (membership == null) throw new IllegalArgumentException("您不是该组织的成员");

        StatsDTO stats = new StatsDTO();
        stats.setOrgId(orgId);
        stats.setOrgName(org.getName());

        stats.setNoteCount(noteMapper.selectCount(
                new LambdaQueryWrapper<Note>().eq(Note::getTenantId, orgId).eq(Note::getTenantType, TenantType.ORGANIZATION)));
        stats.setFolderCount(folderMapper.selectCount(
                new LambdaQueryWrapper<Folder>().eq(Folder::getTenantId, orgId).eq(Folder::getTenantType, TenantType.ORGANIZATION)));
        stats.setTaskCount(taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getTenantId, orgId).eq(Task::getTenantType, TenantType.ORGANIZATION)));
        stats.setTaskCompletedCount(taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getTenantId, orgId).eq(Task::getTenantType, TenantType.ORGANIZATION).eq(Task::getCompleted, true)));
        stats.setDailyNoteCount(dailyNoteMapper.selectCount(
                new LambdaQueryWrapper<DailyNote>().eq(DailyNote::getTenantId, orgId).eq(DailyNote::getTenantType, TenantType.ORGANIZATION)));
        stats.setCalendarEventCount(calendarEventMapper.selectCount(
                new LambdaQueryWrapper<CalendarEvent>().eq(CalendarEvent::getTenantId, orgId).eq(CalendarEvent::getTenantType, TenantType.ORGANIZATION)));
        stats.setWhiteboardCount(whiteboardMapper.selectCount(
                new LambdaQueryWrapper<Whiteboard>().eq(Whiteboard::getTenantId, orgId).eq(Whiteboard::getTenantType, TenantType.ORGANIZATION)));

        List<OrgMember> members = orgMemberMapper.selectList(
                new LambdaQueryWrapper<OrgMember>().eq(OrgMember::getOrganizationId, orgId));
        stats.setMemberCount(members.size());

        Set<Long> userIds = members.stream().map(OrgMember::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, String> roleMap = members.stream()
                .collect(Collectors.toMap(OrgMember::getUserId, OrgMember::getRole));

        List<StatsDTO.MemberStatsDTO> memberStats = new ArrayList<>();
        for (Long uid : userIds) {
            User user = userMap.get(uid);
            if (user == null) continue;
            StatsDTO.MemberStatsDTO ms = new StatsDTO.MemberStatsDTO();
            ms.setUserId(uid);
            ms.setUsername(user.getUsername());
            ms.setDisplayName(user.getDisplayName());
            ms.setRole(roleMap.getOrDefault(uid, "MEMBER"));
            memberStats.add(ms);
        }
        stats.setMemberStats(memberStats);
        stats.setDailyStats(buildDailyStats(orgId, TenantType.ORGANIZATION, days));
        return stats;
    }

    private List<StatsDTO.DailyStatsDTO> buildDailyStats(Long tenantId, TenantType tenantType, int days) {
        // 与前端时间筛选保持一致，统一限制在 [1, 90] 天。
        int safeDays = Math.max(1, Math.min(days, 90));
        List<StatsDTO.DailyStatsDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = safeDays - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.atTime(LocalTime.MAX);

            StatsDTO.DailyStatsDTO dto = new StatsDTO.DailyStatsDTO();
            dto.setDate(day.toString());
            // 口径说明：按“创建时间”统计当日新增笔记/任务及当日创建且已完成的任务。
            dto.setNoteCount(noteMapper.selectCount(
                    new LambdaQueryWrapper<Note>()
                            .eq(Note::getTenantId, tenantId)
                            .eq(Note::getTenantType, tenantType)
                            .ge(Note::getCreatedAt, start)
                            .le(Note::getCreatedAt, end)));
            dto.setTaskCount(taskMapper.selectCount(
                    new LambdaQueryWrapper<Task>()
                            .eq(Task::getTenantId, tenantId)
                            .eq(Task::getTenantType, tenantType)
                            .ge(Task::getCreatedAt, start)
                            .le(Task::getCreatedAt, end)));
            dto.setTaskCompletedCount(taskMapper.selectCount(
                    new LambdaQueryWrapper<Task>()
                            .eq(Task::getTenantId, tenantId)
                            .eq(Task::getTenantType, tenantType)
                            .eq(Task::getCompleted, true)
                            .ge(Task::getCreatedAt, start)
                            .le(Task::getCreatedAt, end)));
            result.add(dto);
        }
        return result;
    }

    private Long getCurrentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getId();
    }
}
