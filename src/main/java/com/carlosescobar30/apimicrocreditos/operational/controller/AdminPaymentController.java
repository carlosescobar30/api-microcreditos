package com.carlosescobar30.apimicrocreditos.operational.controller;

import com.carlosescobar30.apimicrocreditos.operational.dto.PaymentInfoDetailsDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.TransactionValidationDTO;
import com.carlosescobar30.apimicrocreditos.operational.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/payment")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/validate")
    public ResponseEntity<PaymentInfoDetailsDTO> validate (@Valid @RequestBody TransactionValidationDTO transactionValidation){

        PaymentInfoDetailsDTO paymentInfoDTO = paymentService.validateTransaction(transactionValidation);
        return ResponseEntity.ok(paymentInfoDTO);

    }

}
