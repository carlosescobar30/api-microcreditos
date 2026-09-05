package com.carlosescobar30.apimicrocreditos.iam.service;

import com.carlosescobar30.apimicrocreditos.common.exception.notfound.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.iam.domain.Role;
import com.carlosescobar30.apimicrocreditos.iam.domain.enums.RoleName;
import com.carlosescobar30.apimicrocreditos.iam.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository repository;

    public Role getRole (RoleName roleName) {

        return repository.findByName(roleName)
                .orElseThrow(ResourceNotFoundException::new);

    }

}
