package maaartin.game.ultimatoe;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import lombok.NonNull;

import com.google.common.collect.Lists;

import maaartin.game.StandardPlayer;

import maaartin.game.GameActor;
import maaartin.game.GameListener;
import maaartin.game.ai.GameMonteCarloActor;
import maaartin.game.ai.GameRandomActor;

/** The GUI for the {@link Tictactoe} game. */
public final class TictactoeGui implements GameListener<Tictactoe> {
	private final class FieldListener implements ActionListener {
		@Override public void actionPerformed(ActionEvent e) {
			if (swingWorker!=null) return; // It's a AI turn.
			final int guiIndex = buttons.indexOf(e.getSource());
			final String move = String.valueOf(guiIndex);
			setState(game.play(move));
		}
	}

	private static final class FieldButton extends JButton {
		@Override protected void paintComponent(Graphics g1) {
			if (isEnabled()) {
				super.paintComponent(g1);
				return;
			}
			final Graphics2D g = (Graphics2D) g1.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			final int w = getWidth();
			final int h = getHeight();
			g.setColor(EMPTY_COLOR);
			g.fillRect(0, 0, w, h);
			g.setStroke(new BasicStroke(isRecent ? 3 : 2));
			switch (getText().charAt(0)) {
				case 'O':
					g.setColor(O_COLOR);
					g.drawOval(3, 3, w-7, h-7);
					break;
				case 'X':
					g.setColor(X_COLOR);
					g.drawLine(3, 3, w-3, w-3);
					g.drawLine(3, h-3, w-3, 3);
					break;
				case UltimatoeUtils.BORDER:
					g.setColor(BORDER_COLOR);
					g.fillRect(0, 0, getWidth(), getHeight());
					break;
			}
		}

		void setIsRecent(boolean isRecent) {
			if (this.isRecent == isRecent) return;
			this.isRecent = isRecent;
			repaint();
		}

		private static final Color X_COLOR = new Color(200, 0, 0);
		private static final Color O_COLOR = new Color(0, 200, 0);
		private static final Color EMPTY_COLOR = new Color(200, 200, 220);
		private static final Color BORDER_COLOR = new Color(200, 200, 200);

		private boolean isRecent;
	}

	private TictactoeGui() {
		final JButton fasterButton = new JButton(new AbstractAction("faster") {
			@Override public void actionPerformed(ActionEvent e) {
				autoplayDelayMillis /= SPEEDUP_FACTOR;
			}
		});

		final ActionListener autoListener = new ActionListener() {
			/** Switch the AI for a player on or off. */
			@Override public void actionPerformed(ActionEvent e) {
				final JToggleButton b = (JToggleButton) e.getSource();
				final boolean isOn = b.getModel().isSelected();
				final boolean isO = b.getText().endsWith("O");
				if (isO) {
					actors[1] = isOn ? new GameRandomActor() : null;
				} else {
					actors[0] = isOn ? new GameMonteCarloActor() : null;
				}

				final boolean isFullAuto = actors[0]!=null && actors[1]!=null;
				fasterButton.setEnabled(isFullAuto);
				autoplayDelayMillis = MAX_AUTOPLAY_DELAY_MILLIS;
				autoplay();
			}
		};

		for (final String s : "auto-X auto-O".split(" ")) {
			final JToggleButton autoButton = new JToggleButton(s);
			autoButton.addActionListener(autoListener);
			controlPanel.add(autoButton);
		}

		fasterButton.setEnabled(false);
		controlPanel.add(fasterButton);

		mainPanel.setLayout(new GridLayout(N_OF_GUI_FIELDS, N_OF_GUI_FIELDS));
		for (int i=0; i<N_OF_GUI_FIELDS*N_OF_GUI_FIELDS; ++i) {
			final FieldButton button = new FieldButton();
			button.setPreferredSize(BUTTON_SIZE);
			button.addActionListener(listener);
			buttons.add(button);
			mainPanel.add(button);
		}

		frame.add(controlPanel, BorderLayout.NORTH);
		frame.add(mainPanel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		timer.start();

		setState(INITIAL_GAME);
	}

	public static void main(String[] args) {
		new TictactoeGui();
	}

	/** Store the new state of the game and display it. */
	@Override public void setState(final Tictactoe game) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				setStateInternal(game);
				autoplay();
			}
		});
	}

	private void setStateInternal(Tictactoe game) {
		final String newString = game.asString().replace("\n", "");
		final String oldString = this.game==null ? null : this.game.asString().replace("\n", "");
		for (int i=0; i<buttons.size(); ++i) {
			final char c = newString.charAt(i);
			final char c0 = oldString==null ? c : oldString.charAt(i);
			final FieldButton b = buttons.get(i);
			final boolean isRecent = c!=c0 && (c==UltimatoeUtils.PLAYER_0 || c==UltimatoeUtils.PLAYER_1);
			b.setIsRecent(isRecent);
			b.setText("" + c);
			b.setEnabled(c == StandardPlayer.NOBODY.toChar());
		}

		this.game = game;
		frame.setTitle(title());
	}

	private String title() {
		String result;
		if (game.isFinished()) {
			result = game.winner().toString().replace('_', ' ') + " has won!";
		} else {
			result = game.playerOnTurn().toString().replace('_', ' ') + " to go.";
		}
		result = "Turn " + game.turn() + ", " + result;
		result += " --- " + "Tictactoe";
		return result;
	}

	private void autoplay() {
		if (game==null || game.isFinished()) return;
		autoplayInit();
		autoplayFinish();
	}

	/** Start the AI if needed. */
	private void autoplayInit() {
		if (swingWorker!=null) return;
		final GameActor actor = actors[game.playerOnTurn().ordinal()];
		if (actor==null) return;
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

	private static final int N_OF_GUI_FIELDS = 3;
	private static final Dimension BUTTON_SIZE = new Dimension(150, 150);
	private static final int MAX_AUTOPLAY_DELAY_MILLIS = 2000;
	private static final double SPEEDUP_FACTOR = 1.2;

	private static final Tictactoe INITIAL_GAME = Tictactoe.INITIAL_GAME;

	private final JFrame frame = new JFrame();
	private final JPanel controlPanel = new JPanel();
	private final JPanel mainPanel = new JPanel();
	private final FieldListener listener = new FieldListener();
	private final List<FieldButton> buttons = Lists.newArrayList();

	private final Timer timer = new Timer(10, new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) {
			autoplay();
		}
	});
	private long lastAutoplayMillis;
	private long autoplayDelayMillis;

	@NonNull private Tictactoe game;

	/** The worker currently running the AI computing the move, or null. */
	@Nullable private SwingWorker<String, Void> swingWorker;

	private final GameActor[] actors = new GameActor[2];
}
