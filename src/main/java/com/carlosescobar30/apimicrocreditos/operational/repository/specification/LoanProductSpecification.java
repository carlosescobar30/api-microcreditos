package com.carlosescobar30.apimicrocreditos.operational.repository.specification;

import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.dto.LoanProductSpecificationDTO;
import org.springframework.data.jpa.domain.Specification;

public class LoanProductSpecification {

    private static Specification<LoanProduct> addPrincipal(LoanProductSpecificationDTO dto){

        return ((root, query, cB) ->{


            if (dto.minPrincipal() != null && dto.maxPrincipal() != null){

                return cB.between(root.get("totalPrincipal") ,dto.minPrincipal(), dto.maxPrincipal());

            }

            if (dto.minPrincipal() != null){

                return cB.equal(root.get("totalPrincipal"),dto.minPrincipal());

            }

            return cB.equal(root.get("totalPrincipal"),dto.maxPrincipal());

        });

    }

    private static Specification<LoanProduct> addInstallments (LoanProductSpecificationDTO dto){




            return ((root, query, cB) -> {

                if (dto.minInstallments() != null && dto.maxInstallments() != null) {

                    return cB.between(root.get("installments"), dto.minInstallments(), dto.maxInstallments());

                }

                if (dto.minInstallments() != null){

                    return cB.equal(root.get("installments"), dto.minInstallments());

                }

                return cB.equal(root.get("installments"), dto.maxInstallments());

            });

    }

    public static Specification<LoanProduct> build (LoanProductSpecificationDTO dto){

        Specification<LoanProduct> specification = ((root, query, cB) ->
                cB.conjunction());

        if (dto.minPrincipal() != null || dto.maxPrincipal() != null){

            specification = specification.and(addPrincipal(dto));

        }

        if (dto.minInstallments() != null || dto.maxInstallments() != null){

            specification = specification.and(addInstallments(dto));

        }

        return specification;

    }

}
