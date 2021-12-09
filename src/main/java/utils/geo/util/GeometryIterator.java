package utils.geo.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class GeometryIterator<T extends Geometry> implements Iterator<T> {
	private final GeometryCollection m_coll;
	private final int m_length;
	private int m_idx;
	
	GeometryIterator(GeometryCollection coll) {
		m_coll = coll;
		m_length = coll.getNumGeometries();
		m_idx = 0;
	}

	@Override
	public boolean hasNext() {
		return m_idx < m_length;
	}

	@Override
	public T next() {
		if ( hasNext() ) {
			return (T)m_coll.getGeometryN(m_idx++);
		}
		else {
			throw new NoSuchElementException("idx=" + m_idx);
		}
	}
}
