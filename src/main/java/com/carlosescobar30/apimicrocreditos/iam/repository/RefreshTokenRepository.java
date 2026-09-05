package com.carlosescobar30.apimicrocreditos.iam.repository;

import com.carlosescobar30.apimicrocreditos.iam.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;


import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {



    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token")
    Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);



    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshToken rt " +
            "SET rt.status = com.carlosescobar30.apimicrocreditos.iam.domain.enums.RefreshTokenStatus.ROTATED , " +
            "rt.rotatedAt = :now ," +
            "rt.lastUpdate = :now " +
            "WHERE rt.token = :token " +
            "AND rt.status = com.carlosescobar30.apimicrocreditos.iam.domain.enums.RefreshTokenStatus.ACTIVE ")
    int rotate (
            @Param("token") String token,
            @Param("now") Instant now);


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshToken rt " +
            "SET rt.status = com.carlosescobar30.apimicrocreditos.iam.domain.enums.RefreshTokenStatus.REVOKED ," +
            "rt.lastUpdate = :now " +
            "WHERE rt.token = :token")
    void revoke(@Param("token") String token, @Param("now") Instant now);


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshToken rt " +
            "SET rt.status = com.carlosescobar30.apimicrocreditos.iam.domain.enums.RefreshTokenStatus.REVOKED , " +
            "rt.lastUpdate = :now " +
            "WHERE rt.user.id = :userId " +
            "AND rt.status != com.carlosescobar30.apimicrocreditos.iam.domain.enums.RefreshTokenStatus.REVOKED")
    void revokeAllByUser (@Param("userId") Long userId,
                          @Param("now") Instant now);


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RefreshToken rt " +
            "WHERE rt.expiresAt < :now " +
            "OR rt.status = com.carlosescobar30.apimicrocreditos.iam.domain.enums.RefreshTokenStatus.REVOKED")
    int removeRevokedAndExpired(@Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RefreshToken rt " +
            "WHERE rt.rotatedAt < :yesterday " +
            "AND rt.status = com.carlosescobar30.apimicrocreditos.iam.domain.enums.RefreshTokenStatus.ROTATED")
    int removeRotated(@Param("yesterday") Instant yesterday);





}
