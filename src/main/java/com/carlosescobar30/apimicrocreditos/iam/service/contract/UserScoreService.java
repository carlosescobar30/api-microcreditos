package com.carlosescobar30.apimicrocreditos.iam.service.contract;

public interface UserScoreService {

    int validateScoreUser(Long userId);
    int getUserScore();

}
