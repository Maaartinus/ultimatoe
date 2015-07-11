package maaartin.game.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.Queues;

public final class GameServerConnection {
	@RequiredArgsConstructor private final class Receiver implements Runnable {
		@Override public void run() {
			try {
				GameIoUtils.readMagic(in);
				while (true) step();
			} catch (IOException | RuntimeException e) {
				log(e);
			} finally {
				close();
			}
		}

		private void step() throws IOException {
			final String line = in.readLine().trim();
			if (line==null) return;
			final String[] split = line.split(" ", 2);
			final GameCommand command = GameCommand.forString(split[0]);
			final String argument = split[1];
			switch (command) {
				case USER_NAME:
					userName = argument;
					break;
				case GAME_NAME:
					checkNotNull(userName);
					if (gameName!=null) gameServer.unregister(GameServerConnection.this);
					gameName = argument;
					gameServer.register(GameServerConnection.this);
					break;
				case PLAYER_INDEX:
					checkNotNull(gameName);
					checkNotNull(userName);
					checkArgument(argument.equals("X") || argument.equals("O"));
					party = argument;
					break;
				case POST:
					checkNotNull(gameName);
					checkNotNull(userName);
					checkNotNull(party);
					gameServer.broadcast(gameName, argument);
					break;
			}
		}

		private final BufferedReader in;
	}

	@RequiredArgsConstructor private final class Sender implements Runnable {
		@Override public void run() {
			try {
				GameIoUtils.writeMagic(out);
				while (true) step();
			} catch (IOException | InterruptedException | RuntimeException  e) {
				log(e);
			} finally {
				close();
			}
		}

		private void step() throws IOException, InterruptedException {
			final String argument = queue.take();
			out.write(argument);
			out.write("\n");
			out.flush();
		}

		private void post(String argument) {
			queue.add(argument);
		}

		private final BufferedWriter out;
		private final BlockingQueue<String> queue = Queues.newLinkedBlockingDeque();
	}

	GameServerConnection(GameServer gameServer, Socket socket) throws IOException {
		this.gameServer = gameServer;
		this.socket = socket;
		receiver = new Receiver(new BufferedReader(new InputStreamReader(socket.getInputStream())));
		sender = new Sender(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
	}

	void post(String argument) {
		sender.post(argument);
	}

	private void close() {
		gameServer.unregister(this);
		try {
			socket.close();  // Also closes both streams.
		} catch (final IOException e) {
			log(e);
		}
	}

	private void log(Exception e) {
		// Can't really do anything else about it.
		// Using no logger in order to minimize dependencies (and java.util.logging is a joke).
		e.printStackTrace();
	}

	private final GameServer gameServer;

	private final Socket socket;
	@Getter private final Receiver receiver;
	@Getter private final Sender sender;

	private volatile String userName;
	@Getter private volatile String gameName;
	private volatile String party;
}
