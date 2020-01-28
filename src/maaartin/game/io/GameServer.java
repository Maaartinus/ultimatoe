package maaartin.game.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import de.grajcar.dout.Dout;

public final class GameServer {
	private GameServer(String[] args) throws IOException {
		serverSocket = new ServerSocket(12345);
		executorService = Executors.newFixedThreadPool(100);
	}

	public static void main(String[] args) throws IOException {
		Dout.a("STARTED");
		new GameServer(args).go();
		Dout.a("DONE");
	}

	void register(GameServerConnection connection) {
		final String gameName = connection.gameName();
		checkNotNull(gameName);
		connections.put(gameName, connection);
	}

	void unregister(GameServerConnection connection) {
		final String gameName = connection.gameName();
		if (gameName!=null) connections.remove(gameName, connection);
	}

	void broadcast(String gameName, String argument) {
		for (final GameServerConnection c : connections.get(gameName)) {
			c.post(argument);
		}
	}

	private void go() {
		while (true) {
			try {
				final Socket socket = serverSocket.accept();
				try {
					final GameServerConnection connection = new GameServerConnection(this, socket);
					executorService.submit(connection.receiver());
					executorService.submit(connection.sender());
				} catch (IOException | RuntimeException e) {
					log(e);
				}
			} catch (final IOException e) {
				log(e);
				break;
			} finally {
				try {
					serverSocket.close();
				} catch (final IOException e) {
					log(e);
				}
			}
		}
	}

	private void log(Exception e) {
		// Can't really do anything else about it.
		// Using no logger in order to minimize dependencies (and java.util.logging is a joke).
		e.printStackTrace();
	}

	private final ServerSocket serverSocket;
	private final ExecutorService executorService;

	private final Multimap<String, GameServerConnection> connections =
			Multimaps.synchronizedMultimap(HashMultimap.<String, GameServerConnection>create());
}
