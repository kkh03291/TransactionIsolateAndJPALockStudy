package com.study.transactionisolateandjpalockstudy;

import com.study.transactionisolateandjpalockstudy.domain.Member;
import com.study.transactionisolateandjpalockstudy.domain.MemberLevel;
import com.study.transactionisolateandjpalockstudy.domain.MemberPhantomReadDTO;
import com.study.transactionisolateandjpalockstudy.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@Slf4j
@SpringBootTest
public class TransactionIsolationTest {

    @Autowired
    MemberService service;


    @AfterEach
    void tearDown() {
//        postService.removeAll();
        service.removeAll();
    }

//    @Autowired
//    MemberServiceWithJdbcTemplate service;
//
//    @AfterEach
//    void tearDown() {
//        service.removeAll();
//    }
//
//    @Test
//    @DisplayName("Isolation=Read_Commit이면, Dirty Read가 발생하지 않는다.")
//    public void test() throws ExecutionException, InterruptedException {
//
//        //given
//        Member user = Member.builder()
//                .name("user")
//                .level(MemberLevel.USER)
//                .build();
//        ExecutorService executorService = Executors.newFixedThreadPool(2);
//        CountDownLatch latch = new CountDownLatch(1);
//        //when
//        Future<?> submit = executorService.submit(() -> service.save(user, latch));
//        Future<List<Member>> find = executorService.submit(() -> service.findAllWithReadCommit(latch));
//        List<Member> members = find.get();
//        submit.get();
//
//        //then
//        assertThat(members).hasSize(0);
//    }
//
//    @Test
//    @DisplayName("Isolation=Read_UnCommit이면, Dirty Read가 발생한다.")
//    public void test2() throws ExecutionException, InterruptedException {
//        //given
//        Member user = Member.builder()
//                .name("user")
//                .level(MemberLevel.USER)
//                .build();
//        ExecutorService executorService = Executors.newFixedThreadPool(2);
//        CountDownLatch latch = new CountDownLatch(1);
//        //when
//        Future<?> submit = executorService.submit(() -> service.save(user, latch));
//        Future<List<Member>> submit1 = executorService.submit(() -> service.findAllWithReadUnCommit(latch));
//        List<Member> members = submit1.get();
//        submit.get();
//
//        assertThat(members).hasSize(1);
//    }

    @Test
    @DisplayName("Isolation=Read_UnCommit이면, Dirty Read가 발생한다.")
    public void test() throws ExecutionException, InterruptedException {
        //given
        Member user = Member.builder()
                .name("user")
                .level(MemberLevel.USER)
                .build();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        //when
        Future<?> submit = executorService.submit(() -> service.save(user));
        Future<List<Member>> submit1 = executorService.submit(
                () -> service.doReadUnCommit(service::findAll)
        );
        List<Member> members = submit1.get();
        submit.get();

        assertThat(members).hasSize(1)
                .extracting("id", "name", "level")
                .containsAnyOf(tuple(user.getId(), user.getName(), user.getLevel()));
    }

    @Test
    @DisplayName("Isolation=Read_UnCommit이면, Non-Repeatable Read가 발생한다.")
    public void test2() throws ExecutionException, InterruptedException {
        //given
        Member user = Member.builder()
                .name("user")
                .level(MemberLevel.USER)
                .build();
        service.save(user);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        //when
        Future<List<Member>> submit = executorService.submit(
                () -> service.doReadUnCommit(
                        () -> service.nonRepeatableRead(user.getId(), latch)
                )
        );
        Future<?> submit1 = executorService.submit(() -> {
            service.updateName(user.getId(), "user33");
            latch.countDown();
        });
        submit1.get();
        List<Member> members = submit.get();

        //then
        assertThat(members).extracting("name")
                .containsExactly("user", "user33");
    }

    @Test
    @DisplayName("Isolation=Read_UnCommit이면, Phantom Read가 발생한다.")
    public void test3() throws ExecutionException, InterruptedException {
        //given
        Member user = Member.builder()
                .name("user")
                .level(MemberLevel.USER)
                .build();
        Member admin = Member.builder()
                .name("admin")
                .level(MemberLevel.ADMIN)
                .build();

        service.save(user);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        //when
        Future<MemberPhantomReadDTO> submit = executorService.submit(
                () -> service.doReadUnCommit(
                        () -> service.phantomRead(user.getId(), latch)
                )
        );

        Future<?> submit1 = executorService.submit(() -> service.awaitBeforeSave(admin, latch));
        submit1.get();
        MemberPhantomReadDTO memberPhantomReadDTO = submit.get();

        //then
        assertThat(memberPhantomReadDTO.getFirstResult()).hasSize(1)
                .extracting("name", "level")
                .containsExactly(
                        tuple("user", MemberLevel.USER)
                );
        assertThat(memberPhantomReadDTO.getSecondResult()).hasSize(2)
                .extracting("name", "level")
                .containsExactly(
                        tuple("user", MemberLevel.USER),
                        tuple("admin", MemberLevel.ADMIN)
                );
    }

    @Test
    @DisplayName("Isolation=Read_Commit이면, Dirty Read가 발생하지 않는다.")
    public void test4() throws ExecutionException, InterruptedException {
        //given
        Member user = Member.builder()
                .name("user")
                .level(MemberLevel.USER)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        //when
        Future<?> submit = executorService.submit(() -> service.awaitAfterSave(user, latch));
        Future<List<Member>> submit1 = executorService.submit(
                () -> service.doReadCommit(
                        () -> service.findAll(latch)
                )
        );

        submit.get();
        List<Member> members = submit1.get();
        //then
        assertThat(members).isEmpty();
    }

    @Test
    @DisplayName("Isolation=Read_Commit이면, Non-Repeatable Read가 발생한다.")
    public void test5() throws ExecutionException, InterruptedException {
        //given
        Member user = Member.builder()
                .name("user")
                .level(MemberLevel.USER)
                .build();
        service.save(user);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        //when
        Future<List<Member>> submit = executorService.submit(
                () -> service.doReadCommit(
                        () -> service.nonRepeatableRead(user.getId(), latch)
                )
        );

        Future<?> submit1 = executorService.submit(() -> {
            service.updateName(user.getId(), "user33");
            latch.countDown();
        });
        submit1.get();
        List<Member> members = submit.get();

        //then
        assertThat(members).extracting("name")
                .containsExactly("user", "user33");
    }

    @Test
    @DisplayName("Isolation=Read_Commit이면, Phantom Read가 발생한다.")
    public void test6() throws ExecutionException, InterruptedException {
        //given
        Member user = Member.builder()
                .name("user")
                .level(MemberLevel.USER)
                .build();
        Member admin = Member.builder()
                .name("admin")
                .level(MemberLevel.ADMIN)
                .build();

        service.save(user);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        //when
        Future<MemberPhantomReadDTO> submit = executorService.submit(
                () -> service.doReadCommit(
                        () -> service.phantomRead(user.getId(), latch)
                )
        );

        Future<?> submit1 = executorService.submit(() -> service.save(admin));

        submit1.get();
        MemberPhantomReadDTO memberPhantomReadDTO = submit.get();

        //then
        assertThat(memberPhantomReadDTO.getFirstResult()).hasSize(1)
                .extracting("name", "level")
                .containsExactly(
                        tuple("user", MemberLevel.USER)
                );
        assertThat(memberPhantomReadDTO.getSecondResult()).hasSize(2)
                .extracting("name", "level")
                .containsExactly(
                        tuple("user", MemberLevel.USER),
                        tuple("admin", MemberLevel.ADMIN)
                );
    }

    @Test
    @DisplayName("Isolation=Repeatable_Read이면, Dirty Read가 발생하지 않는다.")
    public void test7() throws ExecutionException, InterruptedException {
        //given
        Member user = Member.builder()
                .name("user")
                .level(MemberLevel.USER)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        //when
        Future<?> submit = executorService.submit(() -> service.awaitAfterSave(user, latch));
        Future<List<Member>> submit1 = executorService.submit(
                () -> service.doRepeatableRead(
                        () -> service.findAll(latch)
                )
        );

        submit.get();
        List<Member> members = submit1.get();
        //then
        assertThat(members).isEmpty();
    }

    @Test
    @DisplayName("Isolation=Repeatable_Read이면, Non-Repeatable Read가 발생하지 않는다.")
    public void test8() throws ExecutionException, InterruptedException {
        //given
        Member user = Member.builder()
                .name("user")
                .level(MemberLevel.USER)
                .build();
        service.save(user);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        //when
        Future<List<Member>> submit = executorService.submit(
                () -> service.doRepeatableRead(
                        () -> service.nonRepeatableRead(user.getId(), latch)
                )
        );

        Future<?> submit1 = executorService.submit(() -> {
            service.updateName(user.getId(), "user33");
            latch.countDown();
        });
        submit1.get();
        List<Member> members = submit.get();
        //최종 커밋 이후 조회하면 변경되어있음.
        Member member = service.find(user.getId());

        //then
        assertThat(members).extracting("name")
                .containsExactly("user", "user");
        assertThat(member.getName()).isEqualTo("user33");
    }

    @Test
    @DisplayName("Isolation=Repeatable_Read이면, 일반적으로 Phantom Read가 발생하지 않는다.")
    public void test9() throws ExecutionException, InterruptedException {
        //given
        Member user = Member.builder()
                .name("user")
                .level(MemberLevel.USER)
                .build();
        Member admin = Member.builder()
                .name("admin")
                .level(MemberLevel.ADMIN)
                .build();

        service.save(user);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        //when
        Future<?> submit1 = executorService.submit(() -> service.awaitAfterSave(admin, latch));
        Future<MemberPhantomReadDTO> submit = executorService.submit(
                () -> service.doRepeatableRead(
                        () -> service.phantomRead(user.getId(), latch)
                )
        );

        submit1.get();
        MemberPhantomReadDTO memberPhantomReadDTO = submit.get();

        //then
        assertThat(memberPhantomReadDTO.getFirstResult()).hasSize(1)
                .extracting("name", "level")
                .containsExactly(
                        tuple("user", MemberLevel.USER)
                );
        assertThat(memberPhantomReadDTO.getSecondResult()).hasSize(1)
                .extracting("name", "level")
                .containsExactly(
                        tuple("user", MemberLevel.USER)
                );
    }

    @Test
    @DisplayName("Isolation=Repeatable_Read이면, 락을 획득할때 Phantom Read가 발생한다.")
    public void test10() throws ExecutionException, InterruptedException {
        //given
        Member user = Member.builder()
                .name("user")
                .level(MemberLevel.USER)
                .build();
        Member admin = Member.builder()
                .name("admin")
                .level(MemberLevel.ADMIN)
                .build();

        service.save(user);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        //when
        Future<MemberPhantomReadDTO> submit = executorService.submit(
                () -> service.doRepeatableRead(
                        () -> service.phantomReadForUpdate(latch)
                )
        );
        Future<?> submit1 = executorService.submit(() -> service.awaitBeforeSave(admin, latch));

        submit1.get();
        MemberPhantomReadDTO memberPhantomReadDTO = submit.get();

        //then
        assertThat(memberPhantomReadDTO.getFirstResult()).hasSize(1)
                .extracting("name", "level")
                .containsExactly(
                        tuple("user", MemberLevel.USER)
                );
        assertThat(memberPhantomReadDTO.getSecondResult()).hasSize(2)
                .extracting("name", "level")
                .containsExactly(
                        tuple("user", MemberLevel.USER),
                        tuple("admin", MemberLevel.ADMIN)
                );
    }
}
