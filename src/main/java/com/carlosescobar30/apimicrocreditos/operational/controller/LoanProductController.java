package com.carlosescobar30.apimicrocreditos.operational.controller;

import com.carlosescobar30.apimicrocreditos.operational.dto.AvailableLoanProductDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.LoanProductSpecificationDTO;
import com.carlosescobar30.apimicrocreditos.operational.service.LoanProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class LoanProductController {

    private final LoanProductService loanProductService;

    @GetMapping("/all")
    public ResponseEntity<Page<AvailableLoanProductDTO>> getAllProducts(Pageable pageable) {

        Page<AvailableLoanProductDTO> availableLoanProductDTOS = loanProductService.getAll(pageable);
        return ResponseEntity.ok(availableLoanProductDTOS);

    }

    @GetMapping("/{reference}")
    public ResponseEntity<AvailableLoanProductDTO> getOneProductByReference(
            @PathVariable("reference") UUID loanProductReference){

        AvailableLoanProductDTO availableLoanProductDTO = loanProductService.getOneByReference(loanProductReference);
        return ResponseEntity.ok(availableLoanProductDTO);

    }

    @GetMapping("/search")
    public ResponseEntity<Page<AvailableLoanProductDTO>> search(LoanProductSpecificationDTO dto, Pageable pageable){

        Page<AvailableLoanProductDTO> availableLoanProductDTOS = loanProductService.getBySpecification(dto, pageable);
        return ResponseEntity.ok(availableLoanProductDTOS);

    }





}
