package com.acme.hrassistant.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-shot local schema fix: older DBs had {@code storage_path} and no {@code content}.
 * Hibernate {@code ddl-auto=update} cannot add a NOT NULL column onto existing rows.
 */
@Component
@Order(50)
public class PolicySchemaMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicySchemaMigrator.class);

    private final JdbcTemplate jdbc;

    public PolicySchemaMigrator(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("policy_documents")) {
            return;
        }
        boolean hasContent = columnExists("policy_documents", "content");
        boolean hasStoragePath = columnExists("policy_documents", "storage_path");
        if (hasContent && !hasStoragePath) {
            ensureStatusAllowsDisabled();
            return;
        }

        log.warn("Migrating policy_documents schema (add content / drop storage_path)...");
        if (!hasContent) {
            jdbc.execute("ALTER TABLE policy_documents ADD COLUMN content TEXT");
        }
        if (hasStoragePath) {
            jdbc.execute("ALTER TABLE policy_documents DROP COLUMN storage_path");
        }
        // Existing rows have no body in DB — clear so PolicySeedRunner can re-seed.
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM policy_documents", Integer.class);
        if (count != null && count > 0) {
            jdbc.execute("TRUNCATE TABLE policy_documents");
            try {
                jdbc.execute("TRUNCATE TABLE vector_store");
            } catch (Exception ignored) {
                // vector_store may not exist yet
            }
            log.warn("Cleared {} legacy policy_documents row(s); seed will reload from classpath", count);
        }
        jdbc.execute("ALTER TABLE policy_documents ALTER COLUMN content SET NOT NULL");
        ensureStatusAllowsDisabled();
        log.info("policy_documents schema migration complete");
    }

    private void ensureStatusAllowsDisabled() {
        try {
            jdbc.execute("ALTER TABLE policy_documents DROP CONSTRAINT IF EXISTS policy_documents_status_check");
            jdbc.execute("""
                    ALTER TABLE policy_documents ADD CONSTRAINT policy_documents_status_check
                      CHECK (status::text = ANY (ARRAY['PENDING'::varchar, 'INDEXED'::varchar, 'FAILED'::varchar, 'DISABLED'::varchar]::text[]))
                    """);
        } catch (Exception ex) {
            log.debug("Could not refresh status check constraint: {}", ex.getMessage());
        }
    }

    private boolean tableExists(String table) {
        Integer n = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = ?
                """,
                Integer.class,
                table);
        return n != null && n > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer n = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """,
                Integer.class,
                table,
                column);
        return n != null && n > 0;
    }
}
