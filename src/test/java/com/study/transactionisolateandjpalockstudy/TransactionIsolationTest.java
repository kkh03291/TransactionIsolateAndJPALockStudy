package com.study.transactionisolateandjpalockstudy;

import com.study.transactionisolateandjpalockstudy.domain.Member;
import com.study.transactionisolateandjpalockstudy.domain.MemberLevel;
import com.study.transactionisolateandjpalockstudy.service.MemberServiceWithJdbcTemplate;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.*;

@Slf4j
@SpringBootTest
public class TransactionIsolationTest {

    @Autowired
    MemberServiceWithJdbcTemplate service;

    @AfterEach
    void tearDown() {
        service.removeAll();
    }

    @Test
    @DisplayName("Isolation=Read_Commit이면, Dirty Read가 발생하지 않는다.")
    public void test() throws ExecutionException, InterruptedException {

        //given
        Member user = Member.builder()
                .name("user")
                .level(MemberLevel.USER)
                .build();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        //when
        Future<?> submit = executorService.submit(() -> service.save(user, latch));
        Future<List<Member>> find = executorService.submit(() -> service.findAllWithReadCommit(latch));
        List<Member> members = find.get();
        submit.get();

        //then
        Assertions.assertThat(members).hasSize(0);
    }

    @Test
    @DisplayName("Isolation=Read_UnCommit이면, Dirty Read가 발생한다.")
    public void test2() throws ExecutionException, InterruptedException {
        //given
        Member user = Member.builder()
                .name("user")
                .level(MemberLevel.USER)
                .build();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        //when
        Future<?> submit = executorService.submit(() -> service.save(user, latch));
        Future<List<Member>> submit1 = executorService.submit(() -> service.findAllWithReadUnCommit(latch));
        List<Member> members = submit1.get();
        submit.get();

        Assertions.assertThat(members).hasSize(1);
    }
}
