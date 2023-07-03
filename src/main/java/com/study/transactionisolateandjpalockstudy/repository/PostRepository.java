package com.study.transactionisolateandjpalockstudy.repository;

import com.study.transactionisolateandjpalockstudy.domain.Post;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p from Post p join fetch p.author where p.id = :id")
    Optional<Post> findById(@Param("id") Long id);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("select p from Post p join fetch p.author where p.id = :id")
    Optional<Post> findByIdWithOptimisticLock(@Param("id") Long id);

    @Lock(LockModeType.OPTIMISTIC)
    Optional<Post> findOnlyPostById(Long id);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select p from Post p join fetch p.author where p.id = :id")
    Optional<Post> findByIdWithOptimisticForceIncrementLock(@Param("id") Long id);
}
