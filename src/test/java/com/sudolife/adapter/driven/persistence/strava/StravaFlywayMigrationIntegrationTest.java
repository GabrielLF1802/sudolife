package com.sudolife.adapter.driven.persistence.strava;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class StravaFlywayMigrationIntegrationTest {

    @Test
    void flyway_creates_strava_persistence_tables() {
        DriverManagerDataSource dataSource = dataSource();
        Flyway flyway = Flyway.configure().dataSource(dataSource).cleanDisabled(false).load();

        MigrateResult result = flyway.migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        assertThat(result.migrationsExecuted).isGreaterThanOrEqualTo(2);
        assertThat(tableExists(jdbcTemplate, "STRAVA_ACCOUNT_LINKS")).isTrue();
        assertThat(tableExists(jdbcTemplate, "STRAVA_AUTHORIZATION_STATES")).isTrue();
    }

    private DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:h2:mem:strava_migration;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        return dataSource;
    }

    private boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_name = ?
                """, Integer.class, tableName);

        return count != null && count > 0;
    }
}
