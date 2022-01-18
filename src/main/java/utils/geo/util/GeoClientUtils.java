package utils.geo.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.geotools.geometry.jts.Geometries;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.InputStreamInStream;
import org.locationtech.jts.io.OutputStreamOutStream;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.geometry.BoundingBox;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import utils.Size2d;
import utils.Size2i;
import utils.Utilities;
import utils.func.FOption;
import utils.stream.FStream;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class GeoClientUtils {
	private static final ThreadLocal<GeodeticCalculator> GEODETIC_CALC
											= new ThreadLocal<GeodeticCalculator>();
	
	public final static GeometryFactory GEOM_FACT = new GeometryFactory();
	public final static Point EMPTY_POINT = GEOM_FACT.createPoint((Coordinate)null);
	public final static MultiPoint EMPTY_MULTIPOINT = GEOM_FACT.createMultiPoint((Coordinate[])null);
	public final static LineString EMPTY_LINESTRING = GEOM_FACT.createLineString(new Coordinate[0]);
	public final static LinearRing EMPTY_LINEARRING = GEOM_FACT.createLinearRing(new Coordinate[0]);
	public final static MultiLineString EMPTY_MULTILINESTRING = GEOM_FACT.createMultiLineString(null);
	public final static Polygon EMPTY_POLYGON = GEOM_FACT.createPolygon(new Coordinate[0]);
	public final static MultiPolygon EMPTY_MULTIPOLYGON = GEOM_FACT.createMultiPolygon(null);
	public final static GeometryCollection EMPTY_GEOM_COLLECTION = GEOM_FACT.createGeometryCollection(new Geometry[0]);
	public final static Geometry EMPTY_GEOMETRY = GEOM_FACT.createGeometry(EMPTY_POINT);
	
	public static Geometry emptyGeometry(Geometries type) {
		switch ( type) {
			case POINT:
				return EMPTY_POINT;
			case MULTIPOINT:
				return EMPTY_MULTIPOINT;
			case LINESTRING:
				return EMPTY_LINESTRING;
			case MULTILINESTRING:
				return EMPTY_MULTILINESTRING;
			case MULTIPOLYGON:
				return EMPTY_MULTIPOLYGON;
			case POLYGON:
				return EMPTY_POLYGON;
			case GEOMETRYCOLLECTION:
				return EMPTY_GEOM_COLLECTION;
			case GEOMETRY:
				return EMPTY_GEOMETRY;
			default:
				throw new AssertionError();
		}
	}
	
	final static GeometryBuilder GEOM_BUILDER = new GeometryBuilder(GEOM_FACT);
	
	public static double distanceWgs84(Point pt1, Point pt2) {
		GeodeticCalculator gc = GEODETIC_CALC.get();
		if ( gc == null ) {
			GEODETIC_CALC.set(gc = new GeodeticCalculator());
		}
		
		gc.setStartingGeographicPoint(pt1.getX(), pt1.getY());
		gc.setDestinationGeographicPoint(pt2.getX(), pt2.getY());
		return gc.getOrthodromicDistance();
	}
	
	public static Size2d size(Envelope envl) {
		return new Size2d(envl.getWidth(), envl.getHeight());
	}

	private static final String FPN = "(\\d+(\\.\\d*)?|(\\d+)?\\.\\d+)";
	private static final String PT = String.format("\\(\\s*%s\\s*,\\s*%s\\s*\\)", FPN, FPN);
	private static final String SZ = String.format("\\s*%s\\s*[xX]\\s*%s", FPN, FPN);
	private static final String ENVL =  String.format("\\s*%s\\s*:\\s*%s", PT, SZ);
	private static final Pattern PATTERN_ENVL = Pattern.compile(ENVL);
	public static FOption<Envelope> parseEnvelope(String expr) {
		Matcher matcher = PATTERN_ENVL.matcher(expr);
		if ( matcher.find() ) {
			FOption.empty();
		}
		
		double x = Double.parseDouble(matcher.group(1));
		double y = Double.parseDouble(matcher.group(4));
		Coordinate min = new Coordinate(x, y);
		
		double width = Double.parseDouble(matcher.group(7));
		double height = Double.parseDouble(matcher.group(10));
		Coordinate max = new Coordinate(x + width, y + height);
		
		return FOption.of(new Envelope(min, max));
	}
	
	public static String toString(Envelope envl) {
		double width = envl.getMaxX() - envl.getMinX();
		double height = envl.getMaxY() - envl.getMinY();
		return String.format("(%f,%f):%fx%f", envl.getMinX(), envl.getMinY(), width, height);
	}

	public static Geometry fromWKT(String wktStr) throws ParseException {
		return (wktStr != null) ? new WKTReader(GEOM_FACT).read(wktStr) : null;
	}

	public static String toWKT(Geometry geom) {
		return (geom != null) ? new WKTWriter().write(geom) : null;
	}

	public static Geometry fromWKB(byte[] wkbBytes) throws ParseException {
		return (wkbBytes != null) ? new WKBReader(GEOM_FACT).read(wkbBytes) : null;
	}
	
	public static Geometry fromWKB(InputStream is) throws ParseException, IOException {
		if ( is == null ) {
			return null;
		}
		else {
			return new WKBReader(GEOM_FACT).read(new InputStreamInStream(is));
		}
	}

	public static byte[] toWKB(Geometry geom) {
		return (geom != null && !geom.isEmpty()) ? new WKBWriter().write(geom) : null;
	}
	
	public static void toWKBStream(Geometry geom, OutputStream os) throws IOException {
		Preconditions.checkArgument(geom != null && !geom.isEmpty());
		
		new WKBWriter().write(geom, new OutputStreamOutStream(os));
	}
	
	public static Envelope toEnvelope(BoundingBox bbox) {
		Coordinate topLeft = new Coordinate(bbox.getMinX(), bbox.getMinY());
		Coordinate bottomRight = new Coordinate(bbox.getMaxX(), bbox.getMaxY());
		return GeometryUtils.toEnvelope(topLeft, bottomRight);
	}
	
	public static String getType(Geometry geom) {
		if ( geom == null ) {
			return null;
		}
		else {
			Geometries type = Geometries.get(geom);
			switch ( type ) {
				case MULTIPOLYGON:
					return "ST_MultiPolygon";
				case POINT:
					return "ST_Point";
				case POLYGON:
					return "ST_Polygon";
				case MULTIPOINT:
					return "ST_MultiPoint";
				case LINESTRING:
					return "ST_LineString";
				case MULTILINESTRING:
					return "ST_MultiLineString";
				case GEOMETRYCOLLECTION:
					return "ST_GeometryCollection";
				default:
					throw new AssertionError("unexpected target type: type=" + type);
			}
		}
	};

	public static final int DEFAULT_REDUCER_FACTOR = Integer.MIN_VALUE;
	public static final int NO_REDUCER_FACTOR = -1;
	private static GeometryPrecisionReducer DEFAULT_PRECISION_REDUCER
														= toGeometryPrecisionReducer(2);
	public static GeometryPrecisionReducer toGeometryPrecisionReducer(int reduceFactor) {
		if ( reduceFactor == NO_REDUCER_FACTOR ) {
			return null;
		}
		else if ( reduceFactor >= 0 ) {
			double scale = Math.pow(10, reduceFactor);
			return new GeometryPrecisionReducer(new PrecisionModel(scale));
		}
		else {
			return DEFAULT_PRECISION_REDUCER;
		}
	}
	
	public static GeometryPrecisionReducer getDefaultPrecisionReducer() {
		return DEFAULT_PRECISION_REDUCER;
	}
	
	public static void setDefaultPrecisionReducer(int reduceFactor) {
		if ( reduceFactor == NO_REDUCER_FACTOR ) {
			DEFAULT_PRECISION_REDUCER = null;
		}
		else if ( reduceFactor >= 0 ) {
			double scale = Math.pow(10, reduceFactor);
			DEFAULT_PRECISION_REDUCER = new GeometryPrecisionReducer(new PrecisionModel(scale));
		}
		else {
			throw new IllegalArgumentException("invalid precision reducer factor: " + reduceFactor);
		}
	}
	
	public static Geometry makeValid(Geometry geom) {
		if ( geom instanceof MultiPolygon ) {
			List<Polygon> validPolys = Lists.newArrayList();
			for ( Polygon poly: flatten(geom, Polygon.class) ) {
				validPolys.addAll(JTS.makeValid(poly, false));
			}
			
			return GeometryUtils.toMultiPolygon(validPolys).buffer(0);
		}
		else if ( geom instanceof Polygon ) {
			return GeometryUtils.toMultiPolygon(JTS.makeValid((Polygon)geom, false)).buffer(0);
		}
		else {
			throw new UnsupportedOperationException("cannot make valid this Geometry: geom=" + geom);
		}
	}
	
	public static Geometry cast(Geometry geom, Geometries dstType) {
		Utilities.checkNotNullArgument(geom, "geom is null");
		Utilities.checkNotNullArgument(dstType, "dstType is null");
		
		if ( Geometries.get(geom) == dstType || dstType == Geometries.GEOMETRY ) {
			return geom;
		}
		
		List<Point> pts;
		List<LineString> lines;
		List<Polygon> polys;
		switch ( dstType ) {
			case MULTIPOLYGON:
				polys = flatten(geom, Polygon.class);
				return GEOM_FACT.createMultiPolygon(polys.toArray(new Polygon[polys.size()]));
			case POLYGON:
				polys = flatten(geom, Polygon.class);
				return polys.size() > 0 ? polys.get(0) : EMPTY_POLYGON;
			case POINT:
				pts = flatten(geom, Point.class);
				return pts.size() > 0 ? pts.get(0) : EMPTY_POINT;
			case MULTIPOINT:
				pts = flatten(geom, Point.class);
				return GeometryUtils.toMultiPoint(pts.toArray(new Point[pts.size()]));
			case LINESTRING:
				lines = flatten(geom, LineString.class);
				return lines.size() > 0 ? lines.get(0) : EMPTY_LINESTRING;
			case MULTILINESTRING:
				lines = flatten(geom, LineString.class);
				return GEOM_FACT.createMultiLineString(lines.toArray(new LineString[lines.size()]));
			case GEOMETRYCOLLECTION:
				return (geom instanceof GeometryCollection)
						? (GeometryCollection)geom
						: GEOM_FACT.createGeometryCollection(new Geometry[] {geom});
			default:
				throw new AssertionError("unexpected target type: type=" + dstType);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Geometry> T cast(Geometry geom, Class<T> dstType) {
		Utilities.checkNotNullArgument(geom, "geom is null");
		Utilities.checkNotNullArgument(dstType, "dstType is null");
		
		if ( dstType.isInstance(geom) || dstType == Geometry.class ) {
			return (T)geom;
		}

		List<Point> pts;
		List<LineString> lines;
		List<Polygon> polys;
		if ( MultiPolygon.class == dstType ) {
			polys = flatten(geom, Polygon.class);
			return (T)GEOM_FACT.createMultiPolygon(polys.toArray(new Polygon[polys.size()]));
		}
		else if ( Polygon.class == dstType ) {
			polys = flatten(geom, Polygon.class);
			return polys.size() > 0 ? (T)polys.get(0) : (T)EMPTY_POLYGON;
		}
		else if ( Point.class == dstType ) {
			pts = flatten(geom, Point.class);
			return pts.size() > 0 ? (T)pts.get(0) : (T)EMPTY_POINT;
		}
		else if ( MultiPoint.class == dstType ) {
			pts = flatten(geom, Point.class);
			return (T)GeometryUtils.toMultiPoint(pts.toArray(new Point[pts.size()]));
		}
		else if ( LineString.class == dstType ) {
			lines = flatten(geom, LineString.class);
			return lines.size() > 0 ? (T)lines.get(0) : (T)EMPTY_LINESTRING;
		}
		else if ( MultiLineString.class == dstType ) {
			lines = flatten(geom, LineString.class);
			return (T)GEOM_FACT.createMultiLineString(lines.toArray(new LineString[lines.size()]));
		}
		else if ( GeometryCollection.class == dstType ) {
			return (geom instanceof GeometryCollection)
					? (T)geom
					: (T)GEOM_FACT.createGeometryCollection(new Geometry[] {geom});
		}
		
		throw new AssertionError("unexpected target type: type=" + dstType);
	}
	
	public static MultiPolygon castToMultiPolygon(Geometry src) {
		switch ( Geometries.get(src) ) {
			case MULTIPOLYGON:
				return (MultiPolygon)src;
			case POLYGON:
				return GeometryUtils.toMultiPolygon((Polygon)src);
			case GEOMETRYCOLLECTION:
				return GeometryUtils.toMultiPolygon(flatten(src, Polygon.class));
			default:
				return EMPTY_MULTIPOLYGON;
		}
	}
	
	public static List<Geometry> flatten(Geometry geom) {
		if ( geom instanceof GeometryCollection ) {
			GeometryCollection coll = (GeometryCollection)geom;
			int ngeoms = coll.getNumGeometries();
			
			List<Geometry> geomList = Lists.newArrayListWithExpectedSize(ngeoms);
			for ( int i =0; i < ngeoms; ++i ) {
				geomList.add(coll.getGeometryN(i));
			}
			
			return geomList;
		}
		else {
			return Lists.newArrayList(geom);
		}
	}
	
	public static <T extends Geometry> List<T> flatten(Geometry geom, Class<T> cls) {
		if ( geom instanceof GeometryCollection ) {
			GeometryCollection coll = (GeometryCollection)geom;
			int ngeoms = coll.getNumGeometries();
			
			List<T> geomList = Lists.newArrayList();
			for ( int i =0; i < ngeoms; ++i ) {
				Geometry elm = coll.getGeometryN(i);
				if ( cls.isInstance(elm) ) {
					geomList.add(cls.cast(elm));
				}
			}
			
			return geomList;
		}
		else if ( cls.isInstance(geom) ) {
			return Lists.newArrayList(cls.cast(geom));
		}
		else {
			return Lists.newArrayList();
		}
	}
	
//	public static FStream<Geometry> flatten(Geometry geom) {
//		if ( geom instanceof GeometryCollection ) {
//			return FStream.<Geometry>from(new GeometryIterator<>((GeometryCollection)geom))
//							.flatMap(GeoClientUtils::flatten);
//		}
//		else {
//			return FStream.of(geom);
//		}
//	}
	
//	public static <T extends Geometry> FStream<T> flatten(Geometry geom, Class<T> cls) {
//		return flatten(geom).castSafely(cls);
//	}
	
	public static List<Polygon> getComponents(MultiPolygon mpoly) {
		return fstream(mpoly)
					.cast(Polygon.class)
					.toList();
	}
	
	public static <T extends Geometry> Iterator<T> components(GeometryCollection geom) {
		return new GeometryIterator<>(geom);
	}
	
	public static Stream<Geometry> componentStream(GeometryCollection geom) {
		return Utilities.stream(new GeometryIterator<>(geom));
	}
	
	public static FStream<Geometry> fstream(GeometryCollection geom) {
		return FStream.from(new GeometryIterator<>(geom));
	}

	private static final String DEFAULT_GEOM_COLUMN = "the_geom";
	
	public static Size2d divide(Envelope envl, Size2i unit) {
		double xcount = envl.getWidth() / unit.getWidth();
		double ycount = envl.getHeight() / unit.getHeight();
		
		return new Size2d(xcount, ycount);
	}
	
	public static Size2d divide(Envelope envl, Size2d unit) {
		double xcount = envl.getWidth() / unit.getWidth();
		double ycount = envl.getHeight() / unit.getHeight();
		
		return new Size2d(xcount, ycount);
	}
}
