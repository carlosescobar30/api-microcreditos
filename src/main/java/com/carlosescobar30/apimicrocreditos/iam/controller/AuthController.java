package com.carlosescobar30.apimicrocreditos.iam.controller;

import com.carlosescobar30.apimicrocreditos.iam.dto.LoginRequestDTO;
import com.carlosescobar30.apimicrocreditos.iam.dto.RefreshRequestDTO;
import com.carlosescobar30.apimicrocreditos.iam.dto.RegisterRequestDTO;
import com.carlosescobar30.apimicrocreditos.iam.dto.TokenResponseDTO;
import com.carlosescobar30.apimicrocreditos.iam.security.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<Void> register (@Valid @RequestBody RegisterRequestDTO req) {

      authService.register(req);
      return ResponseEntity.status(HttpStatus.CREATED).build();

  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponseDTO> login (@Valid  @RequestBody LoginRequestDTO req) {

      return ResponseEntity.ok()
              .cacheControl(CacheControl.noStore())
              .body(authService.login(req));

  }

  @PostMapping("/refresh")
  public ResponseEntity<TokenResponseDTO> refresh (@Valid @RequestBody RefreshRequestDTO req) {

      return ResponseEntity.ok()
              .cacheControl(CacheControl.noStore())
              .body(authService.refresh(req));

  }

  @PostMapping("/logout")
    public ResponseEntity<Void> logout (@AuthenticationPrincipal UserDetailsImpl userDetails){

      authService.logout(userDetails.getId());
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

  }


}
