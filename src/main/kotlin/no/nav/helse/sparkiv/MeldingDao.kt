package no.nav.helse.sparkiv

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource

interface MeldingRepository {
    fun lagreMelding(
        fødselsnummer: String,
        id: UUID,
        tidsstempel: ZonedDateTime,
        eventName: String,
        json: String,
    )

    fun lagreMangelfullMelding(
        partisjon: Int,
        offset: Long,
        json: String,
    )
}

class MeldingDao(
    private val dataSource: DataSource,
) : MeldingRepository {
    override fun lagreMelding(
        fødselsnummer: String,
        id: UUID,
        tidsstempel: ZonedDateTime,
        eventName: String,
        json: String,
    ) {
        @Language("PostgreSQL")
        val query = "INSERT INTO melding (id, fødselsnummer, tidsstempel, event_name, json) VALUES (:id, :fodselsnummer, :tidsstempel, :event_name, :json::jsonb) ON CONFLICT DO NOTHING"
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "id" to id,
                        "fodselsnummer" to fødselsnummer,
                        "tidsstempel" to tidsstempel,
                        "event_name" to eventName,
                        "json" to json,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun lagreMangelfullMelding(
        partisjon: Int,
        offset: Long,
        json: String,
    ) {
        @Language("PostgreSQL")
        val query = "INSERT INTO mangelfull_melding (partisjon, commit_offset, json) VALUES (:partisjon, :commit_offset, :json::jsonb)"
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "partisjon" to partisjon,
                        "commit_offset" to offset,
                        "json" to json,
                    ),
                ).asUpdate,
            )
        }
    }

    fun hentMeldinger(vedtaksperiodeId: UUID): List<String> {
        @Language("PostgreSQL")
        val query = "SELECT json FROM melding WHERE json ->> 'vedtaksperiodeId' = :vedtaksperiodeId ORDER BY tidsstempel"
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, mapOf("vedtaksperiodeId" to vedtaksperiodeId.toString()))
                    .map { row -> row.string("json") }
                    .asList,
            )
        }
    }

    fun hentMeldinger(fødselsnummer: String): List<String> {
        @Language("PostgreSQL")
        val query = "SELECT json FROM melding WHERE fødselsnummer = :fodselsnummer ORDER BY tidsstempel"
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, mapOf("fodselsnummer" to fødselsnummer))
                    .map { row -> row.string("json") }
                    .asList,
            )
        }
    }
}
