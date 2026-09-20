package com.carlosescobar30.apimicrocreditos.operational.mappers;

import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.dto.AvailableLoanProductDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoanProductMapper {


    @Mapping(source = "publicId", target = "loanProductReference")
    AvailableLoanProductDTO toAvailableLoanProductsDTO (LoanProduct loanProduct);

}
