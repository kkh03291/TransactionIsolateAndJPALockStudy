package com.study.transactionisolateandjpalockstudy.service;

import com.study.transactionisolateandjpalockstudy.domain.Member;
import com.study.transactionisolateandjpalockstudy.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
