package no.nav.helse.sparkiv

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

interface MeldingRepository {
    fun lagreMelding(fødselsnummer: String, id: UUID, tidsstempel: LocalDateTime, eventName: String, json: String)
    fun lagreMangelfullMelding(partisjon: Int, offset: Long, json: String)
}

class MeldingDao(
    private val dataSource: DataSource
): MeldingRepository {
    override fun lagreMelding(
        fødselsnummer: String,
        id: UUID,
        tidsstempel: LocalDateTime,
        eventName: String,
        json: String
    ) {
        @Language("PostgreSQL")
        val query = "INSERT INTO melding (id, fødselsnummer, tidsstempel, event_name, json) VALUES (:id, :fodselsnummer, :tidsstempel, :event_name, :json::jsonb) ON CONFLICT DO NOTHING"
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, mapOf(
                    "id" to id,
                    "fodselsnummer" to fødselsnummer,
                    "tidsstempel" to tidsstempel,
                    "event_name" to eventName,
                    "json" to json
                )).asUpdate
            )
        }
    }

    override fun lagreMangelfullMelding(partisjon: Int, offset: Long, json: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO mangelfull_melding (partisjon, commit_offset, json) VALUES (:partisjon, :commit_offset, :json::jsonb)"
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, mapOf(
                    "partisjon" to partisjon,
                    "commit_offset" to offset,
                    "json" to json
                )).asUpdate
            )
        }
    }
}
