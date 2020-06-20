package utils.geo.util;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public final class GeomAvroUtils {
	private GeomAvroUtils() {
		throw new AssertionError("should not be called: class=" + getClass());
	}
	
	private static final Map<Class<?>, Schema> PRIMITIVES = Maps.newHashMap();
	static {
		PRIMITIVES.put(String.class, Schema.create(Type.STRING));
		PRIMITIVES.put(Integer.class, Schema.create(Type.INT));
		PRIMITIVES.put(int.class, Schema.create(Type.INT));
		PRIMITIVES.put(Long.class, Schema.create(Type.LONG));
		PRIMITIVES.put(long.class, Schema.create(Type.LONG));
		PRIMITIVES.put(Double.class, Schema.create(Type.DOUBLE));
		PRIMITIVES.put(double.class, Schema.create(Type.DOUBLE));
		PRIMITIVES.put(Float.class, Schema.create(Type.FLOAT));
		PRIMITIVES.put(float.class, Schema.create(Type.FLOAT));
		PRIMITIVES.put(Boolean.class, Schema.create(Type.BOOLEAN));
		PRIMITIVES.put(boolean.class, Schema.create(Type.BOOLEAN));
		PRIMITIVES.put(byte[].class, Schema.create(Type.BYTES));
		
		PRIMITIVES.put(LocalDateTime.class, LogicalTypes.timestampMillis()
														.addToSchema(Schema.create(Schema.Type.LONG)));
	}
	
	public static Schema toSchema(SimpleFeatureType sfType) throws ParseException {
		List<Field> fields = Lists.newArrayList();
		for ( AttributeDescriptor desc: sfType.getAttributeDescriptors() ) {
			Class<?> cls = desc.getType().getBinding();
			
			Schema schema;
			if ( Geometry.class.isAssignableFrom(cls) ) {
				schema =  SchemaBuilder.record(cls.getSimpleName())
										.namespace(cls.getPackage().toString())
										.fields()
											.name("wkb").type().bytesType().noDefault()
										.endRecord();
			}
			else {
				schema = PRIMITIVES.get(cls);
				if ( schema == null ) {
					throw new IllegalArgumentException("unsupported field class: " + cls);
				}
			}
			fields.add(new Field(desc.getLocalName(), schema));
		}
		
		return Schema.createRecord("simple_feature", null, "etri.marmot", false, fields);
	}
	
	public static GenericRecord toGenericRecord(Schema schema, SimpleFeature sf) {
		GenericRecord rec = new GenericData.Record(schema);
		
		for ( int i =0; i < sf.getAttributeCount(); ++i ) {
			Object value = sf.getAttribute(i);
			if ( value instanceof Geometry ) {
				Field field = schema.getField(sf.getType().getDescriptor(i).getLocalName());
				rec.put(i, toGeometryRecord(field.schema(), (Geometry)value));
			}
			else {
				rec.put(i, value);
			}
		}
		
		return rec;
	}
	
	private static GenericRecord toGeometryRecord(Schema geomSchema, Geometry geom) {
		GenericRecord rec = new GenericData.Record(geomSchema);
		rec.put(0, GeoClientUtils.toWKB(geom));
		return rec;
	}
}
