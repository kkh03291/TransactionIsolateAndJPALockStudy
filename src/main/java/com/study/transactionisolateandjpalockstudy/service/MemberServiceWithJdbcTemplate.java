package com.study.transactionisolateandjpalockstudy.service;

import com.study.transactionisolateandjpalockstudy.domain.Member;
import com.study.transactionisolateandjpalockstudy.repository.MemberRepositoryWithJdbcTemplate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class MemberServiceWithJdbcTemplate {

    private final MemberRepositoryWithJdbcTemplate repository;

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<Member> findAllWithReadUnCommit(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("{}",TransactionAspectSupport.currentTransactionStatus().isCompleted());
        return repository.findAll();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Member> findAllWithReadCommit(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("{}",TransactionAspectSupport.currentTransactionStatus().isCompleted());
        return repository.findAll();
    }

    @Transactional
    public void save(Member member, CountDownLatch latch) {
        repository.save(member);
        log.info("{}",TransactionAspectSupport.currentTransactionStatus().isCompleted());
        latch.countDown();
        log.info("{}",TransactionAspectSupport.currentTransactionStatus().isCompleted());
    }

    @Transactional
    public void save(Member member) {
        repository.save(member);
    }

    @Transactional
    public void removeAll() {
        repository.removeAll();
    }

}
