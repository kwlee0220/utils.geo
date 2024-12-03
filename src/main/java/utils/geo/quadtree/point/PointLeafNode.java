package utils.geo.quadtree.point;

import java.util.Iterator;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import utils.geo.quadtree.TooBigValueException;
import utils.stream.FStream;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class PointLeafNode<T extends PointValue, P extends PointPartition<T>> extends PointNode<T,P> {
	private static final Logger s_logger = LoggerFactory.getLogger(PointLeafNode.class);
	
	@Nullable private final Function<Envelope,P> m_partSupplier;
	private final P m_partition;
	private PointLeafNode<T,P> m_prev;
	private PointLeafNode<T,P> m_next;
	
	PointLeafNode(Envelope bounds, P partition) {
		super(bounds);
		
		m_partSupplier = null;
		m_partition = partition;
	}
	
	public PointLeafNode(Envelope bounds, Function<Envelope,P> partSupplier) {
		super(bounds);
		
		m_partSupplier = partSupplier;
		m_partition = partSupplier.apply(bounds);
	}
	
	public P getPartition() {
		return m_partition;
	}

	@Override
	public int getValueCount() {
		return m_partition.size();
	}

	@Override
	public int getDepth() {
		return 1;
	}
	
	public FStream<T> values() {
		return m_partition.values();
	}

	public FStream<T> query(Envelope key) {
		Preconditions.checkArgument(key != null, "search key");

		if ( getBounds().intersects(key) ) {
			return m_partition.intersects(key);
		}
		else {
			return FStream.empty();
		}
	}
	
	boolean insert(T value) throws TooBigValueException {
		return insert(value, true);
	}
	
	boolean insert(T value, boolean reserveForSpeed) throws TooBigValueException {
		if ( m_partition.add(value, reserveForSpeed) ) {
			return true;
		}
		else if ( m_partition.size() == 0 ) {
			// partition이 비어있음에도 불구하고, 입력 값을 삽입할 수 없는 경우는
			// 입력 값이 너무 큰 값으로 간주한다.
			throw new TooBigValueException("value=" + value + ", partition=" + m_partition);
		}
		else {
			return false;
		}
	}
	
	boolean expand() {
		return m_partition.expand();
	}
	
	PointNonLeafNode<T,P> split() {
		Envelope bounds = getBounds();
		double midX = bounds.getMinX() + bounds.getWidth()/2;
		double midY = bounds.getMinY() + bounds.getHeight()/2;
		Envelope[] splits = new Envelope[] {
			new Envelope(bounds.getMinX(), midX, bounds.getMinY(), midY),
			new Envelope(midX, bounds.getMaxX(), bounds.getMinY(), midY),
			new Envelope(bounds.getMinX(), midX, midY, bounds.getMaxY()),
			new Envelope(midX, bounds.getMaxX(), midY, bounds.getMaxY()),
		};
		
		@SuppressWarnings("unchecked")
		PointLeafNode<T,P>[] childNodes = FStream.of(splits)
												.map(envl -> new PointLeafNode<>(envl, m_partSupplier))
												.toArray(PointLeafNode.class);
		for ( int i =0; i < childNodes.length; ++i ) {
			PointLeafNode<T,P> node = childNodes[i];
			PointQuadTree.link((i == 0) ? m_prev : childNodes[i-1], node);
			PointQuadTree.link(node, (i == childNodes.length-1) ? m_next : childNodes[i+1]);
		}
		
		Iterator<T> iter = m_partition.values().iterator();
		while ( iter.hasNext() ) {
			T v = iter.next();
			Coordinate coord = v.getCoordinate();

			for ( int i =0; i < childNodes.length; ++i ) {
				PointLeafNode<T,P> child = childNodes[i];
				
				if ( child.getBounds().intersects(coord) ) {
					boolean done = child.insert(v, false);
					if ( !done ) {
						throw new AssertionError("fails to split node: " + this
												+ " because parition insertion failed");
					}
					break;
				}
			}
		}
		if ( s_logger.isDebugEnabled() ) {
			String details = FStream.of(childNodes)
									.zipWithIndex()
									.map(t -> String.format("%d:%d", t.index(), t.value().getValueCount()))
									.join(", ");
			s_logger.debug(String.format("splitted: %d -> %s", getValueCount(), details));
		}
		
		return new PointNonLeafNode<>(bounds, childNodes);
	}
	
	public PointLeafNode<T,P> getPreviousLeafNode() {
		return m_prev;
	}
	
	void setPreviousLeafNode(PointLeafNode<T,P> node) {
		m_prev = node;
	}
	
	public PointLeafNode<T,P> getNextLeafNode() {
		return m_next;
	}
	
	void setNextLeafNode(PointLeafNode<T,P> node) {
		m_next = node;
	}
	
	@Override
	public String toString() {
		return String.format("%s: bounds=%s, partition=%s", getClass().getSimpleName(),
								getBounds(), m_partition);
	}
}
