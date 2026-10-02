package com.carlosescobar30.apimicrocreditos.operational.mappers;

import com.carlosescobar30.apimicrocreditos.operational.domain.UsuryRate;
import com.carlosescobar30.apimicrocreditos.operational.dto.UsuryRateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuryRateMapper {

    @Mapping(source = "publicId", target = "usuryRateReference")
    UsuryRateDTO toUsuryRateDTO(UsuryRate usuryRate);

}
