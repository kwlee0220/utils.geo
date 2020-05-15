package utils.geo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

import utils.func.CheckedBiConsumerX;
import utils.stream.FStream;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public final class SerializableUtils {
	private SerializableUtils() {
		throw new AssertionError("should not be called: class=" + SerializableUtils.class);
	}
	
	public interface Reader<T> {
		public T read(ObjectInputStream ois) throws IOException, ClassNotFoundException;
	}
	
	public static void writeBinary(ObjectOutputStream oos, byte[] bytes) throws IOException {
		oos.writeInt(bytes.length);
		oos.write(bytes);
	}
	
	public static byte[] readBinary(ObjectInputStream ois) throws IOException {
		byte[] bytes = new byte[ois.readInt()];
		ois.read(bytes);
		
		return bytes;
	}
	
	public static int[] readIntArray(ObjectInputStream ois) throws IOException {
		int count = ois.readInt();
		int[] array = new int[count];
		for ( int i =0; i < count; ++i ) {
			array[i] = ois.readInt();
		}
		
		return array;
	}
	
	public static void writeIntArray(ObjectOutputStream oos, int[] array) throws IOException {
		oos.writeInt(array.length);
		for ( int i =0; i < array.length; ++i ) {
			oos.writeInt(array[i]);
		}
	}
	
	public static <T> void writeCollection(ObjectOutputStream os, Collection<T> coll,
									CheckedBiConsumerX<ObjectOutputStream,T,IOException> writer)
		throws IOException {
		os.writeInt(coll.size());
		FStream.from(coll).forEachOrThrow(v -> writer.accept(os, v));
	}
	
	public static <T> void writeEmptyCollection(ObjectOutputStream os) throws IOException {
		os.writeInt(0);
	}
	
	public static <T,C extends Collection<T>> C readCollection(ObjectInputStream ois, C coll,
																Reader<T> reader)
		throws IOException, ClassNotFoundException {
		int remains = ois.readInt();
		while ( --remains >= 0 ) {
			coll.add(reader.read(ois));
		}
		return coll;
	}
	
	public static <T> List<T> readList(ObjectInputStream ois, Reader<T> reader)
		throws IOException, ClassNotFoundException {
		return readCollection(ois, new ArrayList<>(), reader);
	}
	
	public static void writeCoordinate(ObjectOutputStream os, Coordinate coord) throws IOException {
		os.writeDouble(coord.x);
		os.writeDouble(coord.y);
	}
	
	public static Coordinate readCoordinate(ObjectInputStream ois) throws IOException {
		return new Coordinate(ois.readDouble(), ois.readDouble());
	}
	
	public static void writeEnvelope(ObjectOutputStream os, Envelope envl) throws IOException {
		os.writeDouble(envl.getMinX());
		os.writeDouble(envl.getMaxX());
		os.writeDouble(envl.getMinY());
		os.writeDouble(envl.getMaxY());
	}
	
	public static Envelope readEnvelope(ObjectInputStream ois) throws IOException {
		return new Envelope(ois.readDouble(), ois.readDouble(), ois.readDouble(), ois.readDouble());
	}
	
	public static void writeGeometry(ObjectOutputStream oos, Geometry geom) throws IOException {
		writeBinary(oos, GeometryUtils.toWKB(geom));
	}
	
	public static Geometry readGeometry(ObjectInputStream ois) throws IOException, ParseException {
		return GeometryUtils.fromWKB(readBinary(ois));
	}
}
