package no.nav.helse.sparkiv

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.assertEquals

class MeldingDaoTest {

    @Test
    fun `lagre melding`() {
        val fødselsnummer = "foobar"
        @Language("JSON")
        val melding = """{"fodselsnummer": "$fødselsnummer"}"""
        val dao = MeldingDao(database.dataSource)
        dao.lagreMelding(fødselsnummer, melding)
        assertInnholdIDb(melding, fødselsnummer)
    }

    private fun assertInnholdIDb(forventetInnhold: String, fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "SELECT json FROM melding WHERE fødselsnummer = :fodselsnummer"
        val json = sessionOf(database.dataSource).use { session ->
            session.run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer)).map { row -> row.string("json") }.asSingle)
        }
        assertEquals(forventetInnhold, json)
    }

    private val database =
        object {
            private val postgres =
                PostgreSQLContainer<Nothing>("postgres:17").apply {
                    withReuse(true)
                    withLabel("app-navn", "sparkiv-subsumsjon")
                    start()
                }

            val dataSource =
                HikariDataSource(HikariConfig().apply {
                    jdbcUrl = postgres.jdbcUrl
                    username = postgres.username
                    password = postgres.password
                    maximumPoolSize = 5
                    connectionTimeout = 500
                    initializationFailTimeout = 5000
                })

            init {
                Flyway.configure()
                    .dataSource(dataSource)
                    .load()
                    .migrate()
            }
        }

}
