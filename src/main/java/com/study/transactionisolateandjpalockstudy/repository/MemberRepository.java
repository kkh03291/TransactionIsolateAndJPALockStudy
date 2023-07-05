package com.study.transactionisolateandjpalockstudy.repository;

import com.study.transactionisolateandjpalockstudy.domain.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Modifying(clearAutomatically = true)
    @Query("update Member m set m.level = 'USER' where m.id = :id")
    Integer updateLevel(@Param("id")Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m")
    List<Member> findAllForUpdate();
}
