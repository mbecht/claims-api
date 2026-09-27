package com.mbecht.claims_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DatabaseMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesAllThreeMigrationsSuccessfully() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "SELECT version, success FROM flyway_schema_history WHERE version IN ('1', '2', '3') ORDER BY version");

        assertThat(history).hasSize(3);
        assertThat(history).allSatisfy(row -> assertThat(row.get("success")).isEqualTo(true));
    }

    @Test
    void allThreeTablesExist() {
        List<String> tableNames = jdbcTemplate.queryForList(
                """
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name IN ('users', 'policies', 'claims')
                """,
                String.class);

        assertThat(tableNames).containsExactlyInAnyOrder("users", "policies", "claims");
    }

    @Test
    void usersRoleCheckConstraintRejectsInvalidRole() {
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                        "invalid.role.user", "irrelevant-hash", "NOT_A_REAL_ROLE"));
    }

}
