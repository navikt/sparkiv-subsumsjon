package no.nav.helse.sparkiv

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerRecord
import org.intellij.lang.annotations.Language
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

    @Test
    fun `lagrer meldinger fra kafka`() {
        val consumer = KafkaConsumer("some-id", topic, Properties(), factory)

        val repo = object : MeldingRepository {
            val meldinger = mutableMapOf<String, String>()
            override fun lagreMelding(fødselsnummer: String, id: UUID, tidsstempel: LocalDateTime, eventName: String, json: String) {
                meldinger[fødselsnummer] = json
                consumer.stop()
            }
        }
        runBlocking {
            launch {
                consumer.consume(repo)
            }
            factory.createProducer().use { producer ->
                producer.send(ProducerRecord(topic, json))
            }
        }

        assertEquals(1, repo.meldinger.size)
    }

    @Language("JSON")
    private val json = """{
        "id": "${UUID.randomUUID()}",
        "fodselsnummer": "foobar",
        "tidsstempel": "${LocalDateTime.now()}",
        "event_name": "subsumsjon"
    }"""
}
