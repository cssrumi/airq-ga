package pl.airq.ga.domain;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;
import pl.airq.common.domain.phenotype.AirqPhenotypeQuery;
import pl.airq.common.domain.station.StationQuery;
import pl.airq.common.vo.StationId;

@Path("/api/query")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QueryEndpoint {

    private final StationQuery stationQuery;
    private final AirqPhenotypeQuery phenotypeQuery;

    @Inject
    public QueryEndpoint(StationQuery stationQuery, AirqPhenotypeQuery phenotypeQuery) {
        this.stationQuery = stationQuery;
        this.phenotypeQuery = phenotypeQuery;
    }

    @GET
    @Path("/stations")
    public Response getStations() {
        return Response.ok(stationQuery.findAll().await().indefinitely()).build();
    }

    @GET
    @Path("/phenotypes")
    public Response getPhenotypes() {
        return Response.ok(phenotypeQuery.findAll().await().indefinitely()).build();
    }

    @GET
    @Path("/phenotypes/{stationId}")
    public Response getPhenotypesByStationId(@QueryParam String stationId) {
        final StationId id = StationId.from(stationId);
        return Response.ok(phenotypeQuery.findByStationId(id).await().indefinitely()).build();
    }

}
