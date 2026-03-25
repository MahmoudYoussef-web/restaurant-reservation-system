package com.mahmoud.reservation.entity;

import com.mahmoud.reservation.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

@Entity
@Where(clause = "is_deleted = false")
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_role_name", columnNames = "name")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private RoleName name;
}