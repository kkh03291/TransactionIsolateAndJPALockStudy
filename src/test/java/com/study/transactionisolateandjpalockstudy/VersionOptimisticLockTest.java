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
            Future<?> submit = executorService.submit(() -> postService.edit(post.getId(),
                    new PostEdit("test1", "test1"))
            );
            Future<?> submit1 = executorService.submit(() -> postService.edit(post.getId(),
                    new PostEdit("test2", "test2"))
            );
            submit.get();
            submit1.get();
        }).isInstanceOf(ExecutionException.class).hasCauseInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    /**
     * Member엔티티의 @Version을 주석처리하고 실행해야함.
     * 테스트 실행을 원활히 하기 위해 주석처리함.
     */
    /*
    @Test
    @DisplayName("LockMode=Optimistic 일때, 조회하는 엔티티중 하나라도 version이없으면 예외가 발생한다.")
    public void test2() {
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
        assertThatThrownBy(() -> postService.findWithOptimisticLock(post.getId()))
                .isInstanceOf(JpaSystemException.class);
    }

    @Test
    @DisplayName("LockMode=Optimistic 일때, version정보가 있는 엔티티만 조회한다.")
    public void test3() {
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

        //when
        Post findPost = postService.findOnlyPost(post.getId());

        //then
        assertThat(findPost)
                .extracting("id", "title", "content")
                .containsAnyOf(post.getId(), post.getTitle(), post.getContent());

        assertThatThrownBy(() -> findPost.getAuthor().getName())
                .isInstanceOf(LazyInitializationException.class);
    }
     */

    @Test
    @DisplayName("LockMode=Optimistic 일때, 조회하는 모든 엔티티는 version이 있어야한다.")
    public void test4() {
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

        //when
        Post findPost = postService.findWithOptimisticLock(post.getId());

        //then
        assertThat(findPost)
                .extracting("id", "title", "content")
                .containsAnyOf(post.getId(), post.getTitle(), post.getContent());
        assertThat(findPost.getAuthor())
                .extracting("id", "name", "level")
                .containsAnyOf(post.getAuthor().getId(), post.getAuthor().getName(), post.getAuthor().getLevel());

    }

    @Test
    @DisplayName("LockMode=OptimisticForceIncrementLock 일때, 조회만 해도 조회한 모든 엔티티의 version값이 증가한다.")
    public void test5() {
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

        //when
        //edit은 LockMode=Read로 post의 version만 증가.
        postService.edit(post.getId(), new PostEdit("test", "test"));
        Post findPost = postService.findWithOptimisticForceIncrementLock(post.getId());

        //then
        assertThat(findPost.getVersion()).isEqualTo(2L);
        assertThat(findPost.getAuthor().getVersion()).isEqualTo(1L);
    }
}
