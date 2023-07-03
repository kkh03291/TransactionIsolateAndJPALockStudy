package com.study.transactionisolateandjpalockstudy.repository;

import com.study.transactionisolateandjpalockstudy.domain.Member;
import com.study.transactionisolateandjpalockstudy.domain.MemberLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryWithJdbcTemplate {

    private final JdbcTemplate template;

    public List<Member> findAll() {
        String sql = "select * from member";
        List<Member> query = template.query(sql, listMemberRowMapper());
        return query;
    }

    public Long save(Member member) {
        String sql = "insert into member(name, level) values(?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(con -> {
            PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, member.getName());
            pst.setString(2, member.getLevel().name());
            return pst;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void removeAll() {
        String sql = "delete from member";
        template.update(sql);
    }

    private RowMapper<Member> listMemberRowMapper() {
        return (rs, rowNum) ->
                Member.builder().name(rs.getString("name"))
                        .level(MemberLevel.valueOf(rs.getString("level")))
                        .build();
    }
}
