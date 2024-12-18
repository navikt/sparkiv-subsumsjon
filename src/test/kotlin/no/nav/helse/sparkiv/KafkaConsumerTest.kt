package no.nav.helse.sparkiv

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
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
            override fun lagreMelding(fødselsnummer: String, json: String) {
                meldinger[fødselsnummer] = json
                consumer.stop()
            }
        }
        runBlocking {
            launch {
                consumer.consume(repo)
            }
            factory.createProducer().use { producer ->
                producer.send(ProducerRecord(topic, """{"fodselsnummer": "foobar"}"""))
            }
        }

        assertEquals(1, repo.meldinger.size)
    }
}
