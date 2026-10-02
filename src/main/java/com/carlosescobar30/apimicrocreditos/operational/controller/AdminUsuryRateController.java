package com.carlosescobar30.apimicrocreditos.operational.controller;

import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import com.carlosescobar30.apimicrocreditos.operational.dto.UsuryRateDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.UsuryRateRequestDTO;
import com.carlosescobar30.apimicrocreditos.operational.service.UsuryRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/usury-rate")
@RequiredArgsConstructor
public class AdminUsuryRateController {

    private final UsuryRateService usuryRateService;

    @PostMapping
    public ResponseEntity<UsuryRateDTO> register(@Valid @RequestBody UsuryRateRequestDTO request){

        UsuryRateDTO usuryRate = usuryRateService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuryRate);

    }

    @GetMapping
    public ResponseEntity<Page<UsuryRateDTO>> getAll(
            @RequestParam(value = "modality", required = false) CreditModality modality,
            @PageableDefault(sort = "validFrom", direction = Sort.Direction.DESC) Pageable pageable){

        return ResponseEntity.ok(usuryRateService.getAll(modality, pageable));

    }

}
