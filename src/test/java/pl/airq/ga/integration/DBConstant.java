package pl.airq.ga.integration;

class DBConstant {

    static final String CREATE_AIRQ_PHENOTYPE_TABLE = "CREATE TABLE AIRQ_PHENOTYPE\n" +
            "(\n" +
            "    id             BIGSERIAL PRIMARY KEY,\n" +
            "    timestamp      TIMESTAMPTZ,\n" +
            "    stationid      VARCHAR,\n" +
            "    fields         JSONB,\n" +
            "    values         JSONB,\n" +
            "    prediction     JSONB,\n" +
            "    fitness        DOUBLE PRECISION\n" +
            ")";

    static final String DROP_AIRQ_PHENOTYPE_TABLE = "DROP TABLE IF EXISTS AIRQ_PHENOTYPE";

}
