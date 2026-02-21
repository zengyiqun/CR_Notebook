package com.cr.notebook.controller;

import com.cr.notebook.dto.OrgMemberDTO;
import com.cr.notebook.dto.OrganizationDTO;
import com.cr.notebook.service.OrganizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrganizationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private OrganizationService organizationService;
    @InjectMocks private OrganizationController organizationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(organizationController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void listMyOrganizations_shouldReturn200() throws Exception {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(1L);
        dto.setName("My Org");
        dto.setRole("OWNER");
        dto.setMemberCount(3);

        when(organizationService.listMyOrganizations()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("My Org"));
    }

    @Test
    void createOrganization_shouldReturn200() throws Exception {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(1L);
        dto.setName("New Org");
        dto.setRole("OWNER");
        dto.setMemberCount(1);

        when(organizationService.createOrganization("New Org")).thenReturn(dto);

        mockMvc.perform(post("/api/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "New Org"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Org"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void getOrganization_shouldReturn200() throws Exception {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(10L);
        dto.setName("Org");
        when(organizationService.getOrganization(10L)).thenReturn(dto);

        mockMvc.perform(get("/api/organizations/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void updateOrganization_shouldReturn200() throws Exception {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(10L);
        dto.setName("Updated");
        when(organizationService.updateOrganization(eq(10L), eq("Updated"))).thenReturn(dto);

        mockMvc.perform(put("/api/organizations/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void deleteOrganization_shouldReturn204() throws Exception {
        doNothing().when(organizationService).deleteOrganization(10L);

        mockMvc.perform(delete("/api/organizations/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listMembers_shouldReturn200() throws Exception {
        OrgMemberDTO m = new OrgMemberDTO();
        m.setId(1L);
        m.setUserId(1L);
        m.setUsername("user1");
        m.setRole("OWNER");
        m.setJoinedAt(LocalDateTime.now());

        when(organizationService.listMembers(10L)).thenReturn(List.of(m));

        mockMvc.perform(get("/api/organizations/10/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void addMember_shouldReturn200() throws Exception {
        OrgMemberDTO m = new OrgMemberDTO();
        m.setId(5L);
        m.setUserId(3L);
        m.setUsername("newuser");
        m.setRole("MEMBER");

        when(organizationService.addMember(eq(10L), eq(3L), eq("MEMBER"))).thenReturn(m);

        mockMvc.perform(post("/api/organizations/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", 3, "role", "MEMBER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(3))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void updateMemberRole_shouldReturn200() throws Exception {
        OrgMemberDTO m = new OrgMemberDTO();
        m.setId(2L);
        m.setUserId(3L);
        m.setUsername("user3");
        m.setRole("ADMIN");

        when(organizationService.updateMemberRole(eq(10L), eq(3L), eq("ADMIN"))).thenReturn(m);

        mockMvc.perform(put("/api/organizations/10/members/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.userId").value(3));
    }

    @Test
    void removeMember_shouldReturn204() throws Exception {
        doNothing().when(organizationService).removeMember(10L, 3L);

        mockMvc.perform(delete("/api/organizations/10/members/3"))
                .andExpect(status().isNoContent());
    }
}
