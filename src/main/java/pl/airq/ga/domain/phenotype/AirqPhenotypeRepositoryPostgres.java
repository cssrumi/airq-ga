package pl.airq.ga.domain.phenotype;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.infrastructure.persistance.PersistentRepositoryPostgres;

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
    protected Tuple prepareTuple(AirqPhenotype airqPhenotype) {
        String fields = null;
        String values = null;
        String prediction = null;
        try {
            fields = mapper.writeValueAsString(airqPhenotype.fields);
            values = mapper.writeValueAsString(airqPhenotype.values);
            prediction = mapper.writeValueAsString(airqPhenotype.prediction);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Unable to stringify AirqPhenotype: {}", airqPhenotype);
        }

        return Tuple.of(airqPhenotype.timestamp)
                    .addString(airqPhenotype.stationId.getId())
                    .addString(fields)
                    .addString(values)
                    .addString(prediction)
                    .addDouble(airqPhenotype.fitness);
    }

    @Override
    protected void postSaveAction(RowSet<Row> saveResult) {
    }

    @Override
    protected void postProcessAction(Boolean result, AirqPhenotype data) {
        if (Boolean.TRUE.equals(result)) {
            LOGGER.info("AirqPhenotype saved successfully.");
            return;
        }

        LOGGER.warn("Unable to save AirqPhenotype: {}", data);
    }
}
