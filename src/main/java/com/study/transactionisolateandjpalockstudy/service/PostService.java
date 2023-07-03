package com.study.transactionisolateandjpalockstudy.service;

import com.study.transactionisolateandjpalockstudy.domain.Post;
import com.study.transactionisolateandjpalockstudy.domain.PostEdit;
import com.study.transactionisolateandjpalockstudy.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository repository;

    public Post find(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public Post write(Post post) {
        return repository.save(post);
    }

    @Transactional
    public void edit(Long id, PostEdit edit) {
        find(id).edit(edit);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void remove(Long id) {
        repository.delete(find(id));
    }

    @Transactional
    public void removeAll() {
        repository.deleteAllInBatch();
    }

    public Post findWithOptimisticLock(Long id) {
        return repository.findByIdWithOptimisticLock(id).orElseThrow();
    }
}
