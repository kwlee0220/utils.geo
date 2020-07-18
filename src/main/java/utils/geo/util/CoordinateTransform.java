package utils.geo.util;

import javax.annotation.Nullable;

import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class CoordinateTransform {
	private final CoordinateReferenceSystem m_src;
	private final CoordinateReferenceSystem m_tar;
	private final MathTransform m_trans;
	private final GeometryCoordinateSequenceTransformer m_geomCST;
	
	public static CoordinateTransform get(String src, String tar) {
		return new CoordinateTransform(CRSUtils.toCRS(src), CRSUtils.toCRS(tar));
	}
	
	public static CoordinateTransform get(CoordinateReferenceSystem src, CoordinateReferenceSystem tar) {
		return new CoordinateTransform(src, tar);
	}
	
	public CoordinateTransform(CoordinateReferenceSystem src, CoordinateReferenceSystem tar) {
		try {
			m_src = src;
			m_tar = tar;
			m_trans = CRS.findMathTransform(src, tar, true);
			m_geomCST = new GeometryCoordinateSequenceTransformer();
			m_geomCST.setMathTransform(m_trans);
		}
		catch ( FactoryException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public CoordinateReferenceSystem getSourceCRS() {
		return m_src;
	}
	
	public CoordinateReferenceSystem getTargetCRS() {
		return m_tar;
	}
	
	public CoordinateTransform inverse() {
		return new CoordinateTransform(m_tar, m_src);
	}
	
	public Geometry transform(Geometry src) {
		try {
			return m_geomCST.transform(src);
		}
		catch ( TransformException e ) {
			throw new IllegalArgumentException("invalid coordinate: " + src, e);
		}
	}
	
	public Envelope transform(Envelope src) {
		try {
			return JTS.transform(src, m_trans);
		}
		catch ( TransformException e ) {
			throw new IllegalArgumentException("invalid coordinate: " + src, e);
		}
	}
	
	public Coordinate transform(Coordinate src) {
		try {
			Coordinate tar = new Coordinate();
			return JTS.transform(src, tar, m_trans);
		}
		catch ( TransformException e ) {
			throw new IllegalArgumentException("invalid coordinate: " + src, e);
		}
	}
	
	public static @Nullable CoordinateTransform getTransformToWgs84(String srid) {
		return (srid.equals("EPSG:4326")) ? null : get(srid, "EPSG:4326");
	}
	
	public static Envelope transformToWgs84(Envelope envl, String srid) {
		if ( envl != null ) {
			CoordinateTransform trans = getTransformToWgs84(srid);
			return trans != null ? trans.transform(envl) : envl;
		}
		else {
			return envl;
		}
	}
}
