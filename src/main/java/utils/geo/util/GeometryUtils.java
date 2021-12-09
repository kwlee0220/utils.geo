package utils.geo.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
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

import utils.stream.FStream;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public final class GeometryUtils {
	public final static GeometryFactory GEOM_FACT = new GeometryFactory();
	
	private GeometryUtils() {
		throw new AssertionError("should not be called: class=" + GeometryUtils.class);
	}

	public static Envelope toEnvelope(double tlX, double tlY, double brX, double brY) {
		Coordinate topLeft = new Coordinate(tlX, tlY);
		Coordinate bottomRight = new Coordinate(brX, brY);
		return new Envelope(topLeft, bottomRight);
	}

	public static Envelope toEnvelope(Coordinate tl, Coordinate br) {
		return new Envelope(tl, br);
	}

	public static Envelope Envelope(Point pt1, Point pt2) {
		return new Envelope(pt1.getCoordinate(), pt2.getCoordinate());
	}

	public static Envelope expandBy(Envelope envl, double distance) {
		Envelope expanded = new Envelope(envl);
		expanded.expandBy(distance);
		return expanded;
	}
	
	public static Point toPoint(double x, double y) {
		return GEOM_FACT.createPoint(new Coordinate(x, y));
	}
	
	public static Point toPoint(Coordinate coord) {
		return GEOM_FACT.createPoint(coord);
	}
	
	public static LineString toLineString(Coordinate... coords) {
		return GEOM_FACT.createLineString(coords);
	}
	
	public static LineString toLineString(List<Coordinate> coords) {
		return GEOM_FACT.createLineString(coords.toArray(new Coordinate[coords.size()]));
	}
	
	public static Polygon toPolygon(LinearRing shell, List<LinearRing> holes) {
		LinearRing[] arr = holes.toArray(new LinearRing[holes.size()]);
		return GEOM_FACT.createPolygon(shell, arr);
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
	
	public static MultiPoint toMultiPoint(Point... pts) {
		return GEOM_FACT.createMultiPoint(pts);
	}
	
	public static MultiPoint toMultiPoint(Collection<Point> pts) {
		return GEOM_FACT.createMultiPoint(pts.toArray(new Point[pts.size()]));
	}
	
	public static MultiLineString toMultiLineString(LineString... lines) {
		return GEOM_FACT.createMultiLineString(lines);
	}
	
	public static MultiLineString toMultiLineString(Collection<LineString> lines) {
		return GEOM_FACT.createMultiLineString(lines.toArray(new LineString[lines.size()]));
	}
	
	public static MultiPolygon toMultiPolygon(Polygon... polys) {
		return GEOM_FACT.createMultiPolygon(polys);
	}
	
	public static MultiPolygon toMultiPolygon(List<Polygon> polyList) {
		return GEOM_FACT.createMultiPolygon(polyList.toArray(new Polygon[polyList.size()]));
	}
	
	public static MultiPolygon toMultiPolygon(FStream<Polygon> polygons) {
		return GEOM_FACT.createMultiPolygon(polygons.toArray(Polygon.class));
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
}
