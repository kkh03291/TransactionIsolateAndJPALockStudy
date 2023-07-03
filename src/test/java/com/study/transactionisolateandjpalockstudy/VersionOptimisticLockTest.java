package com.study.transactionisolateandjpalockstudy;

import com.study.transactionisolateandjpalockstudy.domain.*;
import com.study.transactionisolateandjpalockstudy.service.MemberService;
import com.study.transactionisolateandjpalockstudy.service.PostService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaSystemException;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class VersionOptimisticLockTest {

    @Autowired
    MemberService memberService;

    @Autowired
    PostService postService;


    @AfterEach
    void tearDown() {
        postService.removeAll();
        memberService.removeAll();
    }

    @Test
    @DisplayName("@Version 사용 시 Optimistic Lock 이 걸린다.")
    public void test1() {
        //given
        Member user = Member.builder()
                .name("user1")
                .level(MemberLevel.USER)
                .build();

        Member admin = Member.builder()
                .name("admin")
                .level(MemberLevel.ADMIN)
                .build();

        memberService.save (user);
        memberService.save(admin);

        Post post = Post.builder()
                .title("post1")
                .content("test")
                .author(user)
                .build();

        postService.write(post);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        //expect
        assertThatThrownBy(() -> {
                    try {
                        Future<?> submit = executorService.submit(() -> postService.edit(post.getId(),
                                new PostEdit("test1", "test1"))
                        );
                        Future<?> submit1 = executorService.submit(() -> postService.edit(post.getId(),
                                new PostEdit("test2", "test2"))
                        );
                        submit.get();
                        submit1.get();
                    } catch (ExecutionException e) {
                        throw e.getCause();
                    }
        }).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("LockMode=Optimistic 일때, 조회하는 엔티티중 하나라도 version이없으면 예외가 발생한다.")
    public void Non() {
        //given
        Member user = Member.builder()
                .name("user1")
                .level(MemberLevel.USER)
                .build();

        Member admin = Member.builder()
                .name("admin")
                .level(MemberLevel.ADMIN)
                .build();

        memberService.save (user);
        memberService.save(admin);

        Post post = Post.builder()
                .title("post1")
                .content("test")
                .author(user)
                .build();

        postService.write(post);

        //expect
        assertThatThrownBy(() -> {
            postService.findWithOptimisticLock(post.getId());
        }).isInstanceOf(JpaSystemException.class);

    }
}
