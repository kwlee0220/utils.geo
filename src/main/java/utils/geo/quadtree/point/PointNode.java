package utils.geo.quadtree.point;

import org.locationtech.jts.geom.Envelope;

import utils.Utilities;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class PointNode<T extends PointValue, P extends PointPartition<T>> {
	private final Envelope m_bounds;
	
	public abstract int getValueCount();
	public abstract int getDepth();
	
	protected PointNode(Envelope envl) {
		Utilities.checkArgument(envl != null, "bounds is null");
		
		m_bounds = envl;
	}
	
	public Envelope getBounds() {
		return m_bounds;
	}
}