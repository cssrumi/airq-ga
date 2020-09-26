package pl.airq.ga.integration;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import pl.airq.common.domain.DataProvider;
import pl.airq.common.domain.enriched.AirqDataEnrichedEvent;
import pl.airq.common.domain.enriched.AirqDataEnrichedPayload;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.domain.enriched.EnrichedDataQuery;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.domain.phenotype.AirqPhenotypeCreatedEvent;
import pl.airq.common.domain.phenotype.AirqPhenotypeQuery;
import pl.airq.common.domain.station.Station;
import pl.airq.common.domain.station.StationQuery;
import pl.airq.common.process.EventParser;
import pl.airq.common.process.event.AirqEvent;
import pl.airq.common.vo.StationId;
import pl.airq.common.vo.StationLocation;
import pl.airq.ga.domain.phenotype.MockAirqPhenotypeRepositoryPostgres;
import pl.airq.ga.domain.phenotype.Pm10PhenotypeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTestResource(KafkaResource.class)
@QuarkusTest
public class IntegrationTest {

    @ConfigProperty(name = "mp.messaging.incoming.data-enriched.topic")
    private String dataEnrichedTopic;
    @ConfigProperty(name = "ga.prediction.timeFrame")
    private Long timeFrame;
    @ConfigProperty(name = "ga.prediction.timeUnit")
    private ChronoUnit timeUnit;

    @InjectMock
    private AirqPhenotypeQuery airqPhenotypeQuery;
    @InjectMock
    private StationQuery stationQuery;
    @InjectMock
    private EnrichedDataQuery enrichedDataQuery;

    @InjectSpy
    private MockAirqPhenotypeRepositoryPostgres repository;

    @Inject
    private EventParser parser;
    @Inject
    private KafkaConsumer<Void, String> kafkaConsumer;
    @Inject
    private KafkaProducer<Void, String> kafkaProducer;

    @BeforeEach
    void beforeEach() {
        repository.setSaveAndUpsertResult(Boolean.TRUE);
        reset(repository, airqPhenotypeQuery, stationQuery, enrichedDataQuery);
        when(airqPhenotypeQuery.findByStationId(any())).thenReturn(Uni.createFrom().nullItem());
        when(stationQuery.findById(any())).thenReturn(Uni.createFrom().nullItem());
        when(enrichedDataQuery.findAllByStationId(any())).thenReturn(Uni.createFrom().nullItem());
    }

    @AfterAll
    void clear() {
        kafkaProducer.close();
    }

    @Test
    void integration_withoutPhenotypeInDB_expectNewPhenotypeSavedAndEventPublished() {
        StationId stationId = stationId();
        final AirqDataEnrichedEvent event = airqDataEnrichedEvent(stationId);
        final List<EnrichedData> enrichedDataList = generateEnrichedDataSinceLast2Days(stationId);
        when(enrichedDataQuery.findAllByStationId(any())).thenReturn(Uni.createFrom().item(new HashSet<>(enrichedDataList)));
        when(stationQuery.findById(any())).thenReturn(Uni.createFrom().item(new Station(stationId, StationLocation.EMPTY)));

        sendEvent(event);

        final ConsumerRecord<Void, String> record = kafkaConsumer.poll(Duration.ofMinutes(1)).iterator().next();
        final AirqEvent<?> airqEvent = parser.deserializeDomainEvent(record.value());

        assertTrue(airqEvent instanceof AirqPhenotypeCreatedEvent);
        final AirqPhenotype result = ((AirqPhenotypeCreatedEvent) airqEvent).payload.airqPhenotype;
        assertNotNull(result);
        assertTrue(result.fields.stream().noneMatch(Objects::isNull));
        assertEquals(Pm10PhenotypeMap.DEFAULT_ENRICHED_DATA_FIELDS, result.fields);
        assertEquals(result.values.size(), result.fields.size());
        assertTrue(result.values.stream().noneMatch(Objects::isNull));
        assertEquals(stationId, result.stationId);
        assertNotNull(result.timestamp);
        assertTrue(result.fitness > 0);
        assertNotNull(result.prediction);
        assertEquals(timeFrame, result.prediction.timeframe);
        assertEquals(timeUnit, result.prediction.timeUnit);
        assertEquals(Pm10PhenotypeMap.FIELD, result.prediction.field);
        verify(stationQuery, timeout(Duration.ofSeconds(2).toMillis())).findById(any());
        verify(enrichedDataQuery, timeout(Duration.ofSeconds(2).toMillis())).findAllByStationId(any());
        verify(airqPhenotypeQuery, timeout(Duration.ofSeconds(2).toMillis())).findByStationId(any());
        verify(repository, timeout(Duration.ofSeconds(2).toMillis())).save(any());
    }

    @Test
    void integration_withoutPhenotypeInDBAndEnrichedData_expectEmptyComputationResultAndEventNotPublished() {
        StationId stationId = stationId();
        final AirqDataEnrichedEvent event = airqDataEnrichedEvent(stationId);
        when(stationQuery.findById(any())).thenReturn(Uni.createFrom().item(new Station(stationId, StationLocation.EMPTY)));

        sendEvent(event);

        verify(enrichedDataQuery, timeout(Duration.ofSeconds(5).toMillis())).findAllByStationId(any());
        verify(stationQuery, timeout(Duration.ofSeconds(2).toMillis())).findById(any());
        verifyNoInteractions(airqPhenotypeQuery);
        verifyNoInteractions(repository);
        verifyThatNoEventSent(Duration.ofSeconds(3));
    }

    @Test
    void integration_withoutStationNotFound_expectEmptyComputationResultAndEventNotPublished() {
        StationId stationId = stationId();
        final AirqDataEnrichedEvent event = airqDataEnrichedEvent(stationId);

        sendEvent(event);

        verify(stationQuery, timeout(Duration.ofSeconds(5).toMillis())).findById(any());
        verifyNoInteractions(enrichedDataQuery);
        verifyNoInteractions(airqPhenotypeQuery);
        verifyNoInteractions(repository);
        verifyThatNoEventSent(Duration.ofSeconds(3));
    }

    private void verifyThatNoEventSent(Duration awaitDuration) {
        final ConsumerRecord<Void, String> record;
        try {
            record = kafkaConsumer.poll(awaitDuration).iterator().next();
        } catch (NoSuchElementException e) {
            return;
        }

        fail("Record has been found: " + record.value());
    }

    private StationId stationId() {
        return StationId.from(RandomStringUtils.randomAlphabetic(5));
    }

    private List<EnrichedData> generateEnrichedDataSinceLast2Days(StationId stationId) {
        OffsetDateTime current = OffsetDateTime.now();
        List<EnrichedData> enrichedDataList = new ArrayList<>();
        for (int i = 0; i < 24 * 2; i++) {
            current = current.minusHours(1);
            enrichedDataList.add(enrichedData(current, stationId));
        }

        return enrichedDataList;
    }

    private EnrichedData enrichedData(OffsetDateTime dateTime, StationId stationId) {
        return new EnrichedData(
                dateTime,
                RandomUtils.nextFloat(0, 1000),
                RandomUtils.nextFloat(0, 1000),
                RandomUtils.nextFloat(0, 70) - 30,
                RandomUtils.nextFloat(0, 150),
                RandomUtils.nextFloat(0, 360),
                RandomUtils.nextFloat(0, 100),
                RandomUtils.nextFloat(980, 1030),
                1.0f,
                1.0f,
                DataProvider.GIOS,
                stationId
        );
    }

    private AirqDataEnrichedEvent airqDataEnrichedEvent(StationId stationId) {
        EnrichedData data = enrichedData(OffsetDateTime.now(), stationId);
        AirqDataEnrichedPayload payload = new AirqDataEnrichedPayload(data);

        return new AirqDataEnrichedEvent(OffsetDateTime.now(), payload);
    }

    private void sendEvent(AirqEvent<?> airqEvent) {
        final String rawEvent = parser.parse(airqEvent);
        String topic = getTopic(airqEvent);
        final Future<RecordMetadata> future = kafkaProducer.send(new ProducerRecord<>(topic, rawEvent));
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private String getTopic(AirqEvent<?> airqEvent) {
        if (airqEvent.eventType().equals(AirqDataEnrichedEvent.class.getSimpleName())) {
            return dataEnrichedTopic;
        }

        throw new RuntimeException("Invalid event: " + airqEvent.eventType());
    }

    @Dependent
    static class KafkaConfiguration {

        @Produces
        KafkaConsumer<Void, String> kafkaConsumer(@ConfigProperty(name = "kafka.bootstrap.servers") String bootstrapServers,
                                                  @ConfigProperty(name = "mp.messaging.outgoing.phenotype-created.topic") String topic) {
            Properties properties = new Properties();
            properties.put("bootstrap.servers", bootstrapServers);
            properties.put("enable.auto.commit", "true");
            properties.put("group.id", "airq-ga-int-test");
            properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            properties.put("auto.offset.reset", "earliest");

            KafkaConsumer<Void, String> consumer = new KafkaConsumer<>(properties);
            consumer.subscribe(Collections.singleton(topic));
            return consumer;
        }

        @Produces
        KafkaProducer<Void, String> stringKafkaProducer(@ConfigProperty(name = "kafka.bootstrap.servers") String bootstrapServers) {
            Properties properties = new Properties();
            properties.put("bootstrap.servers", bootstrapServers);
            properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            return new KafkaProducer<>(properties);
        }

    }

}
