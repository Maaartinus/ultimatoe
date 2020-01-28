package maaartin.game.ultimatoe;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Synchronized;
import lombok.experimental.Accessors;

import com.google.common.collect.Lists;

import maaartin.game.StandardPlayer;

import de.grajcar.dout.Dout;

import maaartin.game.Game;
import maaartin.game.GameAIParameters;
import maaartin.game.GameActor;
import maaartin.game.GameListener;
import maaartin.game.gui.ActorChooser;

/** The GUI for the {@link Ultimatoe} game. */
public final class UltimatoeGui implements GameListener<Ultimatoe> {
	private class GameHumanActor implements GameActor, ActionListener {
		@Override @Synchronized public String selectMove(Game<?> game) {
			while (move==null) {
				try {
					$lock.wait();
				} catch (final InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			final String result = move;
			move = null;
			return result;
		}

		@Override @Synchronized public void actionPerformed(ActionEvent e) {
			if (!isCurrent) return;
			final FieldButton source = (FieldButton) e.getSource();
			move = UltimatoeUtils.coordinatesToMoveString(source.x(), source.y());
			$lock.notifyAll();
		}

		@Getter private final GameAIParameters parameters = new GameAIParameters();

		@Getter @Setter private boolean isCurrent;
		private String move;
	}

	@RequiredArgsConstructor private static final class FieldButton extends JButton {
		@Override protected void paintComponent(Graphics g1) {
			if (isEnabled()) {
				super.paintComponent(g1);
				return;
			}
			final Graphics2D g = (Graphics2D) g1.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			final int w = getWidth();
			final int h = getHeight();
			g.setColor(winnerColor());
			g.fillRect(0, 0, w, h);
			if (winner==null) return;
			g.setStroke(new BasicStroke(isRecent ? 4 : 2));
			switch (player) {
				case PLAYER_O:
					g.setColor(O_COLOR);
					g.drawOval(3, 3, w-7, h-7);
					break;
				case PLAYER_X:
					g.setColor(X_COLOR);
					g.drawLine(3, 3, w-3, w-3);
					g.drawLine(3, h-3, w-3, 3);
					break;
				case NOBODY:
			}
		}

		private final Color winnerColor() {
			if (winner==null) return BORDER_COLOR;
			switch (winner) {
				case NOBODY: return EMPTY_COLOR;
				case PLAYER_X: return EMPTY_X_COLOR;
				case PLAYER_O: return EMPTY_O_COLOR;
			}
			throw new RuntimeException("impossible");
		}

		void setIsRecent(boolean isRecent) {
			if (this.isRecent == isRecent) return;
			this.isRecent = isRecent;
			repaint();
		}

		private static final Color X_COLOR = new Color(200, 0, 0);
		private static final Color O_COLOR = new Color(0, 200, 0);
		private static final Color EMPTY_COLOR = new Color(200, 200, 220);
		private static final Color EMPTY_X_COLOR = new Color(220, 200, 200);
		private static final Color EMPTY_O_COLOR = new Color(200, 220, 200);
		private static final Color BORDER_COLOR = new Color(200, 200, 200);

		@Getter private final int x;
		@Getter private final int y;

		private boolean isRecent;
		@Accessors(fluent=false) @Setter private StandardPlayer player;
		@Accessors(fluent=false) @Setter private StandardPlayer winner;
	}

	public UltimatoeGui() {
		controlPanel.add(new JLabel("Player X: "));
		controlPanel.add(actors[0]);
		controlPanel.add(new JLabel("Player O: "));
		controlPanel.add(actors[1]);

		fasterButton.setEnabled(false);
		controlPanel.add(fasterButton);

		undoButton.setEnabled(false);
		controlPanel.add(undoButton);

		mainPanel.setLayout(new GridLayout(N_OF_GUI_FIELDS, N_OF_GUI_FIELDS));
		for (int guiY=0; guiY<N_OF_GUI_FIELDS; ++guiY) {
			for (int guiX=0; guiX<N_OF_GUI_FIELDS; ++guiX) {
				final int x = guiX%4 == 3 ? -1 : guiX - guiX/4;
				final int y = guiY%4 == 3 ? -1 : guiY - guiY/4;
				final FieldButton button = new FieldButton(x, y);
				button.setPreferredSize(BUTTON_SIZE);
				button.addActionListener(humanActors[0]);
				button.addActionListener(humanActors[1]);
				mainPanel.add(button);
			}
		}

		frame.add(controlPanel, BorderLayout.NORTH);
		frame.add(mainPanel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		timer.start();

		Dout.a();
		setState(INITIAL_GAME);
		Dout.a();
	}

	public static void main(String[] args) {
		Dout.a();
		new UltimatoeGui();
	}

	/** Store the new state of the game and display it. */
	@Override public void setState(final Ultimatoe game) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				setStateInternal(game);
				autoplay();
			}
		});
	}

	private void setStateInternal(Ultimatoe game) {
		updateFields(game);
		this.game = game;
		history.add(game);
		int nHumanPlayers = 0;
		for (int i=0; i<humanActors.length; ++i) {
			humanActors[i].isCurrent(i == game.playerOnTurn().ordinal());
			if (actors[i].isHuman()) ++nHumanPlayers;
		}
		if (nHumanPlayers != this.nHumanPlayers) {
			this.nHumanPlayers = nHumanPlayers;
			fasterButton.setEnabled(nHumanPlayers==0);
			if (nHumanPlayers==0) autoplayDelayMillis = MAX_AUTOPLAY_DELAY_MILLIS;
		}
		undoButton.setEnabled(history.size() > 2);
		frame.setTitle(title());
	}

	private void updateFields(Ultimatoe game) {
		//		Dout.a("\n" + game);
		for (final Component component : mainPanel.getComponents()) {
			if (!(component instanceof FieldButton)) continue;
			final FieldButton b = (FieldButton) component;
			if (b.x()<0 || b.y()<0) {
				b.setText("" + UltimatoeUtils.BORDER);
				b.setEnabled(false);
				continue;
			}
			final String coordinatesString = UltimatoeUtils.coordinatesToMoveString(b.x(), b.y());
			final int majorIndex = UltimatoeUtils.stringToMajorIndex(coordinatesString);
			final int minorIndex = UltimatoeUtils.stringToMinorIndex(coordinatesString);
			final Tictactoe tictactoe = game.tictactoe(majorIndex);
			final StandardPlayer playerOnField = tictactoe.getPlayerOnField(minorIndex);
			final char c = playerOnField.toChar();
			final char c0 = this.game==null ? c : this.game.tictactoe(majorIndex).getPlayerOnField(minorIndex).toChar();
			final boolean isRecent = c!=c0 && (c==UltimatoeUtils.PLAYER_0 || c==UltimatoeUtils.PLAYER_1);
			b.setIsRecent(isRecent);
			b.setWinner(tictactoe.winner());
			b.setPlayer(playerOnField);
			b.setEnabled(game.isPlayable(majorIndex) && tictactoe.isPlayable(minorIndex));
			b.repaint();
		}
	}

	private String title() {
		String result;
		if (game.isFinished()) {
			result = game.winner().toString().replace('_', ' ') + " has won!";
		} else {
			result = game.playerOnTurn().toString().replace('_', ' ') + " to go.";
		}
		result = "Turn " + game.turn() + ", " + result;
		result += " --- " + "Ultimatoe";
		return result;
	}

	private void autoplay() {
		if (game==null || game.isFinished()) return;
		autoplayInit();
		autoplayFinish();
	}

	private void autoplayInit() {
		if (swingWorker!=null) return;
		final GameActor actor = actors[game.playerOnTurn().ordinal()];
		swingWorker = apply(actor);
		swingWorker.execute();
	}

	/** Apply the move generated by AI if any. */
	private void autoplayFinish() {
		if (swingWorker==null) return;
		if (!swingWorker.isDone()) return;
		if (System.currentTimeMillis() < lastAutoplayMillis + autoplayDelayMillis) return;
		lastAutoplayMillis = System.currentTimeMillis();
		try {
			final String move = swingWorker.get();
			setState(game.play(move));
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace(); //TODO
					} finally {
			swingWorker = null;
		}
	}

	private SwingWorker<String, Void> apply(final GameActor actor) {
		return new SwingWorker<String, Void>() {
			@Override protected String doInBackground() throws Exception {
				return actor.selectMove(game);
			}
		};
	}

	private static final int N_OF_GUI_FIELDS = 11;
	private static final Dimension BUTTON_SIZE = new Dimension(50, 50);
	private static final int MAX_AUTOPLAY_DELAY_MILLIS = 2000;
	private static final double SPEEDUP_FACTOR = 1.2;

	private static final Ultimatoe INITIAL_GAME = Ultimatoe.INITIAL_GAME;

	private final JFrame frame = new JFrame();
	private final JPanel controlPanel = new JPanel();
	private final JPanel mainPanel = new JPanel();
	private final JButton fasterButton = new JButton(new AbstractAction("faster") {
		@Override public void actionPerformed(ActionEvent e) {
			autoplayDelayMillis /= SPEEDUP_FACTOR;
		}
	});
	private final JButton undoButton = new JButton(new AbstractAction("UNDO") {
		@Override public void actionPerformed(ActionEvent e) {
			final int historySize = history.size();
			setState(history.get(historySize-3));
			while (history.size() > historySize-2) history.remove(history.size() - 1);
		}
	});

	private final Timer timer = new Timer(10, new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) {
			autoplay();
		}
	});
	private long lastAutoplayMillis;
	private long autoplayDelayMillis;

	@NonNull private Ultimatoe game;

	/** The worker currently running the AI computing the move, or null. */
	@Nullable private SwingWorker<String, Void> swingWorker;

	private final GameHumanActor[] humanActors = {new GameHumanActor(), new GameHumanActor()};
	private final ActorChooser[] actors = {new ActorChooser(humanActors[0], false), new ActorChooser(humanActors[1], true)};
	private int nHumanPlayers;

	private final List<Ultimatoe> history = Lists.newArrayList();
}
