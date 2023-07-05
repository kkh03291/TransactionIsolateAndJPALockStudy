package com.study.transactionisolateandjpalockstudy.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class MemberPhantomReadDTO {

    private final List<Member> firstResult;
    private final List<Member> secondResult;
}
