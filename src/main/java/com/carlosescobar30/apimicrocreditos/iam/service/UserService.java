package com.carlosescobar30.apimicrocreditos.iam.service;

import com.carlosescobar30.apimicrocreditos.common.exception.conflict.EmailConflictException;
import com.carlosescobar30.apimicrocreditos.common.exception.conflict.UsernameConflictException;
import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.iam.domain.Role;
import com.carlosescobar30.apimicrocreditos.iam.domain.User;
import com.carlosescobar30.apimicrocreditos.iam.domain.enums.RoleName;
import com.carlosescobar30.apimicrocreditos.iam.dto.RegisterRequestDTO;
import com.carlosescobar30.apimicrocreditos.iam.repository.UserRepository;
import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.security.UserDetailsMapper;
import com.carlosescobar30.apimicrocreditos.iam.service.contract.UserScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final UserScoreService userScoreService;


    @Transactional
    public void createUser (RegisterRequestDTO req){

        if(repository.existsByUsername(req.username())){

            throw new  UsernameConflictException();

        }

        if(repository.existsByEmail(req.email())){

            throw new EmailConflictException();

        }



        Set<Role> defaultRole = new HashSet<>();
        defaultRole.add(roleService.getRole(RoleName.ROLE_USER));
        User user = User.builder()
                .name(req.name())
                .lastName(req.lastName())
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .email(req.email())
                .roles(defaultRole)
                .score(userScoreService.getUserScore())
                .isIdentityVerified(false)
                .birthDate(req.birthDate())
                .build();

        User userCreated = repository.save(user);
        log.info("User created for userId: {}", userCreated.getId());

    }

    @Transactional
    public void verifyIdentity (String username){

        User user = repository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsIdentityVerified(true);
        log.info("Identity verified for userId: {}", user.getId());

    }

    @Transactional
    public void createAdminIfMissing (String username, String email, String rawPassword){

        if (repository.existsByUsername(username) || repository.existsByEmail(email)){

            log.info("Admin account not created: the username or email is already registered");
            return;

        }

        Set<Role> adminRole = new HashSet<>();
        adminRole.add(roleService.getRole(RoleName.ROLE_ADMIN));
        User admin = User.builder()
                .name("Admin")
                .lastName("Admin")
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .email(email)
                .roles(adminRole)
                .score(0)
                .isIdentityVerified(true)
                .birthDate(LocalDate.of(1970, 1, 1))
                .build();

        User adminCreated = repository.save(admin);
        log.info("Admin account created for userId: {}", adminCreated.getId());

    }

    public User getReference (Long id) {

        log.debug("User reference created for userId: {}", id);
        return repository.getReferenceById(id);

    }

    public UserDetailsImpl getUserDetailImpl (Long id){

        User user = repository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserDetailsMapper.build(user);
    }

}
