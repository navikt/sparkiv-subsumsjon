package no.nav.helse.sparkiv

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random
import kotlin.test.assertEquals

class MeldingDaoTest {

    @Test
    fun `lagre melding`() {
        val fødselsnummer = "12345678910"
        val id = UUID.randomUUID()
        val tidsstempel = LocalDateTime.now().withNano(0)
        val eventName = "subsumsjon"

        @Language("JSON")
        val melding = """{"foo": "bar"}"""

        val dao = MeldingDao(database.dataSource)
        dao.lagreMelding(fødselsnummer, id, tidsstempel, eventName, melding)
        assertMeldingIDb(
            forventetId = id,
            forventetFødselsnummer = fødselsnummer,
            forventetEventName = eventName,
            forventetTidsstempel = tidsstempel,
            forventetJson = melding
        )
    }

    @Test
    fun `lagre mangelfull melding`() {
        @Language("JSON")
        val melding = """{"foo": "bar"}"""
        val partisjon = Random.nextInt()
        val offset = Random.nextLong()

        val dao = MeldingDao(database.dataSource)
        dao.lagreMangelfullMelding(partisjon, offset, melding)
        assertMangelfullMeldingIDb(
            forventetPartisjon = partisjon,
            forventetOffset = offset,
            forventetJson = melding
        )
    }

    private fun assertMeldingIDb(
        forventetId: UUID,
        forventetFødselsnummer: String,
        forventetEventName: String,
        forventetTidsstempel: LocalDateTime,
        forventetJson: String,
    ) {
        data class Result(
            val id: UUID,
            val fødselsnummer: String,
            val tidsstempel: LocalDateTime,
            val eventName: String,
            val json: String
        )
        @Language("PostgreSQL")
        val query = "SELECT fødselsnummer, id, tidsstempel, event_name, json FROM melding WHERE id = :id"
        val result = sessionOf(database.dataSource).use { session ->
            session.run(queryOf(query, mapOf("id" to forventetId)).map { row ->
                Result(
                    id = UUID.fromString(row.string("id")),
                    fødselsnummer = row.string("fødselsnummer"),
                    tidsstempel = row.localDateTime("tidsstempel"),
                    eventName = row.string("event_name"),
                    json = row.string("json")
                )
            }.asSingle)
        }
        assertNotNull(result)
        assertEquals(forventetId, result?.id)
        assertEquals(forventetFødselsnummer, result?.fødselsnummer)
        assertEquals(forventetTidsstempel, result?.tidsstempel)
        assertEquals(forventetEventName, result?.eventName)
        assertEquals(forventetJson, result?.json)
    }

    private fun assertMangelfullMeldingIDb(
        forventetPartisjon: Int,
        forventetOffset: Long,
        forventetJson: String,
    ) {
        data class Result(
            val partisjon: Int,
            val offset: Long,
            val json: String
        )
        @Language("PostgreSQL")
        val query = "SELECT partisjon, commit_offset, json FROM mangelfull_melding WHERE partisjon = :partisjon AND commit_offset = :commit_offset"
        val result = sessionOf(database.dataSource).use { session ->
            session.run(queryOf(query, mapOf("partisjon" to forventetPartisjon, "commit_offset" to forventetOffset)).map { row ->
                Result(
                    partisjon = row.int("partisjon"),
                    offset = row.long("commit_offset"),
                    json = row.string("json")
                )
            }.asList)
        }
        assertEquals(1, result.size)
        assertEquals(forventetPartisjon, result.single().partisjon)
        assertEquals(forventetOffset, result.single().offset)
        assertEquals(forventetJson, result.single().json    )
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
