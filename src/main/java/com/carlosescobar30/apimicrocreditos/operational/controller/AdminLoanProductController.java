package com.carlosescobar30.apimicrocreditos.operational.controller;

import com.carlosescobar30.apimicrocreditos.operational.dto.AvailableLoanProductDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.LoanProductRequestDTO;
import com.carlosescobar30.apimicrocreditos.operational.service.LoanProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/product")
@RequiredArgsConstructor
public class AdminLoanProductController {

    private final LoanProductService loanProductService;

    @PostMapping
    public ResponseEntity<AvailableLoanProductDTO> create(@Valid @RequestBody LoanProductRequestDTO request){

        AvailableLoanProductDTO loanProduct = loanProductService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(loanProduct);

    }

}
