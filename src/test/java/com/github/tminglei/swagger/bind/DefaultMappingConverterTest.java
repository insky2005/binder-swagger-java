package com.github.tminglei.swagger.bind;

import io.swagger.models.ArrayModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static com.github.tminglei.swagger.SwaggerContext.*;
import static com.github.tminglei.bind.Simple.*;
import static com.github.tminglei.bind.Mappings.*;
import static com.github.tminglei.bind.Constraints.*;

/**
 * Created by tminglei on 9/10/15.
 */
public class DefaultMappingConverterTest {
    private DefaultMappingConverter converter = new DefaultMappingConverter();

    @Test
    public void testMtoParameters_Single() {
        List<Parameter> params = converter.mToParameters("id", $(longv(required())).in("query").desc("id").$$);

        assertEquals(params.size(), 1);

        assertTrue(params.get(0) instanceof QueryParameter);
        QueryParameter p = (QueryParameter) params.get(0);

        assertEquals(p.getType(), "integer");
        assertEquals(p.getFormat(), "int64");
        assertEquals(p.getDescription(), "id");
        assertEquals(p.getRequired(), true);

        /// 'in' is required!!!
        try {
            List<Parameter> params1 = converter.mToParameters("id", $(longv(required())).desc("id").$$);
            assertTrue("shouldn't", false);
        } catch (Exception e) {
            assertEquals(e.getMessage(), "in is required!!!");
        }
    }

    @Test
    public void testMtoParameters_Multiple() {
        List<Parameter> params = converter.mToParameters("", mapping(
                field("id", $(intv()).in("path").desc("id").$$),
                field("data", $(mapping(
                        field("id", $(intv()).desc("id").$$),
                        field("name", $(text(required())).desc("name").$$)
                )).in("body").$$)
        ));

        assertEquals(params.size(), 2);

        ///
        assertTrue(params.get(0) instanceof PathParameter);
        PathParameter p1 = (PathParameter) params.get(0);

        assertEquals(p1.getType(), "integer");
        assertEquals(p1.getFormat(), "int32");
        assertEquals(p1.getDescription(), "id");
        assertEquals(p1.getRequired(), true);

        ///
        assertTrue(params.get(1) instanceof BodyParameter);
        BodyParameter p2 = (BodyParameter) params.get(1);
        ModelImpl model = (ModelImpl) p2.getSchema();

        assertEquals(model.getType(), "object");
        assertEquals(model.getRequired(), Arrays.asList("name"));
        assertTrue(model.getProperties() != null);
        assertEquals(model.getProperties().size(), 2);
        assertTrue(model.getProperties().get("id") instanceof IntegerProperty);
        assertTrue(model.getProperties().get("name") instanceof StringProperty);

        /// 'in' is required!!!
        try {
            List<Parameter> params1 = converter.mToParameters("", mapping(
                    field("id", $(intv()).desc("id").$$),
                    field("data", $(mapping(
                            field("id", $(intv()).desc("id").$$),
                            field("name", $(text(required())).desc("name").$$)
                    )).in("body").$$)
            ));
            assertEquals(false, "shouldn't happen");
        } catch (Exception e) {
            assertEquals(e.getMessage(), "in is required!!!");
        }
    }

    @Test
    public void testMtoModel() {
        Model model = converter.mToModel(list(mapping(
                field("id", $(intv()).desc("id").$$),
                field("name", $(text(required())).desc("name").$$)
        )));
        assertTrue(model instanceof ArrayModel);
        ArrayModel m = (ArrayModel) model;

        assertTrue(m.getItems() instanceof ObjectProperty);
        ObjectProperty p = (ObjectProperty) m.getItems();

        assertTrue(p.getProperties() != null);
        assertEquals(p.getProperties().size(), 2);
        assertTrue(p.getProperties().get("id") instanceof IntegerProperty);
        assertTrue(p.getProperties().get("name") instanceof StringProperty);

        IntegerProperty p1 = (IntegerProperty) p.getProperties().get("id");
        assertEquals(p1.getRequired(), false);
        assertEquals(p1.getFormat(), "int32");
        assertEquals(p1.getDescription(), "id");

        StringProperty p2 = (StringProperty) p.getProperties().get("name");
        assertEquals(p2.getRequired(), true);
        assertEquals(p2.getDescription(), "name");
    }

    @Test
    public void testMtoProperty() {
        Property prop = converter.mToProperty(list(mapping(
                field("id", $(intv()).desc("id").$$),
                field("name", $(text(required())).desc("name").$$)
        )));
        assertTrue(prop instanceof ArrayProperty);
        ArrayProperty p = (ArrayProperty) prop;

        assertTrue(p.getItems() instanceof ObjectProperty);
        ObjectProperty p0 = (ObjectProperty) p.getItems();

        assertTrue(p0.getProperties() != null);
        assertEquals(p0.getProperties().size(), 2);
        assertTrue(p0.getProperties().get("id") instanceof IntegerProperty);
        assertTrue(p0.getProperties().get("name") instanceof StringProperty);

        IntegerProperty p1 = (IntegerProperty) p0.getProperties().get("id");
        assertEquals(p1.getRequired(), false);
        assertEquals(p1.getFormat(), "int32");
        assertEquals(p1.getDescription(), "id");

        StringProperty p2 = (StringProperty) p0.getProperties().get("name");
        assertEquals(p2.getRequired(), true);
        assertEquals(p2.getDescription(), "name");
    }

    @Test
    public void testScanModels() {
        List<Map.Entry<String, Model>> models = converter.scanModels($(mapping(
                field("id", longv()),
                field("props1", $(mapping(
                        field("id", longv()),
                        field("name", text())
                )).refName("props").$$),
                field("props2", $(mapping(
                        field("id", longv()),
                        field("name", text()),
                        field("extra", text())
                )).refName("props").$$),
                field("props3", $(mapping(
                        field("id", longv()),
                        field("name", text())
                )).refName("props").$$)
        )).refName("test").$$);

        assertEquals(models.size(), 4);
        assertEquals(models.get(0).getKey(), "test");
        assertTrue(models.get(0).getValue().getProperties().get("props1") instanceof RefProperty);
        assertEquals(((RefProperty) models.get(0).getValue().getProperties().get("props1")).get$ref(), "#/definitions/props");
        assertTrue(models.get(0).getValue().getProperties().get("props2") instanceof RefProperty);
        assertEquals(((RefProperty) models.get(0).getValue().getProperties().get("props2")).get$ref(), "#/definitions/props");
        assertTrue(models.get(0).getValue().getProperties().get("props3") instanceof RefProperty);
        assertEquals(((RefProperty) models.get(0).getValue().getProperties().get("props3")).get$ref(), "#/definitions/props");

        assertEquals(models.get(1).getKey(), "props");
        assertEquals(models.get(2).getKey(), "props");
        assertEquals(models.get(3).getKey(), "props");
        assertFalse(models.get(1).getValue().equals(models.get(2).getValue()));
        assertTrue(models.get(1).getValue().equals(models.get(3).getValue()));
    }

    ///
    @Test
    public void testIsPrimitive() {
        assertEquals(converter.isPrimitive(longv(), true), true);
        assertEquals(converter.isPrimitive(list(longv()), true), true);
        assertEquals(converter.isPrimitive(list(longv()), false), false);
        assertEquals(converter.isPrimitive(mapping(), true), false);
    }

    @Test
    public void testTargetType() {
        assertEquals(converter.targetType(longv()), "integer");
        assertEquals(converter.targetType(list(intv())), "array");
        assertEquals(converter.targetType(mapping()), "object");
        assertEquals(converter.targetType(uuid()), "string");
        assertEquals(converter.targetType(bigDecimal()), "number");
    }

    @Test
    public void testFormat() {
        assertEquals(converter.format(datetime()), "date-time");
        assertEquals(converter.format(text(email())), "email");
        assertEquals(converter.format(longv(email())), "int64");
        assertEquals(converter.format($(text()).format("json").$$), "json");
    }
}
