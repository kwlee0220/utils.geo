package utils.geo;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import utils.io.FileUtils;
import utils.io.IOUtils;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class CRSUtils {
	private static final Logger s_logger = LoggerFactory.getLogger(CRSUtils.class);
	private static CRSAuthorityFactory s_crsFact = CRS.getAuthorityFactory(true);
	
	public static final CoordinateReferenceSystem EPSG_4326;
	public static final CoordinateReferenceSystem EPSG_3857;
	public static final CoordinateReferenceSystem EPSG_5179;
	public static final CoordinateReferenceSystem EPSG_5186;
	public static final CoordinateReferenceSystem GOOGLE_MAP;
	public static final CoordinateReferenceSystem VWORLD;
	public static final CoordinateReferenceSystem WGS84;
	
	private static final Map<String,CoordinateReferenceSystem> CRSMap = Maps.newHashMap();
	static {
		WGS84 = EPSG_4326 = toCRS("EPSG:4326");
		CRSMap.put("EPSG:4326", EPSG_4326);
		
		GOOGLE_MAP = VWORLD = EPSG_3857 = toCRS("EPSG:3857");
		CRSMap.put("EPSG:3857", EPSG_3857);
		
		EPSG_5179 = toCRS("EPSG:5179");
		CRSMap.put("EPSG:5179", EPSG_5179);
		
		EPSG_5186 = toCRS("EPSG:5186");
		CRSMap.put("EPSG:5186", EPSG_5186);
		
		loadPrjFiles();
	}
	
	private CRSUtils() {
		throw new AssertionError("should not be called: class=" + CRSUtils.class.getName());
	}
	
	public static void loadPrjFiles() {
		try {
			ClassLoader cloader = Thread.currentThread().getContextClassLoader();
			URL url = cloader.getResource("marmot/geo/");
			String path = url.getPath();
			
			List<File> prjFiles = FileUtils.walk(new File(path), "**/*.prj").toList();
			for ( File prjFile: prjFiles ) {
				String name = FilenameUtils.getBaseName(prjFile.getName());
				name = name.replaceAll("_", ":");
				
				String wkt = IOUtils.toString(prjFile);
				CoordinateReferenceSystem crs = CRS.parseWKT(wkt);
				
				CRSMap.put(name, crs);
				s_logger.trace("loaded: PRJ={}", name);
			}
		}
		catch ( Exception ignored ) { }
	}
	
	public static CoordinateReferenceSystem toCRS(String epsgCode) {
		CoordinateReferenceSystem crs = CRSMap.get(epsgCode);
		if ( crs != null ) {
			return crs;
		}
		
		try {
			return s_crsFact.createCoordinateReferenceSystem(epsgCode);
		}
		catch ( NoSuchAuthorityCodeException e ) {
			throw new IllegalArgumentException("invalid EPSG code: " + epsgCode);
		}
		catch ( FactoryException e ) {
			throw new RuntimeException("fails to get CoordinateReferenceSystem from " + epsgCode);
		}
	}
	
	public static boolean isEqual(CoordinateReferenceSystem crs1, CoordinateReferenceSystem crs2) {
		try {
			return CRS.findMathTransform(crs1, crs2, true).isIdentity();
		}
		catch ( FactoryException e ) {
			return false;
		}
	}
	
	public static String toEPSG(CoordinateReferenceSystem crs) throws FactoryException {
		String srid = CRS.lookupIdentifier(crs, true);
		if ( srid == null ) {
			srid = toEPSG(CRS.toSRS(crs));
		}
		
		return srid;
	}
	
	static final Map<String,String> CRS_NAME_MAP = Maps.newHashMap();
	static {
		CRS_NAME_MAP.put("ITRF_2000_TM_Korea_Central_Belt", "EPSG:5186");
		CRS_NAME_MAP.put("Korea_2000_Korea_Central_Belt_2010", "EPSG:5186");
		CRS_NAME_MAP.put("Korea_2000_Central_Belt_2010", "EPSG:5186");
		CRS_NAME_MAP.put("PCS_ITRF2000_TM", "EPSG:5179");
		CRS_NAME_MAP.put("GCS_WGS_1984", "EPSG:4326");
		CRS_NAME_MAP.put("Transverse_Mercator", "EPSG:2098");
	}
	public static String toEPSG(String crsName) {
		return CRS_NAME_MAP.get(crsName);
	}
}
