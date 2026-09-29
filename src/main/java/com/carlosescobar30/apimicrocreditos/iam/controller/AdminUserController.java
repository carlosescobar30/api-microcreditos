package com.carlosescobar30.apimicrocreditos.iam.controller;

import com.carlosescobar30.apimicrocreditos.iam.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @PatchMapping("/{username}/identity-verification")
    public ResponseEntity<Void> verifyIdentity (@PathVariable("username") String username){

        userService.verifyIdentity(username);
        return ResponseEntity.noContent().build();

    }

}
