package utils.geo.quadtree;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class TooBigValueException extends QuadTreeException {
	private static final long serialVersionUID = 1L;

	public TooBigValueException(String details) {
		super(details);
	}
}
