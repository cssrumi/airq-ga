package pl.airq.ga.domain.phenotype;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PhenotypeMap<T> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(PhenotypeMap.class);
    public final int size;
    protected final Class<T> clazz;
    protected final List<Field> fields;
    protected final Field fieldToPredict;

    protected PhenotypeMap(Class<T> clazz, List<Field> fromFields, Field fieldToPredict) {
        this.clazz = clazz;
        this.fields = fromFields;
        this.size = fromFields.size();
        this.fieldToPredict = fieldToPredict;
    }

    protected PhenotypeMap(Class<T> clazz, Field fieldToPredict) {
        this(clazz, Collections.emptyList(), fieldToPredict);
    }

    public Float valueToPredict(T object) {
        try {
            final Object obj = fieldToPredict.get(object);
            if (obj == null) return null;
            return (Float) obj;
        } catch (IllegalAccessException e) {
            LOGGER.error("Unable to get field {} from object: {}", fieldToPredict(), object);
            throw new PhenotypeMappingException(String.format("Unable to get %s for: %s", fieldToPredict(), object));
        }
    }

    public String fieldToPredict() {
        return fieldToPredict.getName();
    }

    public float[] map(T object) {
        float[] mapped = new float[size];
        try {
            for (int i = 0; i < size; i++) {
                final Object obj = fields.get(i).get(object);
                if (obj == null) return null;
                mapped[i] = (Float) obj;
            }
        } catch (IllegalAccessException | IllegalArgumentException e) {
            LOGGER.warn("Unable to map object to float array. Object: {}", object, e);
            return null;
        }

        return mapped;
    }

    public List<String> getFields() {
        return fields.stream()
                     .map(Field::getName)
                     .collect(Collectors.toUnmodifiableList());
    }

    private static List<Field> getFields(Class<?> clazz, List<String> fields) {
        return fields.stream()
                     .map(field -> FieldUtils.getField(clazz, field))
                     .collect(Collectors.toUnmodifiableList());
    }

    protected static List<Field> createFieldListWithExcluded(Class<?> clazz, List<String> excludedStringFields) {
        final List<Field> excludedFields = getFields(clazz, excludedStringFields);
        return FieldUtils.getAllFieldsList(clazz)
                         .stream()
                         .filter(field -> !Modifier.isStatic(field.getModifiers()))
                         .filter(field -> Modifier.isPublic(field.getModifiers()))
                         .filter(field -> !excludedFields.contains(field))
                         .collect(Collectors.toUnmodifiableList());
    }

    protected static List<Field> createFieldList(Class<?> clazz, List<String> stringFields) {
        final List<Field> fields = getFields(clazz, stringFields);
        return FieldUtils.getAllFieldsList(clazz)
                         .stream()
                         .filter(field -> !Modifier.isStatic(field.getModifiers()))
                         .filter(field -> Modifier.isPublic(field.getModifiers()))
                         .filter(fields::contains)
                         .collect(Collectors.toUnmodifiableList());
    }
}
