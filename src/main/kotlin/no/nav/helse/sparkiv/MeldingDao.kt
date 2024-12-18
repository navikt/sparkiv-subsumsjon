package no.nav.helse.sparkiv

import javax.sql.DataSource

interface MeldingRepository {
    fun lagreMelding(fødselsnummer: String, json: String)
}

class MeldingDao(
    private val dataSource: DataSource
): MeldingRepository {
    override fun lagreMelding(fødselsnummer: String, json: String) {

    }
}
