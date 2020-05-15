package utils.geo.quadtree.point;

import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import utils.geo.quadtree.TooBigValueException;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class PointNonLeafNode<T extends PointValue, P extends PointPartition<T>> extends PointNode<T,P> {
	private static final Logger s_logger = LoggerFactory.getLogger(PointNonLeafNode.class);
	
	private final PointNode<T,P>[] m_children;
	
	@SuppressWarnings("unchecked")
	PointNonLeafNode(Envelope bounds, PointNode<T,P>[] children) {
		super(bounds);

		m_children = new PointNode[children.length];
		for ( int i =0; i < children.length; ++i ) {
			m_children[i] = children[i];
		}
	}

	@Override
	public int getValueCount() {
		return Stream.of(m_children).mapToInt(PointNode::getValueCount).sum();
	}
	
	public PointNode<T,P>[] getChildrenNode() {
		return m_children;
	}
	
	public PointLeafNode<T,P> getFirstLeafNode() {
		PointNode<T,P> first = m_children[0];
		if ( first instanceof PointNonLeafNode ) {
			return ((PointNonLeafNode<T,P>)first).getFirstLeafNode();
		}
		else {
			return (PointLeafNode<T,P>)first;
		}
	}

	public PointLeafNode<T,P> getLastLeafNode() {
		PointNode<T,P> last = m_children[m_children.length-1];
		if ( last instanceof PointNonLeafNode ) {
			return ((PointNonLeafNode<T,P>)last).getLastLeafNode();
		}
		else {
			return (PointLeafNode<T,P>)last;
		}
	}

	public void collectIntersectingLeafNodes(final Envelope query,
												List<PointLeafNode<T,P>> collecteds) {
		for ( PointNode<T,P> child: m_children ) {
			boolean intersects = child.getBounds().intersects(query);
			if ( intersects ) {
				if ( child instanceof PointLeafNode ) {
					collecteds.add((PointLeafNode<T,P>)child);
				}
				else {
					((PointNonLeafNode<T,P>)child).collectIntersectingLeafNodes(query,
																		collecteds);
				}
			}
		}
	}

	public PointLeafNode<T,P> getFirstIntersectsLeafNode(final Envelope query) {
		for ( int i = 0; i < m_children.length; ++i ) {
			PointNode<T,P> child = m_children[i];
			
			if ( child.getBounds().intersects(query) ) {
				if ( child instanceof PointLeafNode ) {
					return (PointLeafNode<T,P>)child;
				}
				else {
					return ((PointNonLeafNode<T,P>)child).getFirstIntersectsLeafNode(query);
				}
			}
		}
		
		return null;
	}

	/**
	 * 현 non-leaf 노드에 주어진 값을 삽입한다.
	 * 주어진 값이 삽입된 leaf 노드들을 반환한다. 노드가 여러 partition에 결치는 경우가
	 * 발생할 수 있기 때문에 leaf 노드의 리스트가 반환된다.
	 * 
	 * @param value	삽입할 envelope
	 * @return	삽입된 envelope가 저장된 leaf 노드들.
	 */
	public PointLeafNode<T,P> insert(T value) {
		final Coordinate coord = value.getCoordinate();
		
		for ( int i =0; i < m_children.length; ++i ) {
			PointNode<T,P> child = m_children[i];
			
			if ( child.getBounds().intersects(coord) ) {
				if ( child instanceof PointLeafNode ) {
					// 단말노드인 경우는 split이 발생할 수도 있다.
					PointLeafNode<T,P> leaf = (PointLeafNode<T,P>)child;
					if ( leaf.insert(value) ) {
						// split이 발생되지 않은 경우.
						return leaf;
					}
					else {
						// split이 발생된 경우
						// split으로 생성된 parent non-leaf 노드를 child로 설정한다.
						// 아래 line에서 non-leaf 노드를 기준으로 삽입을 시도한다.
						try {
							child = leaf.split();
							m_children[i] = child;
						}
						catch ( TooBigValueException e ) {
							throw e;
						}
					}
				}
				
				// child가 non-leaf 노드인 경우.
				return ((PointNonLeafNode<T,P>)child).insert(value);
			}
		}
		// 본 non-leaf 노드 영역에 주어진 데이터가 포함되는 것으로 계산되지만
		// 실제 어떤 하위 노드에도 겹치지 않은 경우 -> 생길 수 없는 경우.
		String msg = String.format("bounds=%s, value=%s", getBounds(), value);
		s_logger.error(msg);
		throw new IllegalStateException("unexpected state for insert: " + msg);
	}
	
	@Override
	public String toString() {
		return String.format("%s: bounds=%s, count=%d", getClass().getSimpleName(), getBounds(),
								getValueCount());
	}
}
