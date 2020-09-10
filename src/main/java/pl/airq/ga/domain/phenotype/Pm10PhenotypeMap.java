package pl.airq.ga.domain.phenotype;

import java.lang.reflect.Field;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import pl.airq.common.domain.enriched.EnrichedData;

public class Pm10PhenotypeMap extends PhenotypeMap<EnrichedData> {

    public static final List<String> DEFAULT_ENRICHED_DATA_FIELDS = List.of("pm10", "temp", "wind", "windDirection", "humidity", "pressure");

    private Pm10PhenotypeMap(List<Field> fields) {
        super(EnrichedData.class, fields, FieldUtils.getField(EnrichedData.class, "pm10"));
    }

    public static Pm10PhenotypeMap withFields(List<String> fields) {
        return new Pm10PhenotypeMap(createFieldList(EnrichedData.class, fields));
    }

    public static Pm10PhenotypeMap withExcludedFields(List<String> excludedFields) {
        return new Pm10PhenotypeMap(createFieldListWithExcluded(EnrichedData.class, excludedFields));
    }
}
