package com.study.transactionisolateandjpalockstudy.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    private String name;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    private MemberLevel level;

    public void changeName(String name) {
        this.name = name;
    }

    @Builder
    private Member(String name, MemberLevel level) {
        this.name = name;
        this.level = level;
    }
}
