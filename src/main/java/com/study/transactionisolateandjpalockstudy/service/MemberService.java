package com.study.transactionisolateandjpalockstudy.service;

import com.study.transactionisolateandjpalockstudy.domain.Member;
import com.study.transactionisolateandjpalockstudy.domain.MemberPhantomReadDTO;
import com.study.transactionisolateandjpalockstudy.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository repository;

    @Transactional
    public Member save(Member member) {
        return repository.save(member);
    }

    public Member find(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public void remove(Long id) {
        repository.delete(find(id));
    }

    @Transactional
    public void removeAll() {
        repository.deleteAllInBatch();
    }

    @Transactional
    public void updateName(Long id, String name) {
        find(id).changeName(name);
    }

    @Transactional
    public void awaitBeforeSave(Member member, CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        repository.save(member);
        log.info("{}",TransactionAspectSupport.currentTransactionStatus().isCompleted());
    }
    @Transactional
    public void awaitAfterSave(Member member, CountDownLatch latch) {
        repository.save(member);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("{}",TransactionAspectSupport.currentTransactionStatus().isCompleted());
    }

    public List<Member> findAll(CountDownLatch latch) {
        List<Member> all = repository.findAll();
        latch.countDown();
        log.info("{}",TransactionAspectSupport.currentTransactionStatus().isCompleted());
        return all;
    }

    public List<Member> findAll() {
        List<Member> all = repository.findAll();
        log.info("{}",TransactionAspectSupport.currentTransactionStatus().isCompleted());
        return all;
    }

    public List<Member> nonRepeatableRead(Long id, CountDownLatch latch) {
        Member firstFindMember = find(id);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        repository.updateLevel(id);   //em.clear()를 위한 의미없는 udpate
        Member secondFindMember = find(id);
        return List.of(firstFindMember, secondFindMember);
    }

    public MemberPhantomReadDTO phantomRead(Long id, CountDownLatch latch) {
        List<Member> firstResult = repository.findAll();
        latch.countDown();
        repository.updateLevel(id);
        List<Member> secondResult = repository.findAll();
        return new MemberPhantomReadDTO(firstResult, secondResult);
    }

    public MemberPhantomReadDTO phantomReadForUpdate(CountDownLatch latch) {
        List<Member> firstResult = repository.findAll();
        latch.countDown();
        List<Member> secondResult = repository.findAllForUpdate();
        return new MemberPhantomReadDTO(firstResult, secondResult);
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public<T> T doReadUnCommit(Supplier<T> supplier) {
        return supplier.get();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public<T> T doReadCommit(Supplier<T> supplier) {
        return supplier.get();
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public<T> T doRepeatableRead(Supplier<T> supplier) {
        return supplier.get();
    }
}
