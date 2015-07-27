package maaartin.game.ai.zomis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

import maaartin.game.Game;
import maaartin.game.GameAIParameters;
import maaartin.game.GameActor;
import maaartin.game.ai.GameMonteCarloActor;
import maaartin.game.ultimatoe.Ultimatoe;
import maaartin.game.ultimatoe.UltimatoeGui;

import de.grajcar.dout.Dout;

public class GameZomisActor implements GameActor {
	@RequiredArgsConstructor private class Receiver implements Runnable {
		@Override public void run() {
			try {
				while (!done) step();
			} catch (IOException | RuntimeException e) {
				log(e);
			} finally {
				close();
			}
		}

		private void step() throws IOException {
			final String line = in.readLine();
			Dout.a("RECEIVE", line);
			if (line==null) {
				inQueue.add(GameZomisMessage.Type.IN_ABORT.newMessage());
			} else {
				inQueue.add(GameZomisMessage.forLine(line));
			}
			synchronized ($lock) {
				$lock.notifyAll();
			}
		}

		private final BufferedReader in;
	}

	@RequiredArgsConstructor private class Sender implements Runnable {
		@Override public void run() {
			try {
				while (!done) step();
			} catch (IOException | InterruptedException | RuntimeException e) {
				log(e);
			} finally {
				close();
			}
		}

		private void step() throws IOException, InterruptedException {
			final GameZomisMessage message = outQueue.take();
			if (message.type() == GameZomisMessage.Type.OUT_ABORT) return;
			final String line = message.toString();
			Dout.a("SEND", line);
			out.write(line);
			out.write("\n");
			out.flush();
		}

		private final BufferedWriter out;
	}

	private class Initializer implements Runnable {
		@Override public void run() {
			try {
				if (!isSecond) {
					run0();
				} else {
					run1();
				}
				Dout.a("INITIALIZED", playerIndex);
			} catch (final Exception e) {
				log(e);
				close();
			}
		}

		private void run0() {
			throw new RuntimeException("Not implemented");
		}

		private void run1() throws InterruptedException {
			if (actorName.endsWith(KILLER_SUFFIX)) {
				final String name = actorName.substring(0, actorName.length() - KILLER_SUFFIX.length());
				awaitPartner(name);
				invite(name);
				awaitGame();
			} else {
				throw new RuntimeException("Not implemented");
			}
		}
	}

	public GameZomisActor(boolean isActive, String name, boolean isSecond) {
		this.isActive = isActive;
		this.actorName = name + KILLER_SUFFIX;
		this.isSecond = isSecond;
		try {
			socket = new Socket(HOST_NAME, HOST_PORT);
			sender = new Sender(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
			receiver = new Receiver(new BufferedReader(new InputStreamReader(socket.getInputStream())));
			executorService.submit(new Initializer());
			executorService.submit(sender);
			executorService.submit(receiver);
		} catch (final Exception e) {
			log(e);
			throw new RuntimeException(e);
		}
		outQueue.add(GameZomisMessage.Type.OUT_USER.newMessage("xxx", actorName, password));
	}

	@Override @Synchronized public String selectMove(Game<?> game) {
		if (!game.equals(this.game)) {
			final String move = this.game.children().get(game);
			checkNotNull(move, "\n%s\n -> \n%s\n", this.game, game);
			sendMove(move);
			//			Dout.a("\n\nOLD\n" + this.game + "\nNEW\n" + game + "\nMOVE=" + move + "\n\n");
			checkArgument(this.game.play(move).equals(game));
		}
		while (!done) {
			final String result = move;
			if (result != null) {
				move = null;
				if (this.game.playerOnTurn().ordinal() != playerIndex) {
					//					Dout.a("SKIP", game.playerOnTurn().ordinal(), playerIndex, result, "\n" + game);
					continue;
				}
				return result;
			}
			try {
				processInQueue(true);
			} catch (final InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		throw new RuntimeException("Game finished");
	}

	void setListener(UltimatoeGui listener) {
		this.listener = listener;
	}

	@Synchronized void invite(String partnerName) throws InterruptedException {
		processInQueue(false);
		outQueue.add(GameZomisMessage.Type.OUT_INVT.newMessage("UTTT", partnerName));
	}

	@Synchronized void awaitPartner(String partnerName) throws InterruptedException {
		while (!done) {
			if (onlinePlayers.contains(partnerName)) return;
			processInQueue(true);
		}
	}

	@Synchronized private void awaitGame() throws InterruptedException {
		while (true) {
			if (gameId!=null) return;
			processInQueue(true);
		}
	}

	@Synchronized void sendMove(String move) {
		checkArgument(move.length() == 2);
		checkNotNull(gameId);
		final GameZomisMessage message = GameZomisMessage.Type.OUT_MOVE.newMessage(
				gameId, String.valueOf(move.charAt(0)), String.valueOf(move.charAt(1)));
		outQueue.add(message);
	}

	@Synchronized private void processInQueue(boolean wait) throws InterruptedException {
		while (true) {
			final GameZomisMessage message = inQueue.poll();
			if (message==null) {
				if (!wait) break;
				$lock.wait();
				continue;
			}
			switch (message.type()) {
				case IN_WELC:
					break;
				case IN_STUS:
					processStus(message.args());
					break;
				case IN_INVT:
					processInvt(message.args());
					break;
				case IN_NEWG:
					processNewg(message.args());
					break;
				case IN_MOVE:
					processMove(message.args());
					break;
				case IN_GEND:
					processGend(message.args());
					break;
				default:
					Dout.a("Unexpected message: " + message);
			}
			wait = false;
			$lock.notifyAll();
			break;
		}
	}

	private void processStus(ImmutableList<String> args) {
		checkArgument(args.size() == 2);
		switch (args.get(1)) {
			case "online":
				onlinePlayers.add(args.get(0));
				break;
			case "offline":
				onlinePlayers.remove(args.get(0));
				break;
			default:
				throw new IllegalArgumentException("Unexpected: " + args);
		}
	}

	private void processInvt(ImmutableList<String> args) {
		checkArgument(args.size() == 3);
		if (!args.get(1).equals("UTTT") || gameId!=null) {
			outQueue.add(GameZomisMessage.Type.OUT_INVR.newMessage(gameId, "0"));
		} else {
			verify(partnerName==null);
			gameId = args.get(0);
			partnerName = args.get(2);
			outQueue.add(GameZomisMessage.Type.OUT_INVR.newMessage(gameId, "1"));
		}
	}

	private void processNewg(ImmutableList<String> args) {
		checkArgument(args.size() == 2);
		if (gameId==null) gameId = args.get(0);
		verify(gameId.equals(args.get(0)));
		playerIndex = Integer.parseInt(args.get(1));
	}

	private void processMove(ImmutableList<String> args) {
		if (!gameId.equals(args.get(0))) return;
		//		Dout.a(actorName, isSecond, playerIndex, game.playerOnTurn(), game.playerOnTurn().ordinal());
		final String inMove = args.get(1) + args.get(2);
		play(inMove);
		if (game.isFinished()) {
			close();
			return;
		}
		//		if (playerIndex != game.playerOnTurn().ordinal()) return;
		//		final String outMove = delegate.selectMove(game);
		//		sendMove(outMove);
	}

	@Synchronized private void processGend(ImmutableList<String> args) {
		done = true;
	}

	@Synchronized private void play(String move) {
		try {
			game = game.play(move);
			this.move = move;
			//			Dout.a("PLAY", game.playerOnTurn(), move, "\n" + game);
			$lock.notifyAll();
		} catch (final Exception e) {
			Dout.a(e);
		}
		if (listener!=null) listener.setState(game);
	}

	@Synchronized private void close() {
		Dout.a99();
		final boolean wasDone = done;
		done = true;
		if (!wasDone) {
			inQueue.add(GameZomisMessage.Type.IN_ABORT.newMessage());
			outQueue.add(GameZomisMessage.Type.OUT_ABORT.newMessage());
		}
		try {
			socket.close();  // Also closes both streams.
		} catch (final IOException e) {
			log(e);
		}
		executorService.shutdownNow();
	}

	private void log(Exception e) {
		// Can't really do anything else about it.
		// Using no logger in order to minimize dependencies (and java.util.logging is a joke).
		e.printStackTrace();
	}

	private static final String HOST_NAME = "stats.zomis.net";
	private static final int HOST_PORT = 7282;

	private static final String KILLER_SUFFIX = "-KILLER";

	@Getter private final GameAIParameters parameters = new GameAIParameters();

	private final boolean isActive;
	private final String actorName;
	private final boolean isSecond;

	private volatile boolean done;

	private final Socket socket;
	private final Receiver receiver;
	private final Sender sender;
	private final ExecutorService executorService = Executors.newFixedThreadPool(3);

	private final BlockingQueue<GameZomisMessage> inQueue = Queues.newLinkedBlockingDeque();
	private final BlockingQueue<GameZomisMessage> outQueue = Queues.newLinkedBlockingDeque();

	private final String password = "password";
	private String partnerName;
	private String gameId;
	private int playerIndex = -1;

	private final Set<String> onlinePlayers = Sets.newHashSet();

	private Ultimatoe game = Ultimatoe.INITIAL_GAME;
	private String move;
	//	private final GameActor delegate = new GameMonteCarloActor();
	private UltimatoeGui listener;
}
