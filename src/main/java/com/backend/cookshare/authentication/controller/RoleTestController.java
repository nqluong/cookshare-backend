package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.enums.UserRole;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class RoleTestController {

    @GetMapping("/user")
    @PreAuthorize("hasPermission(null, 'USER')")
    public ResponseEntity<String> userEndpoint() {
        return ResponseEntity.ok("Chỉ USER có thể truy cập endpoint này (ADMIN tự động có quyền)");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasPermission(null, 'ADMIN')")
    public ResponseEntity<String> adminEndpoint() {
        return ResponseEntity.ok("Chỉ ADMIN có thể truy cập endpoint này");
    }

    @GetMapping("/any-role")
    @PreAuthorize("hasPermission(null, 'USER')")
    public ResponseEntity<String> anyRoleEndpoint() {
        return ResponseEntity.ok("USER có thể truy cập (ADMIN tự động có quyền)");
    }
}
