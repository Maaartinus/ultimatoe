package maaartin.game.zomis;

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
import java.util.concurrent.atomic.AtomicBoolean;

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

public class GameZomisActorOld implements Runnable {
	@RequiredArgsConstructor private class Receiver implements Runnable {
		@Override public void run() {
			try {
				while (!done.get()) step();
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
				final GameZomisMessageOld message = GameZomisMessageOld.forLine(line);
				inQueue.add(message);
			}
		}

		private final BufferedReader in;
	}

	@RequiredArgsConstructor private class Sender implements Runnable {
		@Override public void run() {
			try {
				while (!done.get()) step();
			} catch (IOException | InterruptedException | RuntimeException e) {
				log(e);
			} finally {
				close();
			}
		}

		private void step() throws IOException, InterruptedException {
			final GameZomisMessageOld message = outQueue.take();
			if (message.type() == GameZomisMessageOld.Type.OUT_ABORT) return;
			final String line = message.toString();
			Dout.a(actorName, "SEND", line);
			out.write(line);
			out.write("\n");
			out.flush();
		}

		private final BufferedWriter out;
	}

	public GameZomisActorOld(String actorName, boolean isSecond) {
		this.actorName = actorName;
		this.isSecond = isSecond;
		try {
			socket = new Socket(HOST_NAME, HOST_PORT);
			sender = new Sender(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
			receiver = new Receiver(new BufferedReader(new InputStreamReader(socket.getInputStream())));
			executorService.submit(sender);
			executorService.submit(receiver);
		} catch (final Exception e) {
			log(e);
			throw new RuntimeException(e);
		}
		outQueue.add(GameZomisMessageOld.Type.OUT_USER.newMessage("xxx", actorName, password));
	}

	@Override public void run() {
		try {
			if (!isSecond) {
				run0();
			} else {
				run1();
			}
			Dout.a(actorName, "DONE", playerIndex);
			Thread.sleep(9000);
		} catch (final Exception e) {
			log(e);
			close();
		} finally {
			close();
		}
	}

	void setListener(UltimatoeGui listener) {
		this.listener = listener;
	}

	private void run0() throws InterruptedException {
		awaitPartner(1000, GameZomisActorDemo.ACTOR_1);
		invite(GameZomisActorDemo.ACTOR_1);
		awaitGame(1000);
		sendMove("00");
		while (!done.get()) {
			processInQueue();
			Thread.sleep(1000);

		}
	}

	private void run1() throws Exception {
		Thread.sleep(100);
		awaitPartner(1000, GameZomisActorDemo.ACTOR_0);
		awaitGame(1000);
		while (!done.get()) {
			processInQueue();
			Thread.sleep(1000);
		}
	}


	@Synchronized void invite(String partnerName) {
		processInQueue();
		outQueue.add(GameZomisMessageOld.Type.OUT_INVT.newMessage("UTTT", partnerName));
	}

	@Synchronized void awaitPartner(long millis, String partnerName) {
		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() < start + millis) {
			processInQueue();
			if (onlinePlayers.contains(partnerName)) return;
			try {
				Thread.sleep(10);
			} catch (final InterruptedException e) {
				log(e);
				throw new RuntimeException(e);
			}
		}
	}

	@Synchronized void awaitGame(long millis) {
		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() < start + millis) {
			processInQueue();
			if (gameId!=null) return;
			try {
				Thread.sleep(10);
			} catch (final InterruptedException e) {
				log(e);
				throw new RuntimeException(e);
			}
		}
	}

	@Synchronized void sendMove(String move) {
		checkArgument(move.length() == 2);
		checkNotNull(gameId);
		final GameZomisMessageOld message = GameZomisMessageOld.Type.OUT_MOVE.newMessage(
				gameId, String.valueOf(move.charAt(0)), String.valueOf(move.charAt(1)));
		outQueue.add(message);
	}

	private void processInQueue() {
		while (true) {
			final GameZomisMessageOld message = inQueue.poll();
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
			outQueue.add(GameZomisMessageOld.Type.OUT_INVR.newMessage(gameId, "0"));
		} else {
			verify(partnerName==null);
			gameId = args.get(0);
			partnerName = args.get(2);
			outQueue.add(GameZomisMessageOld.Type.OUT_INVR.newMessage(gameId, "1"));
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

	private void processGend(ImmutableList<String> args) {
		done.set(true);
	}

	private void play(String move) {
		try {
			ultimatoe = ultimatoe.play(move);
		} catch (final Exception e) {
			Dout.a(e);
			ultimatoe.play(move);
		}
		if (listener!=null) listener.setState(ultimatoe);
	}

	@Synchronized private void close() {
		final boolean wasDone = done.get();
		done.set(true);
		if (!wasDone) {
			inQueue.add(GameZomisMessageOld.Type.IN_ABORT.newMessage());
			outQueue.add(GameZomisMessageOld.Type.OUT_ABORT.newMessage());
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

	@Getter private final GameAIParameters parameters = new GameAIParameters();

	private final String actorName;
	private final boolean isSecond;
	private final AtomicBoolean done = new AtomicBoolean();

	private final Socket socket;
	private final Receiver receiver;
	private final Sender sender;
	private final ExecutorService executorService = Executors.newFixedThreadPool(2);

	private final BlockingQueue<GameZomisMessageOld> inQueue = Queues.newLinkedBlockingDeque();
	private final BlockingQueue<GameZomisMessageOld> outQueue = Queues.newLinkedBlockingDeque();

	private String userName;
	private final String password = "password";
	private String partnerName;
	private String gameId;
	private String playerIndex;

	private final Set<String> onlinePlayers = Sets.newHashSet();

	private Ultimatoe ultimatoe = Ultimatoe.INITIAL_GAME;
	private final GameActor delegate = new GameMonteCarloActor();
	private UltimatoeGui listener;
}
