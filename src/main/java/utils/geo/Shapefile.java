package utils.geo;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.geotools.data.PrjFileReader;
import org.geotools.data.shapefile.ShapefileDumper;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.IndexFile;
import org.geotools.data.shapefile.shp.ShapefileException;
import org.geotools.data.shapefile.shp.ShapefileHeader;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.shapefile.shp.ShapefileReader.Record;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.BaseSimpleFeatureCollection;
import org.geotools.feature.collection.DelegateSimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import utils.Throwables;
import utils.func.FOption;
import utils.func.Lazy;
import utils.func.Unchecked;
import utils.io.IOUtils;
import utils.stream.FStream;
import utils.stream.FStreams.AbstractFStream;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class Shapefile implements Closeable {
	private static final GeometryFactory GEOM_FACT = new GeometryFactory();
	
	private final File m_file;
	private final Charset m_charset;
	private final ShpFiles m_shpFiles;
	@Nullable private ShapefileHeader m_shpHeader;
	private final Lazy<DbaseFileHeader> m_dbfHeader;
	
	public static Shapefile of(File file, Charset charset) throws IOException {
		return new Shapefile(file, charset);
	}
	
	public static Shapefile of(File file) throws IOException {
		return new Shapefile(file, Charset.defaultCharset());
	}
	
	private Shapefile(File file, Charset charset) throws IOException {
		m_file = file;
		m_charset = charset;
		m_shpFiles = new ShpFiles(file);
		m_dbfHeader = Lazy.of(() -> Unchecked.getOrThrowSneakily(this::readDbfHeader));
	}

	@Override
	public void close() throws IOException {
		m_shpFiles.dispose();
	}
	
	public int getRecordCount() throws IOException {
		return getDbfHeader().getNumRecords();
	}
	
	public Envelope getTopBounds() {
		ShapefileHeader header = getShpHeader();
		return new Envelope(header.minX(), header.maxX(), header.minY(), header.maxY());
	}
	
	public File getShpFile() {
		return m_file;
	}
	
	public File getPrjFile() {
		return new File(m_shpFiles.get(ShpFileType.PRJ));
	}
	
	public File getDbfFile() {
		return new File(m_shpFiles.get(ShpFileType.DBF));
	}
	
	public IndexFile getIndexFile() throws IOException {
		return new IndexFile(m_shpFiles, false);
	}
	
	public ShapefileReader read() throws ShapefileException, IOException {
		ShapefileReader reader = new ShapefileReader(m_shpFiles, true, false, GEOM_FACT);
		if ( m_shpHeader == null ) {
			m_shpHeader = reader.getHeader();
		}
		
		return reader;
	}
	
	public FStream<Geometry> streamGeometries() throws ShapefileException, IOException {
		return new GeometryStream(read());
	}
	
	public FStream<Envelope> streamEnvelopes() throws ShapefileException, IOException {
		return new EnvelopeStream(read());
	}
	
	public FStream<SimpleFeature> streamFeatures() throws IOException {
		return new SimpleFeatureStream(m_file);
	}
	
	public ShapefileHeader getShpHeader() {
		if ( m_shpHeader == null ) {
			ShapefileReader reader = null;
			try {
				reader = new ShapefileReader(m_shpFiles, true, false, GEOM_FACT);
				m_shpHeader = reader.getHeader();
			}
			catch ( Exception e ) {
				Throwables.sneakyThrow(e);
				throw new AssertionError();
			}
			finally {
				IOUtils.closeQuietly(reader);
			}
		}
		
		return m_shpHeader;
	}
	
	public DbaseFileHeader getDbfHeader() {
		return m_dbfHeader.get();
	}
	
	public static void writeShapefile(File outputDir, Iterable<SimpleFeature> features,
										FOption<Charset> charset, FOption<Long> maxShpSize,
										FOption<Long> maxDbfSize) throws IOException {
		SimpleFeatureType sfType = features.iterator().next().getType();
		writeShapefile(outputDir, sfType, features, charset, maxShpSize, maxDbfSize);
	}
	
	public static void writeShapefile(File outputDir, SimpleFeatureType sfType,
										Iterable<SimpleFeature> features, FOption<Charset> charset,
										FOption<Long> maxShpSize, FOption<Long> maxDbfSize) throws IOException {
		SimpleFeatureCollection sfColl = new BaseSimpleFeatureCollection(sfType) {
			@Override
			public SimpleFeatureIterator features() {
				return new DelegateSimpleFeatureIterator(features.iterator());
			}
		};
		writeShapefile(outputDir, sfColl, charset, maxShpSize, maxDbfSize);
	}
	
	public static void writeShapefile(File outputDir, SimpleFeatureCollection sfColl,
										FOption<Charset> charset, FOption<Long> maxShpSize,
										FOption<Long> maxDbfSize) throws IOException {
		FileUtils.forceMkdir(outputDir);
		
		ShapefileDumper dumper = new ShapefileDumper(outputDir);
		charset.ifPresent(dumper::setCharset);
		maxShpSize.ifPresent(dumper::setMaxShpSize);
		maxDbfSize.ifPresent(dumper::setMaxDbfSize);
		
		dumper.dump(sfColl);
	}
	
	private DbaseFileHeader readDbfHeader() throws IOException {
		DbaseFileReader reader = null;
		try {
			reader = new DbaseFileReader(m_shpFiles, false, m_charset);
			return reader.getHeader();
		}
		finally {
			if ( reader != null ) {
				Unchecked.runOrThrowSneakily(reader::close);
			}
		}
	}
	
	public CoordinateReferenceSystem readCrs() throws FileNotFoundException, IOException,
														FactoryException {
		try ( FileInputStream is = new FileInputStream(getPrjFile()) ) {
			PrjFileReader reader = new PrjFileReader(is.getChannel());
			return reader.getCoordinateReferenceSystem();
		}
	}
	
	private static class GeometryStream extends AbstractFStream<Geometry> {
		private final ShapefileReader m_reader;
		
		GeometryStream(ShapefileReader reader) {
			m_reader = reader;
		}

		@Override
		protected void closeInGuard() throws Exception {
			m_reader.close();
		}

		@Override
		public FOption<Geometry> next() {
			try {
				if ( m_reader.hasNext() ) {
					Record rec = m_reader.nextRecord();
					return FOption.of((Geometry)rec.shape());
				}
				else {
					return FOption.empty();
				}
			}
			catch ( IOException e ) {
				Throwables.sneakyThrow(e);
				throw new AssertionError();
			}
		}
	}
	
	private static class EnvelopeStream extends AbstractFStream<Envelope> {
		private final ShapefileReader m_reader;
		
		EnvelopeStream(ShapefileReader reader) {
			m_reader = reader;
		}

		@Override
		protected void closeInGuard() throws Exception {
			m_reader.close();
		}

		@Override
		public FOption<Envelope> next() {
			try {
				if ( m_reader.hasNext() ) {
					Record rec = m_reader.nextRecord();
					return FOption.of(rec.envelope());
				}
				else {
					return FOption.empty();
				}
			}
			catch ( IOException e ) {
				Throwables.sneakyThrow(e);
				throw new AssertionError();
			}
		}
	}
	
	public static final class SimpleFeatureStream extends AbstractFStream<SimpleFeature> {
		private final SimpleFeatureDataStore m_sfdStore;
		private final SimpleFeatureIterator m_iter;
		
		private SimpleFeatureStream(File shpFilePath) throws IOException {
			m_sfdStore = SimpleFeatureDataStore.of(shpFilePath);
			m_iter = m_sfdStore.read().features();
		}

		@Override
		protected void closeInGuard() throws Exception {
			m_iter.close();
			m_sfdStore.close();
		}

		@Override
		public FOption<SimpleFeature> next() {
			if ( m_iter.hasNext() ) {
				return FOption.of(m_iter.next());
			}
			else {
				return FOption.empty();
			}
		}
	}
}
