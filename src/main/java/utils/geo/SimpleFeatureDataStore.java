package utils.geo;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Map;

import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeatureType;

import com.google.common.collect.Maps;

import utils.io.FileUtils;
import utils.stream.FStream;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class SimpleFeatureDataStore implements Closeable {
	private final File m_file;
	private final Charset m_charset;
	private final ShapefileDataStore m_sfStore;
	
	public static SimpleFeatureDataStore of(File file) throws IOException {
		return new SimpleFeatureDataStore(file, Charset.defaultCharset());
	}
	
	public static SimpleFeatureDataStore of(File file, Charset charset) throws IOException {
		return new SimpleFeatureDataStore(file, charset);
	}
	
	private SimpleFeatureDataStore(File file, Charset charset) throws IOException {
		m_file = file;
		m_charset = charset;
		m_sfStore = loadDataStore(m_file, m_charset);
	}

	@Override
	public void close() throws IOException {
		m_sfStore.dispose();
	}
	
	public File getFile() {
		return m_file;
	}
	
	public Charset getCharset() {
		return m_charset;
	}
	
	public SimpleFeatureType getSchema() throws IOException {
		return m_sfStore.getSchema();
	}
	
	public SimpleFeatureCollection read() throws IOException {
		return m_sfStore.getFeatureSource().getFeatures();
	}
	
	public static SimpleFeatureDataStore create(File shpFile, SimpleFeatureType type,
											Charset charset, boolean createIndex)
		throws IOException {
		Map<String,Serializable> params = Maps.newHashMap();
		params.put(ShapefileDataStoreFactory.URLP.key, shpFile.toURI().toURL());
		params.put(ShapefileDataStoreFactory.DBFCHARSET.key, charset.name());
		params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, createIndex);

		ShapefileDataStoreFactory fact = new ShapefileDataStoreFactory();
		ShapefileDataStore store = (ShapefileDataStore)fact.createNewDataStore(params);
		store.createSchema(type);

		return new SimpleFeatureDataStore(shpFile, charset);
	}
	
	@Override
	public String toString() {
		return m_file.toString();
	}
	
	private static ShapefileDataStore loadDataStore(File file, Charset charset)
		throws IOException {
		ShapefileDataStore store = (ShapefileDataStore)FileDataStoreFinder.getDataStore(file);
		store.setCharset(charset);
		
		return store;
	}
	
	public static FStream<File> traverseFiles(File start, Charset charset) throws IOException {
		return FileUtils.walk(start, "**/*.shp");
	}
	
	public static FStream<SimpleFeatureDataStore> traverse(File start, Charset charset) throws IOException {
		return traverseFiles(start, charset)
						.mapOrThrow(file -> of(file, charset));
	}
}
