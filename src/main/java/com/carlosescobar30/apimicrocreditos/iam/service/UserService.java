package com.carlosescobar30.apimicrocreditos.iam.service;

import com.carlosescobar30.apimicrocreditos.common.exception.conflict.EmailConflictException;
import com.carlosescobar30.apimicrocreditos.common.exception.conflict.UsernameConflictException;
import com.carlosescobar30.apimicrocreditos.common.exception.notfound.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.iam.domain.Role;
import com.carlosescobar30.apimicrocreditos.iam.domain.User;
import com.carlosescobar30.apimicrocreditos.iam.domain.enums.RoleName;
import com.carlosescobar30.apimicrocreditos.iam.dto.RegisterRequestDTO;
import com.carlosescobar30.apimicrocreditos.iam.repository.UserRepository;
import com.carlosescobar30.apimicrocreditos.iam.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;


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
                .isIdentityVerified(false)
                .birthDate(req.birthDate())
                .build();

        User userCreated = repository.save(user);
        log.info("User created for userId: {}", userCreated.getId());

    }

    public User getReference (Long id) {

        log.debug("User reference created for userId: {}", id);
        return repository.getReferenceById(id);

    }

    public UserDetailsImpl getUserDetailImpl (Long id){

        User user = repository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserDetailsImpl.build(user);
    }

}
