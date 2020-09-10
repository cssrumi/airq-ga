package pl.airq.ga.infrastructure.persistance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.PersistentRepository;
import pl.airq.common.domain.phenotype.AirqPhenotype;

@ApplicationScoped
public class AirqPhenotypeRepositoryPostgres implements PersistentRepository<AirqPhenotype> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirqPhenotypeRepositoryPostgres.class);
    private static final String INSERT_QUERY = "INSERT INTO AIRQ_PHENOTYPE (\"timestamp\", stationid, fields, values, prediction, fitness) VALUES ($1, $2, $3, $4, $5, $6)";

    private final PgPool client;
    private final ObjectMapper mapper;

    @Inject
    public AirqPhenotypeRepositoryPostgres(PgPool client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public Uni<Boolean> save(AirqPhenotype data) {
        return client.preparedQuery(INSERT_QUERY)
                     .execute(prepareAirqPhenotypeTuple(data))
                     .onItem()
                     .transform(result -> {
                         if (result.rowCount() != 0) {
                             LOGGER.debug("AirqPhenotype saved successfully.");
                             return true;
                         }

                         LOGGER.warn("Unable to save AirqPhenotype: " + data);
                         return false;
                     });
    }

    @Override
    public Uni<Boolean> upsert(AirqPhenotype data) {
        return save(data);
    }

    private Tuple prepareAirqPhenotypeTuple(AirqPhenotype airqPhenotype) {
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
}
