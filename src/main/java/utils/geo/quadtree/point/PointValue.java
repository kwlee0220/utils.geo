package utils.geo.quadtree.point;

import com.vividsolutions.jts.geom.Coordinate;

import utils.geo.quadtree.EnvelopedValue;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface PointValue extends EnvelopedValue {
	public Coordinate getCoordinate();
}
