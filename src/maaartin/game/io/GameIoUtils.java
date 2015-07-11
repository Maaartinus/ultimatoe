package maaartin.game.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class GameIoUtils {
	static void writeMagic(BufferedWriter out) throws IOException {
		out.write(MAGIC);
		out.write("\n");
	}

	static void readMagic(BufferedReader in) throws IOException {
		while (true) {
			final String line = in.readLine();
			if (line==null) break;
			if (line.equals(MAGIC)) break;
		}
	}

	private static final String MAGIC = "maaagic861613926";
}
