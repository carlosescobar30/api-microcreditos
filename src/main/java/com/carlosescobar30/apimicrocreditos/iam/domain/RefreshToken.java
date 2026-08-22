package com.carlosescobar30.apimicrocreditos.iam.domain;

import com.carlosescobar30.apimicrocreditos.common.domain.EntityBaseClass;
import com.carlosescobar30.apimicrocreditos.iam.domain.enums.RefreshTokenStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Entity
@Table(name = "refresh_tokens", schema = "iam")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken extends EntityBaseClass {

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private RefreshTokenStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Instant rotatedAt;

    @Column(nullable = false)
    private Instant expiresAt;

}
