package maaartin.game.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import lombok.RequiredArgsConstructor;

import maaartin.game.Game;
import maaartin.game.GameActor;

public final class GameClient implements Runnable {
	@RequiredArgsConstructor public static final class Context {//TODO
		private final String userName;
		private final String gameName;
		private final String party;
	}

	private GameClient(URL url, Game<?> game, GameActor actor, boolean isStartingPlayer) throws IOException {//TODO
		this.game = game;
		this.actor = actor;
		this.isStartingPlayer = isStartingPlayer;
		connection = url.openConnection();
		in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
		GameIoUtils.writeMagic(out);
		GameIoUtils.readMagic(in);
	}

	@Override public void run() {
		try {
			if (isStartingPlayer) write();
			while (true) {
				if (game.isFinished()) break;
				read();
				if (game.isFinished()) break;
				write();
			}
		} catch (IOException | RuntimeException e) {
			log(e);
		}
	}

	private void write() throws IOException {
		final String move = actor.selectMove(game);
		game = game.play(move);
		checkNotNull(game);
		out.write(move);
		out.write("\n");
		out.flush();
	}

	private void read() throws IOException {
		final String move = in.readLine();
		checkNotNull(move);
		game = game.play(move);
		checkNotNull(game);
	}

	private void log(Exception e) {
		// Can't really do anything else about it.
		// Using no logger in order to minimize dependencies (and java.util.logging is a joke).
		e.printStackTrace();
	}

	private final GameActor actor;
	private final URLConnection connection;
	private final BufferedReader in;
	private final BufferedWriter out;
	private final boolean isStartingPlayer;

	private Game<? extends Game<?>> game;
}
