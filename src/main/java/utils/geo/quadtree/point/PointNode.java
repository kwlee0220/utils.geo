package utils.geo.quadtree.point;

import com.vividsolutions.jts.geom.Envelope;

import utils.Utilities;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class PointNode<T extends PointValue, P extends PointPartition<T>> {
	private final Envelope m_bounds;
	
	public abstract int getValueCount();
	
	protected PointNode(Envelope envl) {
		Utilities.checkArgument(envl != null, "bounds is null");
		
		m_bounds = envl;
	}
	
	public Envelope getBounds() {
		return m_bounds;
	}
}