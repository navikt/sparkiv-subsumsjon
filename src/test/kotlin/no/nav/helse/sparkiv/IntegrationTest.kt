package no.nav.helse.sparkiv

import com.github.navikt.tbd_libs.kafka.Config
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import kotlin.time.Duration.Companion.seconds

open class IntegrationTest {
    private val kafka = ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1")).apply {
        withReuse(true)
        start()
    }
    private val kafkaConfig = LocalKafkaConfig(
        mapOf(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers).toProperties()
    )
    private val factory = ConsumerProducerFactory(kafkaConfig)

    private val scope = CoroutineScope(Dispatchers.IO)

    @AfterEach
    fun tearDown() {
        stoppAppen()
    }

    @Test
    fun integrationTest() {
        val topic = "topic.v1"
        runBlocking(scope.coroutineContext) {
            scope.launch {
                logger.info("starting app")
                app(env = database.envvars + mapOf("KAFKA_TOPIC" to topic), kafkaConfig = kafkaConfig)
            }
            delay(1.seconds)
            factory.createProducer().use {
                val randomUUID = UUID.randomUUID()
                logger.info("Producing message with id=${randomUUID}")
                it.send(ProducerRecord(topic, """{"fodselsnummer": "$randomUUID"}"""))
            }
            delay(1.seconds)
            logger.info("asserting")
            assertInnholdIDb()
            logger.info("asserted")
        }
        logger.info("runBlocking exited")
    }

    private fun assertInnholdIDb() {
        @Language("PostgreSQL")
        val query = "SELECT true FROM melding WHERE fødselsnummer = :fodselsnummer"
        val exists = sessionOf(database.dataSource).use { session ->
            session.run(queryOf(query, mapOf("fodselsnummer" to "")).map { row -> row.boolean(1) }.asSingle)
        }
        assertEquals(true, exists)
    }

    private val database =
        object {
            private val postgres =
                PostgreSQLContainer<Nothing>("postgres:14").apply {
                    withReuse(true)
                    start()

                    println("Database localapp: jdbc:postgresql://localhost:$firstMappedPort/test startet opp, credentials: test og test")
                }

            val envvars =
                mapOf(
                    "DATABASE_HOST" to "localhost",
                    "DATABASE_PORT" to "${postgres.firstMappedPort}",
                    "DATABASE_DATABASE" to "test",
                    "DATABASE_USERNAME" to "test",
                    "DATABASE_PASSWORD" to "test",
                )

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

    private fun stoppAppen() {
        val client = HttpClient.newHttpClient()
        logger.info("Calling /stop")
        client.send(
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:8080/stop"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
    }

    private class LocalKafkaConfig(private val connectionProperties: Properties) : Config {
        override fun producerConfig(properties: Properties) = properties.apply {
            putAll(connectionProperties)
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
            put(ProducerConfig.LINGER_MS_CONFIG, "0")
            put(ProducerConfig.RETRIES_CONFIG, "0")
        }

        override fun consumerConfig(groupId: String, properties: Properties) = properties.apply {
            putAll(connectionProperties)
            put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }

        override fun adminConfig(properties: Properties) = properties.apply {
            putAll(connectionProperties)
        }
    }
}
