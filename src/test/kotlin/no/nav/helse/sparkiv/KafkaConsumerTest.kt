package no.nav.helse.sparkiv

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerRecord
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class KafkaConsumerTest {
    private val kafka = ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1")).apply {
        withReuse(true)
        start()
    }
    private val kafkaConfig = LocalKafkaConfig(
        mapOf(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers).toProperties()
    )
    private val topic = "topic-1"
    private val factory = ConsumerProducerFactory(kafkaConfig)

    private val consumer = KafkaConsumer("some-id", topic, Properties(), factory)

    private val repo = object : MeldingRepository {
        val meldinger = mutableMapOf<String, String>()
        val mangelfulleMeldinger = mutableListOf<String>()
        override fun lagreMelding(fødselsnummer: String, id: UUID, tidsstempel: LocalDateTime, eventName: String, json: String) {
            meldinger[fødselsnummer] = json
            consumer.stop()
        }

        override fun lagreMangelfullMelding(partisjon: Int, offset: Long, json: String) {
            mangelfulleMeldinger.add(json)
            consumer.stop()
        }

        fun clear() {
            meldinger.clear()
            mangelfulleMeldinger.clear()
        }
    }

    @BeforeEach
    fun beforeEach() {
        repo.clear()
    }

    @Test
    fun `lagrer komplette meldinger fra kafka`() {
        runBlocking {
            launch {
                consumer.consume(repo)
            }
            factory.createProducer().use { producer ->
                producer.send(ProducerRecord(topic, komplettJson))
            }
        }

        assertEquals(1, repo.meldinger.size)
    }

    @Test
    fun `lagrer melding uten id fra kafka`() {
        runBlocking {
            launch {
                consumer.consume(repo)
            }
            factory.createProducer().use { producer ->
                producer.send(ProducerRecord(topic, jsonUtenId))
            }
        }

        assertEquals(1, repo.mangelfulleMeldinger.size)
    }

    @Test
    fun `lagrer melding uten fødselsnummer fra kafka`() {
        runBlocking {
            launch {
                consumer.consume(repo)
            }
            factory.createProducer().use { producer ->
                producer.send(ProducerRecord(topic, jsonUtenFødselsnummer))
            }
        }

        assertEquals(1, repo.mangelfulleMeldinger.size)
    }

    @Test
    fun `lagrer melding uten tidsstempel fra kafka`() {
        runBlocking {
            launch {
                consumer.consume(repo)
            }
            factory.createProducer().use { producer ->
                producer.send(ProducerRecord(topic, jsonUtenTidsstempel))
            }
        }

        assertEquals(1, repo.mangelfulleMeldinger.size)
    }

    @Test
    fun `lagrer melding uten eventName fra kafka`() {
        runBlocking {
            launch {
                consumer.consume(repo)
            }
            factory.createProducer().use { producer ->
                producer.send(ProducerRecord(topic, jsonUtenEventName))
            }
        }

        assertEquals(1, repo.mangelfulleMeldinger.size)
    }

    @Language("JSON")
    private val komplettJson = """{
        "id": "${UUID.randomUUID()}",
        "fodselsnummer": "foobar",
        "tidsstempel": "${LocalDateTime.now()}",
        "eventName": "subsumsjon"
    }"""

    @Language("JSON")
    private val jsonUtenId = """{
        "fodselsnummer": "foobar",
        "tidsstempel": "${LocalDateTime.now()}",
        "eventName": "subsumsjon"
    }"""

    @Language("JSON")
    private val jsonUtenFødselsnummer = """{
        "id": "${UUID.randomUUID()}",
        "tidsstempel": "${LocalDateTime.now()}",
        "eventName": "subsumsjon"
    }"""

    @Language("JSON")
    private val jsonUtenTidsstempel = """{
        "id": "${UUID.randomUUID()}",
        "fodselsnummer": "foobar",
        "eventName": "subsumsjon"
    }"""

    @Language("JSON")
    private val jsonUtenEventName = """{
        "id": "${UUID.randomUUID()}",
        "fodselsnummer": "foobar",
        "tidsstempel": "${LocalDateTime.now()}"
    }"""
}
