package com.carlosescobar30.apimicrocreditos.operational.controller;

import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.operational.dto.LoanAndInstallmentsInfoDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.LoanAvailabilityDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.LoanInfoDTO;
import com.carlosescobar30.apimicrocreditos.operational.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/loan")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/request/{reference}")
    public ResponseEntity<LoanAvailabilityDTO> requestLoan (@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                            @PathVariable("reference") UUID loanProductReference){
        LoanAvailabilityDTO loanAvailabilityDTO = loanService.request(userDetails, loanProductReference);
        return ResponseEntity.ok(loanAvailabilityDTO);


    };

    @DeleteMapping("/cancelled/{reference}")
    public ResponseEntity<Void> cancelPreApprovedLoan(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                      @PathVariable("reference") UUID loanProductReference){

        loanService.cancel(userDetails, loanProductReference);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }

    @PostMapping("/accept/{reference}")
    public ResponseEntity<LoanAndInstallmentsInfoDTO> acceptPreApprovedLoan(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                            @PathVariable("reference") UUID loanReference){

         LoanAndInstallmentsInfoDTO loanAndInstallmentsInfoDTO = loanService.accept(userDetails, loanReference);
         return ResponseEntity.ok(loanAndInstallmentsInfoDTO);

    }

    @GetMapping("/get/all")
    public ResponseEntity<Page<LoanInfoDTO>> getAll (@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                     Pageable pageable){

        Page<LoanInfoDTO> loanInfoDTO = loanService.getAll(userDetails, pageable);
        return ResponseEntity.ok(loanInfoDTO);

    }

    @GetMapping("/get/{reference}")
    public ResponseEntity<LoanInfoDTO> getAll (@AuthenticationPrincipal UserDetailsImpl userDetails,
                                               @PathVariable("reference") UUID loanReference){

        LoanInfoDTO loanInfoDTO = loanService.getOne(userDetails, loanReference);
        return ResponseEntity.ok(loanInfoDTO);

    }



}
