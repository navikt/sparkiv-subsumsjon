package no.nav.helse.sparkiv

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.flywaydb.core.Flyway
import java.time.Duration
import javax.sql.DataSource

internal class DataSourceBuilder(env: Map<String, String>) {
    private val dbUrl = requireNotNull(env["DATABASE_JDBC_URL"]) { "JDBC url må være satt" }

    private val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = dbUrl
            idleTimeout = Duration.ofMinutes(1).toMillis()
            maxLifetime = idleTimeout * 5
            initializationFailTimeout = Duration.ofMinutes(1).toMillis()
            connectionTimeout = Duration.ofSeconds(30).toMillis()
            minimumIdle = 1
            maximumPoolSize = 10
            metricRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, PrometheusRegistry.defaultRegistry, Clock.SYSTEM)
        }

    private val hikariMigrationConfig =
        HikariConfig().apply {
            jdbcUrl = dbUrl
            initializationFailTimeout = Duration.ofMinutes(1).toMillis()
            connectionTimeout = Duration.ofMinutes(1).toMillis()
            maximumPoolSize = 2
        }

    private fun runMigration(dataSource: DataSource) =
        Flyway.configure()
            .dataSource(dataSource)
            .lockRetryCount(-1)
            .load()
            .migrate()

    internal fun getDataSource(): HikariDataSource {
        return HikariDataSource(hikariConfig)
    }

    internal fun migrate() {
        HikariDataSource(hikariMigrationConfig).use { runMigration(it) }
    }
}
