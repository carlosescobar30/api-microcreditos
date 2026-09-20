package com.carlosescobar30.apimicrocreditos.iam.service;

import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.iam.domain.User;
import com.carlosescobar30.apimicrocreditos.iam.repository.UserRepository;
import com.carlosescobar30.apimicrocreditos.iam.service.contract.UserScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class MockUserScoreServiceImpl implements UserScoreService {

    private final UserRepository repository;
    private final Random random = new Random();

    @Transactional
    public int validateScoreUser(Long userId){

        int probability = generateRandomValue(1, 10);

        User user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (probability > 8 ){

            user.setScore(getUserScore());
            return user.getScore();

        }

        return user.getScore();

    };

    public int getUserScore(){

        return generateRandomValue(999,1);

    }

    private int generateRandomValue (Integer max, Integer min){

        return random.nextInt((max - min) + 1 ) + min;

    }

}
