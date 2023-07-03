package com.study.transactionisolateandjpalockstudy.repository;

import com.study.transactionisolateandjpalockstudy.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
}
