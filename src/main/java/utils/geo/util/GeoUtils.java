/**
 * 
 */
package utils.geo.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.geometry.jts.Geometries;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.InputStreamInStream;
import org.locationtech.jts.io.OutputStreamOutStream;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.geometry.BoundingBox;

import com.google.common.base.Preconditions;

import utils.Tuple;
import utils.func.FOption;
import utils.stream.FStream;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class GeoUtils {
	public final static GeometryFactory GEOM_FACT = new GeometryFactory();
	public final static Point EMPTY_POINT = GEOM_FACT.createPoint((Coordinate)null);
	public final static MultiPoint EMPTY_MULTIPOINT = GEOM_FACT.createMultiPointFromCoords((Coordinate[])null);
	public final static LineString EMPTY_LINESTRING = GEOM_FACT.createLineString(new Coordinate[0]);
	public final static LinearRing EMPTY_LINEARRING = GEOM_FACT.createLinearRing(new Coordinate[0]);
	public final static MultiLineString EMPTY_MULTILINESTRING = GEOM_FACT.createMultiLineString(null);
	public final static Polygon EMPTY_POLYGON = GEOM_FACT.createPolygon(new Coordinate[0]);
	public final static MultiPolygon EMPTY_MULTIPOLYGON = GEOM_FACT.createMultiPolygon(null);
	public final static GeometryCollection EMPTY_GEOM_COLLECTION = GEOM_FACT.createGeometryCollection(new Geometry[0]);
	public final static Geometry EMPTY_GEOMETRY = GEOM_FACT.createGeometry(EMPTY_POINT);
	
	private GeoUtils() {
		throw new AssertionError("should not be called: class=" + GeoUtils.class);
	}
	
	public static Point emptyPoint() {
		return EMPTY_POINT;
	}
	
	public static MultiPoint emptyMultiPoint() {
		return EMPTY_MULTIPOINT;
	}
	
	public static LineString emptyLineString() {
		return EMPTY_LINESTRING;
	}
	
	public static LinearRing emptyLinearRing() {
		return EMPTY_LINEARRING;
	}
	
	public static MultiLineString emptyMultiLineString() {
		return EMPTY_MULTILINESTRING;
	}
	
	public static Polygon emptyPolygon() {
		return EMPTY_POLYGON;
	}
	
	public static MultiPolygon emptyMultiPolygon() {
		return EMPTY_MULTIPOLYGON;
	}
	
	public static GeometryCollection emptyGeometryCollection() {
		return EMPTY_GEOM_COLLECTION;
	}
	
	public static Geometry emptyGeometry() {
		return EMPTY_GEOMETRY;
	}
	
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
	
	public static Point toPoint(double x, double y) {
		return GEOM_FACT.createPoint(new Coordinate(x, y));
	}
	
	public static Point plus(Point pt1, Point pt2) {
		return toPoint(pt1.getX() + pt2.getX(), pt1.getY() + pt2.getY());
	}
	
	public static Point average(Collection<Point> pts) {
		Point total = FStream.from(pts).reduce(GeoUtils::plus);
		return toPoint(total.getX() / pts.size(), total.getY() / pts.size());
	}
	
	public static Point toPoint(Coordinate coord) {
		return GEOM_FACT.createPoint(coord);
	}
	
	public static double getAngleRadian(Coordinate p0, Coordinate p1) {
		double diffX = p1.getX() - p0.getX();
		double diffY = p1.getY() - p0.getY();
		return Math.atan2(diffY, diffX);
	}
	
	public static double getAngleRadian(Point p0, Point p1) {
		return getAngleRadian(p0.getCoordinate(), p1.getCoordinate());
	}

	public static Envelope toEnvelope(double tlX, double tlY, double brX, double brY) {
		Coordinate topLeft = new Coordinate(tlX, tlY);
		Coordinate bottomRight = new Coordinate(brX, brY);
		return new Envelope(topLeft, bottomRight);
	}

	public static Envelope toEnvelope(Coordinate tl, Coordinate br) {
		return new Envelope(tl, br);
	}

	public static Envelope toEnvelope(Point pt1, Point pt2) {
		return new Envelope(pt1.getCoordinate(), pt2.getCoordinate());
	}

	public static Envelope expandBy(Envelope envl, double distance) {
		Envelope expanded = new Envelope(envl);
		expanded.expandBy(distance);
		return expanded;
	}
	
	public static Tuple<Point,Point> getTLBR(Envelope envl) {
		return Tuple.of(toPoint(envl.getMinX(), envl.getMinY()), toPoint(envl.getMaxX(), envl.getMaxY()));
	}
	
	public static Polygon toPolygon(Coordinate... shell) {
		return GEOM_FACT.createPolygon(shell);
	}
	
	public static Polygon toPolygon(Envelope envl) {
		Coordinate[] coords = new Coordinate[] {
			new Coordinate(envl.getMinX(), envl.getMinY()),	
			new Coordinate(envl.getMaxX(), envl.getMinY()),	
			new Coordinate(envl.getMaxX(), envl.getMaxY()),	
			new Coordinate(envl.getMinX(), envl.getMaxY()),	
			new Coordinate(envl.getMinX(), envl.getMinY()),	
		};
		LinearRing shell = GEOM_FACT.createLinearRing(coords);
		return GEOM_FACT.createPolygon(shell);
	}
	
	public static Polygon toPolygon(BoundingBox bbox) {
		Coordinate[] coords = new Coordinate[] {
			new Coordinate(bbox.getMinX(), bbox.getMinY()),	
			new Coordinate(bbox.getMaxX(), bbox.getMinY()),	
			new Coordinate(bbox.getMaxX(), bbox.getMaxY()),	
			new Coordinate(bbox.getMinX(), bbox.getMaxY()),	
			new Coordinate(bbox.getMinX(), bbox.getMinY()),	
		};
		LinearRing shell = GEOM_FACT.createLinearRing(coords);
		return GEOM_FACT.createPolygon(shell);
	}
	
	public static LineSegment toLineSegment(Coordinate p0, Coordinate p1) {
		return new LineSegment(p0, p1);
	}
	public static LineSegment toLineSegment(Point p0, Point p1) {
		return new LineSegment(p0.getCoordinate(), p1.getCoordinate());
	}
	
	public static double getAngleRadian(LineSegment seg) {
		return seg.angle();
	}
	
	public static double getAngleRadian(LineSegment l1, LineSegment l2) {
		return l1.angle() - l2.angle();
	}
	
	public static LineString toLineString(LineSegment seg) {
		return GEOM_FACT.createLineString(new Coordinate[]{seg.p0, seg.p1});
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
	
	public static String toString(Coordinate coord) {
		return String.format("(%f,%f)", coord.getX(), coord.getY());
	}
	
	public static String toString(Coordinate coord, int decimals) {
		String fmt = String.format("(%%.%df,%%.%df)", decimals, decimals);
		return String.format(fmt, coord.getX(), coord.getY());
	}
	
	public static String toString(Point pt) {
		return toString(pt.getCoordinate());
	}
	
	public static String toString(Point pt, int decimals) {
		return toString(pt.getCoordinate(), decimals);
	}
	
	public static String toString(LineSegment line) {
		return String.format("%s-%s", toString(line.p0), toString(line.p1));
	}
	public static String toString(LineSegment line, int decimals) {
		return String.format("%s->%s", toString(line.p0, decimals), toString(line.p1, decimals));
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
	
	public static String toString(Envelope envl, int decimals) {
		double width = envl.getMaxX() - envl.getMinX();
		double height = envl.getMaxY() - envl.getMinY();
		String pattern = String.format("(%%.%df,%%.%df):%%.%dfx%%.%df", decimals, decimals, decimals, decimals);
		return String.format(pattern, envl.getMinX(), envl.getMinY(), width, height);
	}
	
	public static Point getCentroid(Envelope envl) {
		double centerX = envl.getMinX() + (envl.getWidth() / 2);
		double centerY = envl.getMinY() + (envl.getHeight() / 2);
		
		return toPoint(centerX, centerY);
	}
}
