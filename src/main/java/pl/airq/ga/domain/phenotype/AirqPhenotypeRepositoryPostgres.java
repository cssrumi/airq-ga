package pl.airq.ga.domain.phenotype;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.infrastructure.persistance.PersistentRepositoryPostgres;

import static pl.airq.common.infrastructure.persistance.PersistentRepositoryPostgres.Default.NEVER_EXIST_QUERY;
import static pl.airq.common.infrastructure.persistance.PersistentRepositoryPostgres.Default.NEVER_EXIST_TUPLE;

@Singleton
public class AirqPhenotypeRepositoryPostgres extends PersistentRepositoryPostgres<AirqPhenotype> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirqPhenotypeRepositoryPostgres.class);
    private static final String INSERT_QUERY = "INSERT INTO AIRQ_PHENOTYPE (\"timestamp\", stationid, fields, values, prediction, fitness) VALUES ($1, $2, $3, $4, $5, $6)";

    private final ObjectMapper mapper;

    @Inject
    public AirqPhenotypeRepositoryPostgres(PgPool client, ObjectMapper mapper) {
        super(client);
        this.mapper = mapper;
    }

    @Override
    protected String insertQuery() {
        return INSERT_QUERY;
    }

    @Override
    protected String updateQuery() {
        return INSERT_QUERY;
    }

    @Override
    protected String isAlreadyExistQuery() {
        return NEVER_EXIST_QUERY;
    }

    @Override
    protected Tuple insertTuple(AirqPhenotype data) {
        String fields = null;
        String values = null;
        String prediction = null;
        try {
            fields = mapper.writeValueAsString(data.fields);
            values = mapper.writeValueAsString(data.values);
            prediction = mapper.writeValueAsString(data.prediction);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Unable to stringify AirqPhenotype: {}", data);
        }

        return Tuple.of(data.timestamp)
                    .addString(data.stationId.value())
                    .addString(fields)
                    .addString(values)
                    .addString(prediction)
                    .addDouble(data.fitness);
    }

    @Override
    protected Tuple updateTuple(AirqPhenotype data) {
        return insertTuple(data);
    }

    @Override
    protected Tuple isAlreadyExistTuple(AirqPhenotype data) {
        return NEVER_EXIST_TUPLE;
    }

    @Override
    protected Uni<Void> postProcessAction(Result result, AirqPhenotype data) {
        return Uni.createFrom().voidItem()
                  .invoke(() -> logResult(result, data));
    }

    private void logResult(Result result, AirqPhenotype data) {
        if (result.isSuccess()) {
            LOGGER.info("AirqPhenotype has been {}.", result);
            return;
        }

        LOGGER.warn("Insertion result: {} for {}", result, data);
    }

}
