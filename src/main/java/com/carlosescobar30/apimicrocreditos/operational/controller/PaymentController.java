package com.carlosescobar30.apimicrocreditos.operational.controller;

import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.operational.dto.PayRequestDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.PaymentInfoDTO;
import com.carlosescobar30.apimicrocreditos.operational.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{loanReference}")
    public ResponseEntity<PaymentInfoDTO> payOneInstallment (@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                            @PathVariable("loanReference") UUID loanReference,
                                                            @RequestBody PayRequestDTO payRequestDTO){

        PaymentInfoDTO paymentInfoDTO = paymentService.pay(userDetails, loanReference, payRequestDTO);
        return ResponseEntity.ok(paymentInfoDTO);

    }

}
