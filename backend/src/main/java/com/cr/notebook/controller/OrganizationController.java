package com.cr.notebook.controller;

import com.cr.notebook.dto.OrgMemberDTO;
import com.cr.notebook.dto.OrganizationDTO;
import com.cr.notebook.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<List<OrganizationDTO>> listMyOrganizations() {
        return ResponseEntity.ok(organizationService.listMyOrganizations());
    }

    @PostMapping
    public ResponseEntity<OrganizationDTO> createOrganization(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        return ResponseEntity.ok(organizationService.createOrganization(name));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDTO> getOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getOrganization(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationDTO> updateOrganization(@PathVariable Long id,
                                                               @RequestBody Map<String, String> body) {
        String name = body.get("name");
        return ResponseEntity.ok(organizationService.updateOrganization(id, name));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable Long id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<OrgMemberDTO>> listMembers(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.listMembers(id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<OrgMemberDTO> addMember(@PathVariable Long id,
                                                   @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String role = body.get("role") != null ? body.get("role").toString() : null;
        return ResponseEntity.ok(organizationService.addMember(id, userId, role));
    }

    @PutMapping("/{id}/members/{userId}")
    public ResponseEntity<OrgMemberDTO> updateMemberRole(@PathVariable Long id,
                                                          @PathVariable Long userId,
                                                          @RequestBody Map<String, String> body) {
        String role = body.get("role");
        return ResponseEntity.ok(organizationService.updateMemberRole(id, userId, role));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        organizationService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/avatar")
    public ResponseEntity<Map<String, String>> updateOrgAvatar(@PathVariable Long id,
                                                                @RequestBody Map<String, String> body) {
        String avatarUrl = body.get("avatarUrl");
        organizationService.updateOrgAvatar(id, avatarUrl);
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl != null ? avatarUrl : ""));
    }
}
