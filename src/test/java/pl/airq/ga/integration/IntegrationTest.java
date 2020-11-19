package pl.airq.ga.integration;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import pl.airq.common.domain.DataProvider;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.domain.enriched.EnrichedDataQuery;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.domain.phenotype.AirqPhenotypeQuery;
import pl.airq.common.domain.station.Station;
import pl.airq.common.domain.station.StationQuery;
import pl.airq.common.kafka.AirqEventDeserializer;
import pl.airq.common.kafka.AirqEventSerializer;
import pl.airq.common.kafka.SKeyDeserializer;
import pl.airq.common.kafka.TSKeySerializer;
import pl.airq.common.process.EventParser;
import pl.airq.common.process.ctx.enriched.EnrichedDataCreatedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataDeletedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataEventPayload;
import pl.airq.common.process.ctx.enriched.EnrichedDataUpdatedEvent;
import pl.airq.common.process.ctx.phenotype.AirqPhenotypeCreatedEvent;
import pl.airq.common.process.ctx.phenotype.AirqPhenotypeCreatedPayload;
import pl.airq.common.process.event.AirqEvent;
import pl.airq.common.store.key.SKey;
import pl.airq.common.store.key.TSKey;
import pl.airq.common.vo.StationId;
import pl.airq.common.vo.StationLocation;
import pl.airq.ga.domain.phenotype.Pm10PhenotypeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pl.airq.ga.integration.DBConstant.CREATE_AIRQ_PHENOTYPE_TABLE;
import static pl.airq.ga.integration.DBConstant.DROP_AIRQ_PHENOTYPE_TABLE;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTestResource(PostgresResource.class)
@QuarkusTestResource(KafkaResource.class)
@QuarkusTest
public class IntegrationTest {

    private final Map<SKey, AirqEvent<AirqPhenotypeCreatedPayload>> eventsMap = new ConcurrentHashMap<>();
    private final List<AirqEvent<AirqPhenotypeCreatedPayload>> eventsList = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean shouldConsume = new AtomicBoolean(true);

    @InjectMock
    StationQuery stationQuery;
    @InjectMock
    EnrichedDataQuery enrichedDataQuery;

    @InjectSpy
    AirqPhenotypeQuery airqPhenotypeQuery;

    @Inject
    PgPool client;
    @Inject
    KafkaProducer<TSKey, AirqEvent<EnrichedDataEventPayload>> producer;
    @Inject
    KafkaConsumer<SKey, AirqEvent<AirqPhenotypeCreatedPayload>> consumer;

    @ConfigProperty(name = "mp.messaging.incoming.data-enriched.topic")
    String enrichedDataTopic;
    @ConfigProperty(name = "mp.messaging.outgoing.phenotype-created.topic")
    String phenotypeCreatedTopic;
    @ConfigProperty(name = "ga.prediction.time-frame")
    Long timeFrame;
    @ConfigProperty(name = "ga.prediction.time-unit")
    ChronoUnit timeUnit;

    @BeforeAll
    void startConsuming() {
        executor.submit(() -> {
            while (shouldConsume.get()) {
                consumer.poll(Duration.ofMillis(100))
                        .records(phenotypeCreatedTopic)
                        .forEach(record -> {
                            eventsMap.put(record.key(), record.value());
                            eventsList.add(record.value());
                        });
            }
        });
    }

    @AfterAll
    void stopConsuming() {
        shouldConsume.set(false);
        executor.shutdown();
    }


    @BeforeEach
    void beforeEach() {
        recreateAirqPhenotypeTable();
        eventsMap.clear();
        eventsList.clear();

        when(stationQuery.findById(any())).thenReturn(Uni.createFrom().nullItem());
        when(enrichedDataQuery.findAllByStationId(any())).thenReturn(Uni.createFrom().nullItem());
    }

    @Test
    void EnrichedDataCreatedEventArrived_withoutPhenotypeInDB_expectNewPhenotypeSavedAndEventPublished() {
        StationId stationId = stationId();
        List<EnrichedData> enrichedDataList = generateEnrichedDataSinceLast2Days(stationId);
        EnrichedDataEventPayload createdPayload = new EnrichedDataEventPayload(enrichedDataList.get(0));
        EnrichedDataCreatedEvent createdEvent = new EnrichedDataCreatedEvent(OffsetDateTime.now(), createdPayload);

        when(enrichedDataQuery.findAllByStationId(any())).thenReturn(Uni.createFrom().item(new HashSet<>(enrichedDataList)));
        when(stationQuery.findById(any())).thenReturn(Uni.createFrom().item(new Station(stationId, StationLocation.EMPTY)));

        TSKey createdKey = sendEvent(createdEvent);
        AirqEvent<AirqPhenotypeCreatedPayload> receivedEvent = awaitForEvent(createdKey);

        verifyAirqPhenotypeCount(1);
        verifyAirqPhenotypeCreatedEvent(receivedEvent, stationId);
    }

    @Test
    void EnrichedDataUpdatedEventArrived_withoutPhenotypeInDB_expectNewPhenotypeSavedAndEventPublished() {
        StationId stationId = stationId();
        List<EnrichedData> enrichedDataList = generateEnrichedDataSinceLast2Days(stationId);
        EnrichedDataEventPayload updatedPayload = new EnrichedDataEventPayload(enrichedDataList.get(0));
        EnrichedDataUpdatedEvent updatedEvent = new EnrichedDataUpdatedEvent(OffsetDateTime.now(), updatedPayload);

        when(enrichedDataQuery.findAllByStationId(any())).thenReturn(Uni.createFrom().item(new HashSet<>(enrichedDataList)));
        when(stationQuery.findById(any())).thenReturn(Uni.createFrom().item(new Station(stationId, StationLocation.EMPTY)));

        TSKey updatedKey = sendEvent(updatedEvent);
        AirqEvent<AirqPhenotypeCreatedPayload> receivedEvent = awaitForEvent(updatedKey);

        verifyAirqPhenotypeCount(1);
        verifyAirqPhenotypeCreatedEvent(receivedEvent, stationId);
    }

    @Test
    void EnrichedDataCreatedEventArrived_withoutPhenotypeInDBAndEnrichedData_expectEmptyComputationResultAndEventNotPublished() {
        StationId stationId = stationId();
        EnrichedData enrichedData = enrichedData(OffsetDateTime.now(), stationId);
        EnrichedDataEventPayload createdPayload = new EnrichedDataEventPayload(enrichedData);
        EnrichedDataCreatedEvent createdEvent = new EnrichedDataCreatedEvent(OffsetDateTime.now(), createdPayload);

        when(enrichedDataQuery.findAllByStationId(any())).thenReturn(Uni.createFrom().item(new HashSet<>()));
        when(stationQuery.findById(any())).thenReturn(Uni.createFrom().item(new Station(stationId, StationLocation.EMPTY)));

        sendEvent(createdEvent);

        verify(enrichedDataQuery, timeout(Duration.ofSeconds(5).toMillis())).findAllByStationId(any());
        verify(stationQuery, timeout(Duration.ofSeconds(2).toMillis())).findById(any());
        sleep(Duration.ofSeconds(2));
        verifyNoInteractions(airqPhenotypeQuery);
        assertThat(eventsList).isEmpty();
        verifyAirqPhenotypeCount(0);
    }

    @Test
    void EnrichedDataCreatedEventArrived_withStationNotFound_expectEmptyComputationResultAndEventNotPublished() {
        StationId stationId = stationId();
        EnrichedData enrichedData = enrichedData(OffsetDateTime.now(), stationId);
        EnrichedDataEventPayload createdPayload = new EnrichedDataEventPayload(enrichedData);
        EnrichedDataCreatedEvent createdEvent = new EnrichedDataCreatedEvent(OffsetDateTime.now(), createdPayload);

        sendEvent(createdEvent);

        verify(stationQuery, timeout(Duration.ofSeconds(5).toMillis())).findById(any());
        verifyNoInteractions(enrichedDataQuery);
        verifyNoInteractions(airqPhenotypeQuery);
        assertThat(eventsList).isEmpty();
        verifyAirqPhenotypeCount(0);
    }

    @Test
    void EnrichedDataDeletedEventArrived_withStationNotFound_expectEmptyComputationResultAndEventNotPublished() {
        StationId stationId = stationId();
        EnrichedData enrichedData = enrichedData(OffsetDateTime.now(), stationId);
        EnrichedDataEventPayload deletedPayload = new EnrichedDataEventPayload(enrichedData);
        EnrichedDataDeletedEvent deletedEvent = new EnrichedDataDeletedEvent(OffsetDateTime.now(), deletedPayload);

        sendEvent(deletedEvent);

        sleep(Duration.ofSeconds(2));
        verifyNoInteractions(enrichedDataQuery);
        verifyNoInteractions(airqPhenotypeQuery);
        assertThat(eventsList).isEmpty();
        verifyAirqPhenotypeCount(0);
    }

    private StationId stationId() {
        return StationId.from(RandomStringUtils.randomAlphabetic(5));
    }

    private List<EnrichedData> generateEnrichedDataSinceLast2Days(StationId stationId) {
        OffsetDateTime current = OffsetDateTime.now();
        current = current.minusMinutes(current.getMinute())
                         .minusSeconds(current.getSecond())
                         .minusNanos(current.getNano());
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

    private void verifyAirqPhenotypeCount(int value) {
        Set<AirqPhenotype> data = airqPhenotypeQuery.findAll().await().atMost(Duration.ofSeconds(2));
        assertThat(data).hasSize(value);
    }

    private void verifyAirqPhenotypeCreatedEvent(AirqEvent<AirqPhenotypeCreatedPayload> event, StationId stationId) {
        assertThat(event).isInstanceOf(AirqPhenotypeCreatedEvent.class);
        final AirqPhenotype airqPhenotype = event.payload.airqPhenotype;
        assertThat(airqPhenotype).isNotNull();
        assertThat(airqPhenotype.fields).containsExactlyInAnyOrderElementsOf(Pm10PhenotypeMap.DEFAULT_ENRICHED_DATA_FIELDS);
        assertThat(airqPhenotype.values).noneMatch(Objects::isNull);
        assertThat(airqPhenotype.stationId).isEqualTo(stationId);
        assertThat(airqPhenotype.timestamp).isBeforeOrEqualTo(OffsetDateTime.now());
        assertThat(airqPhenotype.fitness).isGreaterThan(0);
        assertThat(airqPhenotype.prediction).isNotNull();
        assertThat(airqPhenotype.prediction.timeUnit).isEqualTo(timeUnit);
        assertThat(airqPhenotype.prediction.timeframe).isEqualTo(timeFrame);
        assertThat(airqPhenotype.prediction.field).isEqualTo(Pm10PhenotypeMap.FIELD);
    }

    private void recreateAirqPhenotypeTable() {
        client.query(DROP_AIRQ_PHENOTYPE_TABLE).execute()
              .flatMap(r -> client.query(CREATE_AIRQ_PHENOTYPE_TABLE).execute())
              .await().atMost(Duration.ofSeconds(5));
    }

    private TSKey sendEvent(AirqEvent<EnrichedDataEventPayload> event) {
        EnrichedData enrichedData = event.payload.enrichedData;
        TSKey key = TSKey.from(enrichedData.timestamp, enrichedData.station.value());
        final Future<RecordMetadata> future = producer.send(new ProducerRecord<>(enrichedDataTopic, key, event));
        try {
            future.get(5, TimeUnit.SECONDS);
            return key;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private AirqEvent<AirqPhenotypeCreatedPayload> awaitForEvent(TSKey key) {
        SKey sKey = new SKey(key.stationId());
        await().atMost(Duration.ofSeconds(5)).until(() -> eventsMap.containsKey(sKey));
        return eventsMap.get(sKey);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ignore) {
        }
    }

    @Dependent
    static class KafkaConfiguration {

        @ConfigProperty(name = "kafka.bootstrap.servers")
        String bootstrapServers;
        @ConfigProperty(name = "mp.messaging.outgoing.phenotype-created.topic")
        String phenotypeCreatedTopic;

        @Inject
        EventParser parser;

        @Produces
        KafkaProducer<TSKey, AirqEvent<EnrichedDataEventPayload>> stringKafkaProducer() {
            Properties properties = new Properties();
            properties.put("bootstrap.servers", bootstrapServers);

            return new KafkaProducer<>(properties, new TSKeySerializer(), new AirqEventSerializer<>(parser));
        }

        @Produces
        KafkaConsumer<SKey, AirqEvent<AirqPhenotypeCreatedPayload>> kafkaConsumer() {
            Properties properties = new Properties();
            properties.put("bootstrap.servers", bootstrapServers);
            properties.put("enable.auto.commit", "true");
            properties.put("group.id", "airq-ga-int-test");
            properties.put("auto.offset.reset", "earliest");

            KafkaConsumer<SKey, AirqEvent<AirqPhenotypeCreatedPayload>> consumer = new KafkaConsumer<>(
                    properties, new SKeyDeserializer(), new AirqEventDeserializer<>(parser)
            );
            consumer.subscribe(Collections.singleton(phenotypeCreatedTopic));
            return consumer;
        }

    }

}
