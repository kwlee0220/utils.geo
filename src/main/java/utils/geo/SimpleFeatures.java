package utils.geo;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import utils.io.FileUtils;
import utils.stream.FStream;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class SimpleFeatures {
	static final Logger s_logger = LoggerFactory.getLogger(SimpleFeatures.class);
	
	private SimpleFeatures() {
		throw new AssertionError("Should not be called: " + getClass().getName());
	}
	
	public static SimpleFeatureCollection toFeatureCollection(List<SimpleFeature> features) {
		Preconditions.checkArgument(features != null);
		
		SimpleFeatureType sfType = features.get(0).getType();
		return new ListFeatureCollection(sfType, features);
	}
	
	public static FStream<File> streamShapeFiles(File start) throws IOException {
		return FileUtils.walk(start, "**/*.shp");
	}
}
