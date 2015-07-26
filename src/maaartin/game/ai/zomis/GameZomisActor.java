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
import maaartin.game.zomis.GameZomisActorDemo;

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
			Dout.a(actorName, "RECEIVE", line);
			if (line==null) {
				close();
			} else {
				final GameZomisMessage message = GameZomisMessage.forLine(line);
				Dout.a("IN-QUEUE-ADD", message);
				inQueue.add(message);
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
			Dout.a(actorName, "SEND", line);
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
				Dout.a(actorName, "DONE", playerIndex);
			} catch (final Exception e) {
				log(e);
				close();
			}
		}

		private void run0() throws InterruptedException {
			awaitPartner(ZONIS_IDIOT);
			invite(ZONIS_IDIOT);
		}

		private void run1() throws InterruptedException {
			awaitPartner(GameZomisActorDemo.ACTOR_0);
			awaitGame();
			processInQueue();
		}
	}

	public GameZomisActor(boolean isActive, String actorName, boolean isSecond) {
		this.isActive = isActive;
		this.actorName = actorName;
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
		while (!done) {
			final String result = move;
			if (result != null) {
				move = null;
				return result;
			}
			try {
				$lock.wait();
			} catch (final InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		throw new RuntimeException("Game finished");
	}

	void setListener(UltimatoeGui listener) {
		this.listener = listener;
	}

	@Synchronized void invite(String partnerName) {
		processInQueue();
		outQueue.add(GameZomisMessage.Type.OUT_INVT.newMessage("UTTT", partnerName));
	}

	@Synchronized void awaitPartner(String partnerName) throws InterruptedException {
		while (!done) {
			processInQueue();
			if (onlinePlayers.contains(partnerName)) return;
			$lock.wait();
		}
	}

	@Synchronized private void awaitGame() throws InterruptedException {
		while (true) {
			processInQueue();
			if (gameId!=null) return;
			$lock.wait();
		}
	}

	@Synchronized void sendMove(String move) {
		checkArgument(move.length() == 2);
		checkNotNull(gameId);
		final GameZomisMessage message = GameZomisMessage.Type.OUT_MOVE.newMessage(
				gameId, String.valueOf(move.charAt(0)), String.valueOf(move.charAt(1)));
		outQueue.add(message);
	}

	@Synchronized private void processInQueue() {
		while (true) {
			final GameZomisMessage message = inQueue.poll();
			if (message==null) break;
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
			$lock.notifyAll();
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
		playerIndex = args.get(1);
	}

	private void processMove(ImmutableList<String> args) {
		if (!gameId.equals(args.get(0))) return;
		Dout.a(actorName, isSecond, playerIndex, ultimatoe.playerOnTurn(), ultimatoe.playerOnTurn().ordinal());
		final String inMove = args.get(1) + args.get(2);
		play(inMove);
		if (ultimatoe.isFinished()) {
			close();
			return;
		}
		if (Integer.parseInt(playerIndex) != ultimatoe.playerOnTurn().ordinal()) return;
		final String outMove = delegate.selectMove(ultimatoe);
		sendMove(outMove);
	}

	@Synchronized private void processGend(ImmutableList<String> args) {
		done = true;
	}

	@Synchronized private void play(String move) {
		try {
			ultimatoe = ultimatoe.play(move);
			this.move = move;
			Dout.a(move);
			$lock.notifyAll();
		} catch (final Exception e) {
			Dout.a(e);
			ultimatoe.play(move);
		}
		if (listener!=null) listener.setState(ultimatoe);
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

	private static final String ZONIS_IDIOT = "#AI_UTTT_Idiot";

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
	private String playerIndex;

	private final Set<String> onlinePlayers = Sets.newHashSet();

	private Ultimatoe ultimatoe = Ultimatoe.INITIAL_GAME;
	private String move;
	private final GameActor delegate = new GameMonteCarloActor();
	private UltimatoeGui listener;
}
