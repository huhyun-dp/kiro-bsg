package com.lxpantos.auth.config;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InquirySchemaMigrationTest {
    private final DriverManagerDataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:migration_" + UUID.randomUUID()
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    private final JdbcTemplate jdbc = new JdbcTemplate(dataSource);

    @Test
    void createsInquirySchemaAndCanRunAgain() throws Exception {
        migrate();
        insertMember();
        insertInquiry();
        migrate();

        assertThat(jdbc.queryForObject("SELECT view_count FROM inquiries", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT title FROM inquiries", String.class)).isEqualTo("Existing inquiry");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO inquiries (member_id, title, content, created_at) "
                + "VALUES (999, 'Invalid', 'Content', CURRENT_TIMESTAMP)"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void adoptsExistingInquiryTableWithoutLosingData() throws Exception {
        jdbc.execute("CREATE TABLE members (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE inquiries (id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "member_id BIGINT NOT NULL, title VARCHAR(20) NOT NULL, content TEXT NOT NULL, "
                + "view_count BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, "
                + "CONSTRAINT fk_inquiries_member FOREIGN KEY (member_id) REFERENCES members(id))");
        jdbc.update("INSERT INTO members (id) VALUES (1)");
        insertInquiry();

        migrate();
        migrate();

        assertThat(jdbc.queryForObject("SELECT title FROM inquiries WHERE id = 1", String.class))
                .isEqualTo("Existing inquiry");
        assertThat(jdbc.queryForObject("SELECT exectype FROM databasechangelog WHERE id = '8-create-inquiries'",
                String.class)).isEqualTo("MARK_RAN");
    }

    private void migrate() throws Exception {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
        liquibase.afterPropertiesSet();
    }

    private void insertMember() {
        jdbc.update("INSERT INTO members (id, email, password_hash, name, created_at) "
                + "VALUES (1, 'migration@example.com', 'hash', 'Member', CURRENT_TIMESTAMP)");
    }

    private void insertInquiry() {
        jdbc.update("INSERT INTO inquiries (member_id, title, content, created_at) "
                + "VALUES (1, 'Existing inquiry', 'Preserved content', CURRENT_TIMESTAMP)");
    }
}
