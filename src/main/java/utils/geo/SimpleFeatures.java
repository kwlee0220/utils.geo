package utils.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class SimpleFeatures {
	static final Logger s_logger = LoggerFactory.getLogger(SimpleFeatures.class);
	
	private SimpleFeatures() {
		throw new AssertionError("Should not be called: " + getClass().getName());
	}
}
