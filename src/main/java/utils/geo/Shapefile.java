package utils.geo;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.PrjFileReader;
import org.geotools.data.shapefile.ShapefileDataStore;
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
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import utils.Throwables;
import utils.func.FOption;
import utils.func.Lazy;
import utils.func.Try;
import utils.func.Unchecked;
import utils.func.UncheckedSupplier;
import utils.geo.util.CRSUtils;
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
	
	/**
	 * 주어진 경로의 shp 파일을 읽어 {@link Shapefile} 객체를 생성한다.
	 * 
	 * @param file		Shapefile 파일 경로명
	 * @param charset	Shapefile 파일이 사용하는 문자열 인코딩
	 * @return	{@link Shapefile} 객체
	 * @throws IOException	주어진 shapefile 파일을 읽는 도중 예외가 발생한 경우
	 */
	public static Shapefile of(File file, Charset charset) throws IOException {
		return new Shapefile(file, charset);
	}

	/**
	 * 주어진 경로의 shp 파일을 읽어 {@link Shapefile} 객체를 생성한다.
	 * shp 파일의 문자열 인코딩을 default 값을 사용한다.
	 * 
	 * @param file		Shapefile 파일 경로명
	 * @return	{@link Shapefile} 객체
	 * @throws IOException	주어진 shapefile 파일을 읽는 도중 예외가 발생한 경우
	 */
	public static Shapefile of(File file) throws IOException {
		return new Shapefile(file, Charset.defaultCharset());
	}
	
	private Shapefile(File file, Charset charset) throws IOException {
		m_file = file;
		m_charset = charset;
		m_shpFiles = new ShpFiles(file);
		m_dbfHeader = Lazy.of(UncheckedSupplier.sneakyThrow(this::readDbfHeader));
	}

	@Override
	public void close() throws IOException {
		m_shpFiles.dispose();
	}
	
	/**
	 * Shapefile에 포함된 레코드의 갯수를 반환한다.
	 * 
	 * @return	레코드 갯수
	 * @throws IOException	레코드 갯수 확인을 위해 shapefile 읽기 도중 오류가 발생한 경우.
	 */
	public int getRecordCount() throws IOException {
		return getDbfHeader().getNumRecords();
	}
	
	/**
	 * Shapefile에 포함된 전체 공간 정보의 MBR을 반환한다.
	 * 
	 * @return {@link Envelope} 객체
	 * @throws IOException	MBR값 접근을 위해 shapefile 읽기 도중 오류가 발생한 경우.
	 */
	public Envelope getTopBounds() {
		ShapefileHeader header = getShpHeader();
		return new Envelope(header.minX(), header.maxX(), header.minY(), header.maxY());
	}
	
	/**
	 * 본 Shapefile 객체가 사용하는 '.shp' 파일의 경로명을 반환한다.
	 * 
	 * @return 파일 경로명
	 */
	public File getShpFile() {
		return m_file;
	}
	
	/**
	 * 본 Shapefile 객체가 사용하는 '.prj' 파일의 경로명을 반환한다.
	 * 
	 * @return 파일 경로명
	 */
	public File getPrjFile() {
		return new File(m_shpFiles.get(ShpFileType.PRJ));
	}
	
	/**
	 * 본 Shapefile 객체가 사용하는 '.dbf' 파일의 경로명을 반환한다.
	 * 
	 * @return 파일 경로명
	 */
	public File getDbfFile() {
		return new File(m_shpFiles.get(ShpFileType.DBF));
	}
	
	/**
	 * 본 Shapefile 객체가 사용하는 '.idx' 파일의 경로명을 반환한다.
	 * 
	 * @return 파일 경로명
	 */
	public IndexFile getIndexFile() throws IOException {
		return new IndexFile(m_shpFiles, false);
	}
	
	public ShapefileDataStore getDataStore() throws IOException {
		ShapefileDataStore store = (ShapefileDataStore)FileDataStoreFinder.getDataStore(m_file);
		store.setCharset(m_charset);
		
		return store;
	}
	
	/**
	 * 본 Shapefile에 저장된 공간 정의 좌표계를 반환한다.
	 * 
	 * @return 공간 좌표계 정보
	 * @throws IOException	좌표계 정보를 얻기 위해 shp 파일을 읽는 도중 오류가 발생한 경우.
	 * @throws FactoryException	좌표계 정보를 얻는 도중 오류가 발생한 경우.
	 */
	public CoordinateReferenceSystem readCrs() throws IOException, FactoryException {
		try ( FileInputStream is = new FileInputStream(getPrjFile()) ) {
			PrjFileReader reader = new PrjFileReader(is.getChannel());
			return reader.getCoordinateReferenceSystem();
		}
	}
	
	public String readSrid() throws IOException, FactoryException {
		return CRSUtils.toEPSG(readCrs());
	}
	
	/**
	 * 본 Shapefile 객체 포함된  feature들을 접근하는 리더 객체를 반환한다.
	 * 
	 * @return {@link ShapefileReader} 객체.
	 * @throws IOException	feature 접근을 위해 파일을 읽는 도중 오류가 발생한 경우.
	 * @throws ShapefileException feature 접근을 위해 파일을 읽는 도중 오류가 발생한 경우.
	 */
	public ShapefileReader read() throws ShapefileException, IOException {
		ShapefileReader reader = new ShapefileReader(m_shpFiles, true, false, GEOM_FACT);
		if ( m_shpHeader == null ) {
			m_shpHeader = reader.getHeader();
		}
		
		return reader;
	}
	
	/**
	 * 본 Shapefile 객체 포함된 공간 객체들을 접근하는 스트림 객체를 반환한다.
	 * 
	 * @return {@link FStream} 객체.
	 * @throws IOException	feature 접근을 위해 파일을 읽는 도중 오류가 발생한 경우.
	 * @throws ShapefileException feature 접근을 위해 파일을 읽는 도중 오류가 발생한 경우.
	 */
	public FStream<Geometry> streamGeometries() throws ShapefileException, IOException {
		return new GeometryStream(read());
	}
	
	/**
	 * 본 Shapefile 객체 포함된 각 공간 객체들의 MBR을 접근하는 스트림 객체를 반환한다.
	 * 
	 * @return {@link FStream} 객체.
	 * @throws IOException	MBR 접근을 위해 파일을 읽는 도중 오류가 발생한 경우.
	 * @throws ShapefileException MBR 접근을 위해 파일을 읽는 도중 오류가 발생한 경우.
	 */
	public FStream<Envelope> streamEnvelopes() throws ShapefileException, IOException {
		return new EnvelopeStream(read());
	}
	
	public SimpleFeatureType getSimpleFeatureType() throws IOException {
		return SimpleFeatureDataStore.of(m_file).getSchema();
	}

	/**
	 * 본 Shapefile 객체 포함된  feature들을 접근하는 리더 객체를 반환한다.
	 * 
	 * @return {@link ShapefileReader} 객체.
	 * @throws IOException	feature 접근을 위해 파일을 읽는 도중 오류가 발생한 경우.
	 */
	public FStream<SimpleFeature> streamFeatures() throws IOException {
		return new SimpleFeatureStream(m_file, m_charset);
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
				if ( reader != null ) {
					Try.run(reader::close);
					reader = null;
				}
			}
		}
		
		return m_shpHeader;
	}
	
	public DbaseFileHeader getDbfHeader() {
		return m_dbfHeader.get();
	}
	
	/**
	 * 주어진 공간 feature을 shp 형식의 파일에 저장한다.
	 * shp 형식에 따라 '.shp', '.dbf' 및 '.prj' 확장자를 같은 파일들이 생성된다.
	 * 
	 * @param outputDir	생성되는 파일이 위치할 폴더 경로명
	 * @param features	저장할 공간 feature 객체 리스트
	 * @param charset	생성할 파일의 문자열 코드
	 * @param maxShpSize	생성될 '.shp' 파일의 최대 크기. 지정된 크기를 초과하면
	 * 					추가의 '.shp' 파일이 생성된다.
	 * 					별도로 지정하지 않는 경우는 {@link FOption#empty}를 사용한다.
	 * @param maxDbfSize	생성될 '.dbf' 파일의 최대 크기. 지정된 크기를 초과하면
	 * 					추가의 '.dbf' 파일이 생성된다.
	 * 					별도로 지정하지 않는 경우는 {@link FOption#empty}를 사용한다.
	 */
	public static void writeShapefile(File outputDir, Iterable<SimpleFeature> features,
										Charset charset, FOption<Long> maxShpSize,
										FOption<Long> maxDbfSize) throws IOException {
		SimpleFeatureType sfType = features.iterator().next().getType();
		writeShapefile(outputDir, sfType, features, charset, maxShpSize, maxDbfSize);
	}
	
	/**
	 * 주어진 공간 feature을 shp 형식의 파일에 저장한다.
	 * shp 형식에 따라 '.shp', '.dbf' 및 '.prj' 확장자를 같은 파일들이 생성된다.
	 * 
	 * @param outputDir	생성되는 파일이 위치할 폴더 경로명
	 * @param sfType	저장할 {@link SimpleFeature} 객체들의 타입
	 * @param features	저장할 공간 feature 객체 리스트
	 * @param charset	생성할 파일의 문자열 코드
	 * @param maxShpSize	생성될 '.shp' 파일의 최대 크기. 지정된 크기를 초과하면
	 * 					추가의 '.shp' 파일이 생성된다.
	 * 					별도로 지정하지 않는 경우는 {@link FOption#empty}를 사용한다.
	 * @param maxDbfSize	생성될 '.dbf' 파일의 최대 크기. 지정된 크기를 초과하면
	 * 					추가의 '.dbf' 파일이 생성된다.
	 * 					별도로 지정하지 않는 경우는 {@link FOption#empty}를 사용한다.
	 */
	public static void writeShapefile(File outputDir, SimpleFeatureType sfType,
										Iterable<SimpleFeature> features, Charset charset,
										FOption<Long> maxShpSize, FOption<Long> maxDbfSize) throws IOException {
		SimpleFeatureCollection sfColl = new BaseSimpleFeatureCollection(sfType) {
			@Override
			public SimpleFeatureIterator features() {
				return new DelegateSimpleFeatureIterator(features.iterator());
			}
		};
		writeShapefile(outputDir, sfColl, charset, maxShpSize, maxDbfSize);
	}
	
	/**
	 * 주어진 공간 feature을 shp 형식의 파일에 저장한다.
	 * shp 형식에 따라 '.shp', '.dbf' 및 '.prj' 확장자를 같은 파일들이 생성된다.
	 * 
	 * @param outputDir	생성되는 파일이 위치할 폴더 경로명
	 * @param sfColl	저장할 공간 feature 객체 리스트
	 * @param charset	생성할 파일의 문자열 코드
	 * @param maxShpSize	생성될 '.shp' 파일의 최대 크기. 지정된 크기를 초과하면
	 * 					추가의 '.shp' 파일이 생성된다.
	 * 					별도로 지정하지 않는 경우는 {@link FOption#empty}를 사용한다.
	 * @param maxDbfSize	생성될 '.dbf' 파일의 최대 크기. 지정된 크기를 초과하면
	 * 					추가의 '.dbf' 파일이 생성된다.
	 * 					별도로 지정하지 않는 경우는 {@link FOption#empty}를 사용한다.
	 */
	public static void writeShapefile(File outputDir, SimpleFeatureCollection sfColl,
										Charset charset, FOption<Long> maxShpSize,
										FOption<Long> maxDbfSize) throws IOException {
		FileUtils.forceMkdir(outputDir);
		
		ShapefileDumper dumper = new ShapefileDumper(outputDir);
		dumper.setCharset(charset);
		maxShpSize.ifPresent(dumper::setMaxShpSize);
		maxDbfSize.ifPresent(dumper::setMaxDbfSize);
		
		dumper.dump(sfColl);
	}
	
	public static FStream<File> traverseShpFiles(File start) throws IOException {
		return utils.io.FileUtils.walk(start, "**/*.shp");
	}
	
	public static FStream<Shapefile> traverse(File start, Charset charset) throws IOException {
		return traverseShpFiles(start)
						.mapOrThrow(file -> of(file, charset));
	}
	
	@Override
	public String toString() {
		return m_file.toString();
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
		
		private SimpleFeatureStream(File shpFilePath, Charset charset) throws IOException {
			m_sfdStore = SimpleFeatureDataStore.of(shpFilePath, charset);
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
