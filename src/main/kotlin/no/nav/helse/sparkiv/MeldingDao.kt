package no.nav.helse.sparkiv

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

interface MeldingRepository {
    fun lagreMelding(fødselsnummer: String, json: String)
}

class MeldingDao(
    private val dataSource: DataSource
): MeldingRepository {
    override fun lagreMelding(fødselsnummer: String, json: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO melding (fødselsnummer, json) VALUES (:fodselsnummer, :json::jsonb)"
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, mapOf("fodselsnummer" to fødselsnummer, "json" to json)).asUpdate
            )
        }
    }
}
