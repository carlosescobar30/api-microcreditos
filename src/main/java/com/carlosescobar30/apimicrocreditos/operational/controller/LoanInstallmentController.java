package com.carlosescobar30.apimicrocreditos.operational.controller;

import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.operational.dto.InstallmentsInfoDTO;
import com.carlosescobar30.apimicrocreditos.operational.service.LoanInstallmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("installment")
@RequiredArgsConstructor
public class LoanInstallmentController {

    private final LoanInstallmentService loanInstallmentService;


    @GetMapping("/{loanInstallmentReference}")
    public ResponseEntity<InstallmentsInfoDTO> getOne (
            @PathVariable("loanInstallmentReference") UUID loanInstallmentReference){

        InstallmentsInfoDTO installment = loanInstallmentService.getOne(loanInstallmentReference);
        return ResponseEntity.ok(installment);

    }

    @GetMapping("/current/{reference}")
    public ResponseEntity<InstallmentsInfoDTO> getCurrent(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                          @PathVariable("reference") UUID loanReference){

        InstallmentsInfoDTO installment = loanInstallmentService.getCurrent(userDetails, loanReference);
        return ResponseEntity.ok(installment);

    }

    @GetMapping("/overdue/{reference}/all")
    public ResponseEntity<Page<InstallmentsInfoDTO>> getAllOverdue(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                   @PathVariable("reference") UUID loanReference,
                                                                   Pageable pageable){

        Page<InstallmentsInfoDTO> installment = loanInstallmentService.getAllOverdue(userDetails, loanReference, pageable);
        return ResponseEntity.ok(installment);

    }

    @GetMapping("{reference}/all")
    public ResponseEntity<Page<InstallmentsInfoDTO>> getAll(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                            @PathVariable("reference") UUID loanReference,
                                                            Pageable pageable){

        Page<InstallmentsInfoDTO> installment = loanInstallmentService.getAll(userDetails, loanReference, pageable);
        return ResponseEntity.ok(installment);

    }

}
