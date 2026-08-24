package com.lxpantos.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class KiroDemoApplicationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsWithCleanArchitectureAdaptersWired() {
        Integer seededMembers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM members WHERE email LIKE 'member%@bsg-demo.local'",
                Integer.class
        );

        assertThat(seededMembers).isEqualTo(100);
    }
}
