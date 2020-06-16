package utils.geo.quadtree.point;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Envelope;

import utils.Utilities;
import utils.geo.quadtree.TooBigValueException;
import utils.stream.FStream;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class PointQuadTree<T extends PointValue, P extends PointPartition<T>> {
	@SuppressWarnings("unused")
	private static final Logger s_logger = LoggerFactory.getLogger(PointQuadTree.class);
	public static final int QUAD = 4;
	
	private Envelope m_rootBounds;	// QuadTree에 포함된 모든 데이터를 포함하는 최소 넓이 타일의 사각영역
	private PointNode<T,P> m_root;
	
	/**
	 * QuadTree를 생성한다.
	 * 
	 * @param quadKey	QuadTree에 부여할 quad-key.
	 * @param partitionSupplier	생성될 QuadTree가 사용할 partition 생성기.
	 * 						데이터 삽입과정에서 새로운 단말노드가 생성되어 partition이 필요할 때 활용된다.
	 */
	public PointQuadTree(Envelope rootBounds, Function<Envelope,P> partitionSupplier) {
		m_root = new PointLeafNode<T,P>(rootBounds, partitionSupplier);
		m_rootBounds = rootBounds;
	}
	
	/**
	 * QuadTree를 생성한다.
	 * 
	 * @param root	생성될 QuadTree의 최상위 노드.
	 */
	public PointQuadTree(PointNode<T,P> root) {
		Utilities.checkNotNullArgument(root, "root is null");

		m_root = root;
		m_rootBounds = root.getBounds();
	}
	
	/**
	 * QuadTree에 포함된 모든 value들을 포함하는 타일 중 가장 작은 타일의 quad-key를 반환한다.
	 * 
	 * @return	quad-key값
	 */
	public Envelope getRootBounds() {
		return m_rootBounds;
	}
	
	/**
	 * 입력 공간 데이터를 삽입하고, 데이터가 삽입된 단말 노드를 반환한다.
	 * 만일 복수 개의 단말 노드에 삽입된 경우는 모든 단말노드를 반환한다.
	 * 이때, 순서는 노드의 quad-key의 순서에 따라 반환된다.
	 * 
	 * @param value	삽입할 공간 데이터.
	 * @return	삽입된 공간 데이터가 포함된 단말 노드 리스트.
	 * @throws TooBigValueException	삽입할 데이터가 너무커서 단말노드에 저장할 수 없는 경우.
	 */
	public PointLeafNode<T,P> insert(T value) throws TooBigValueException {
		Utilities.checkNotNullArgument(value);
		
		while ( m_root instanceof PointLeafNode ) {
			PointLeafNode<T,P> lroot = (PointLeafNode<T,P>)m_root;
			if ( lroot.insert(value) ) {
				return lroot;
			}
			
			// 현재 단말노드에 더 이상 값을 넣을 수 없는 경우는 노드를 분할시킨다.
			m_root = lroot.split();
		}
		
		return ((PointNonLeafNode<T,P>)m_root).insert(value);
	}
	
	/**
	 * 본 quad-tree에 포함된 단말 노드 중에서 quad-key 순으로 가장 작은 값의 단말 노드를 반환한다.
	 * 
	 * @return	단말노드
	 */
	public PointLeafNode<T,P> getFirstLeafNode() {
		return (m_root instanceof PointLeafNode) ? (PointLeafNode<T,P>)m_root
											: ((PointNonLeafNode<T,P>)m_root).getFirstLeafNode();
	}
	
	/**
	 * 본 quad-tree에 포함된 단말 노드 중에서 quad-key 순으로 가장 큰 값의 단말 노드를 반환한다.
	 * 
	 * @return	단말노드
	 */
	public PointLeafNode<T,P> getLastLeafNode() {
		return (m_root instanceof PointLeafNode) ? (PointLeafNode<T,P>)m_root
											: ((PointNonLeafNode<T,P>)m_root).getLastLeafNode();
	}

	/**
	 * QuadTree에 저장된 모든 단말 노드들의 순환자를 반환한다.
	 * 
	 * @return	단말 노드 순환자
	 */
	public FStream<PointLeafNode<T,P>> streamLeafNodes() {
		return FStream.from(new LeafNodeIterator<>(this));
	}

	/**
	 * 본 quad-tree에 포함된 단말 노드 중에서 주어진 box와 겹치는 단말 노드 중 quad-key 순으로
	 * 가장 작은 값의 단말 노드를 반환한다.
	 * 주어진 box와 단말노드의 tile 영역과의 겹치는 여부만을 확인하기 때문에, 단말 노드의 partition에
	 * 포함된 데이터 중에는 실제로 겹치는 데이터가 없을 수도 있다.
	 * 질의 사각형 ({@code query})은 반드시 EPSG:4326 좌표체계로 기술되어야 한다.
	 * 
	 * @param op	공간 질의 연산자
	 * @param key	질의 box.
	 * @return	단말노드. 겹치는 단말 노드가 없는 경우는 null이 반환된다.
	 */
	public List<PointLeafNode<T,P>> queryLeafNodes(Envelope key) {
		List<PointLeafNode<T,P>> foundList = Lists.newArrayList();
		if ( m_root instanceof PointLeafNode ) {
			boolean intersects = m_root.getBounds().intersects(key);
			if ( intersects ) {
				foundList.add((PointLeafNode<T,P>)m_root);
			}
		}
		else {
			((PointNonLeafNode<T,P>)m_root).collectIntersectingLeafNodes(key, foundList);
		}
		
		return foundList;
	}
	
	/**
	 * QuadTree에 삽입된 모든 데이터들의 스트림을 반환한다.
	 * 
	 * @return	테이터 스트림
	 */
	public FStream<T> streamValues() {
		return streamLeafNodes().flatMap(PointLeafNode::values);
	}

	/**
	 * QuadTree에 저장된 모든 데이터 중 주어진 질의 사각형과 겹치는 데이터를 접근하는 순환자를 반환한다.
	 * 질의 사각형 ({@code query})은 반드시 EPSG:4326 좌표체계로 기술되어야 한다.
	 * 
	 * @param op	공간 질의 연산자
	 * @param key	질의 box.
	 * @return	데이터 순환자
	 */
	public FStream<T> query(Envelope key) {
		return FStream.from(queryLeafNodes(key))
						.flatMap(node -> node.query(key));
	}
	
	@Override
	public String toString() {
		return String.format("root_bounds=%s", m_rootBounds);
	}
	
	static <T extends PointValue, P extends PointPartition<T>>
	void link(PointNode<T,P> prev, PointNode<T,P> next) {
		PointLeafNode<T,P> lprev = (prev != null && prev instanceof PointNonLeafNode)
							? ((PointNonLeafNode<T,P>)prev).getLastLeafNode()
							: (PointLeafNode<T,P>)prev;
		PointLeafNode<T,P> lnext = (next != null && next instanceof PointNonLeafNode)
				? ((PointNonLeafNode<T,P>)next).getFirstLeafNode()
				: (PointLeafNode<T,P>)next;
				
		if ( lprev != null ) {
			lprev.setNextLeafNode(lnext);
		}
		if ( lnext != null ) {
			lnext.setPreviousLeafNode(lprev);
		}
	}
	
	private static class LeafNodeIterator<T extends PointValue, P extends PointPartition<T>>
														implements Iterator<PointLeafNode<T,P>> {
		private PointLeafNode<T,P> m_next;
		
		private LeafNodeIterator(PointQuadTree<T,P> tree) {
			m_next = tree.getFirstLeafNode();
		}

		@Override
		public boolean hasNext() {
			return m_next != null;
		}

		@Override
		public PointLeafNode<T,P> next() {
			PointLeafNode<T,P> ret = m_next;
			
			m_next = m_next.getNextLeafNode();
			return ret;
		}
	}
}
