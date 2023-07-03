package com.study.transactionisolateandjpalockstudy.domain;

import lombok.Data;

@Data
public class PostEdit {
    String title;
    String content;

    public PostEdit(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
