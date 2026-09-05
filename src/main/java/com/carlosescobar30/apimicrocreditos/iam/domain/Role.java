package com.carlosescobar30.apimicrocreditos.iam.domain;

import com.carlosescobar30.apimicrocreditos.iam.domain.enums.RoleName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Table(name = "roles", schema = "iam")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Enumerated(value = EnumType.STRING)
    private RoleName name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return name == role.getName();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
