package maaartin.game.ultimatoe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
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

import maaartin.game.GameActor;
import maaartin.game.GameListener;
import maaartin.game.ai.GameMonteCarloActor;
import maaartin.game.ai.GameRandomActor;

/** The GUI for the {@link Ultimatoe} game. */
public final class UltimatoeGui implements GameListener<Ultimatoe> {
	private final class FieldListener implements ActionListener {
		@Override public void actionPerformed(ActionEvent e) {
			if (swingWorker!=null) return; // It's a AI turn.
			final int guiIndex = buttons.indexOf(e.getSource());
			final int guiX = guiIndex % N_OF_GUI_FIELDS;
			final int guiY = guiIndex / N_OF_GUI_FIELDS;
			// Convert the gui coordinates to game cooredinates in range 0..8.
			final int x = guiX - guiX/4;
			final int y = guiY - guiY/4;
			final String move = UltimatoeUtils.coordinatesToMoveString(x, y);
			setState(game.play(move));
		}
	}

	private final class FieldButton extends JButton {
		@Override protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			final String text = getText();
			if (text.isEmpty() || text.charAt(0) != UltimatoeUtils.BORDER) return;
			setForeground(new Color(0x20FFC0C0, true));
			g.fillRect(0, 0, getWidth(), getHeight());
		}
	}

	private UltimatoeGui() {
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
				final boolean isX = b.getText().endsWith("X");
				if (isX) {
					actors[1] = isOn ? new GameMonteCarloActor<>(INITIAL_GAME) : null;
				} else {
					actors[0] = isOn ? new GameRandomActor<>(INITIAL_GAME) : null;
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

		setState(INITIAL_GAME);
		frame.setVisible(true);
		timer.start();
	}

	public static void main(String[] args) {
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
		final String newString = game.asString().replace("\n", "");
		final String oldString = this.game==null ? null : this.game.toString().replace("\n", "");
		for (int i=0; i<buttons.size(); ++i) {
			final char c = newString.charAt(i);
			if (oldString != null && c == oldString.charAt(i)) continue;
			final JButton b = buttons.get(i);
			b.setForeground(Color.BLACK);
			b.setText("" + c);
			b.setEnabled(c == UltimatoeUtils.PLAYABLE);
			if (c == UltimatoeUtils.BORDER) b.setForeground(Color.DARK_GRAY);
		}

		this.game = game;
		frame.setTitle(title());
	}

	private String title() {
		String result;
		if (game.isFinished()) {
			result = "\"" + UltimatoeUtils.scoreToWinner(game.score()) + "\"" + " has won!";
		} else {
			result = "\"" + game.playerOnTurn().toString() + "\"" + " to go.";
		}
		result = "Turn " + game.turn() + ", " + result;
		result += " --- " + "Ultimatoe";
		return result;
	}

	private void autoplay() {
		if (game.isFinished()) return;
		autoplayInit();
		autoplayFinish();
	}

	/** Start the AI if needed. */
	private void autoplayInit() {
		if (swingWorker!=null) return;
		final GameActor<Ultimatoe> actor = actors[game.playerOnTurn().ordinal()];
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

	private SwingWorker<String, Void> apply(final GameActor<Ultimatoe> actor) {
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
	private final FieldListener listener = new FieldListener();
	private final List<JButton> buttons = Lists.newArrayList();

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

	@SuppressWarnings("unchecked")
	private final GameActor<Ultimatoe>[] actors = (GameActor<Ultimatoe>[]) new GameActor<?>[2];
}
