package com.carlosescobar30.apimicrocreditos.iam.service;

import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.iam.adapter.UserAdapter;
import com.carlosescobar30.apimicrocreditos.iam.domain.User;
import com.carlosescobar30.apimicrocreditos.iam.dto.UserAdapterResponseDTO;
import com.carlosescobar30.apimicrocreditos.iam.repository.UserRepository;
import com.carlosescobar30.apimicrocreditos.iam.service.contract.UserScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAdapterImpl implements UserAdapter {

    private final UserRepository repository;
    private final UserScoreService userScoreService;

    @Override
    public UserAdapterResponseDTO userInfo(Long userId) {

        User user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserAdapterResponseDTO.builder()
                .isIdentityVerified(user.getIsIdentityVerified())
                .score(userScoreService.validateScoreUser(user.getId()))
                .userReference(user.getPublicId())
                .build();

    }

}
