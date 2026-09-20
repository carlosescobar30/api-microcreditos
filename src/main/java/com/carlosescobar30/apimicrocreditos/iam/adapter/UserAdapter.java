package com.carlosescobar30.apimicrocreditos.iam.adapter;

import com.carlosescobar30.apimicrocreditos.iam.dto.UserAdapterResponseDTO;

import java.util.Random;

public interface UserAdapter {

    UserAdapterResponseDTO userInfo(Long id);

}
