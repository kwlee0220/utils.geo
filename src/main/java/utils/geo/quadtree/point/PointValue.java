package utils.geo.quadtree.point;

import org.locationtech.jts.geom.Coordinate;

import utils.geo.quadtree.EnvelopedValue;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface PointValue extends EnvelopedValue {
	public Coordinate getCoordinate();
}
