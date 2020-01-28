package maaartin.game.fivedown;

import static com.google.common.base.Preconditions.checkNotNull;

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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.google.common.collect.Lists;

import maaartin.game.GameActor;
import maaartin.game.GameListener;
import maaartin.game.StandardPlayer;
import maaartin.game.ai.GameMonteCarloActor;
import maaartin.game.ai.GameRandomActor;

/** The GUI for the {@link Fivedown} game. */
@Accessors(fluent=false) public final class FivedownGui implements GameListener<Fivedown> {
	private final class FieldListener implements ActionListener {
		@Override public void actionPerformed(ActionEvent e) {
			if (swingWorker!=null) return; // It's a AI turn.
			final int guiIndex = buttons.indexOf(e.getSource());
			final int x = guiIndex % WIDTH;
			final int y = guiIndex / WIDTH;
			final String move = FivedownUtils.coordinatesToMoveString(x, y);
			setState(game.play(move));
		}
	}

	@RequiredArgsConstructor private static final class FieldButton extends JButton {
		@Override protected void paintComponent(Graphics g1) {
			if (gui.game == null) return;
			final Graphics2D g = (Graphics2D) g1.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			final int w = getWidth();
			final int h = getHeight();
			if (gui.game.playerOnTurn() == StandardPlayer.PLAYER_X) {
				g.setColor(isEnabled() ? X_PLAYABLE_COLOR : X_NON_PLAYABLE_COLOR);
			} else {
				g.setColor(isEnabled() ? O_PLAYABLE_COLOR : O_NON_PLAYABLE_COLOR);
			}
			g.fillRect(0, 0, w, h);
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

		private static final Color X_COLOR = new Color(200, 0, 0);
		private static final Color O_COLOR = new Color(0, 200, 0);
		private static final Color X_PLAYABLE_COLOR = new Color(255, 240, 240);
		private static final Color X_NON_PLAYABLE_COLOR = new Color(240, 240, 255);
		private static final Color O_PLAYABLE_COLOR = new Color(240, 255, 240);
		private static final Color O_NON_PLAYABLE_COLOR = new Color(240, 240, 255);

		private final FivedownGui gui;
		@Setter private boolean isRecent;
		@Setter private StandardPlayer player;
	}

	private FivedownGui() {
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

		controlPanel.add(energyLabel);

		mainPanel.setLayout(new GridLayout(HEIGHT, WIDTH));
		for (int i=0; i<HEIGHT*WIDTH; ++i) {
			final FieldButton button = new FieldButton(this);
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
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				new FivedownGui();
			}
		});
	}

	/** Store the new state of the game and display it. */
	@Override public void setState(final Fivedown game) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				setStateInternal(game);
				autoplay();
			}
		});
	}

	private void setStateInternal(Fivedown game) {
		final String newString = game.asString().replace("\n", "");
		final String oldString = this.game==null ? null : this.game.asString().replace("\n", "");
		for (int i=0; i<buttons.size(); ++i) {
			final char c = newString.charAt(i);
			final char c0 = oldString==null ? c : oldString.charAt(i);
			final boolean hasChangedPlayer = FivedownUtils.getPlayer(c) != FivedownUtils.getPlayer(c0);
			final FieldButton b = buttons.get(i);
			b.setPlayer(FivedownUtils.getPlayer(c));
			b.setIsRecent(hasChangedPlayer);
			b.setText("" + Character.toUpperCase(c));
			b.setEnabled(FivedownUtils.isPlayable(c));
			b.repaint();
		}

		this.game = game;
		frame.setTitle(title());
		showEnergy();
	}

	private String title() {
		String result;
		if (game.isFinished()) {
			result = game.winner().toString().replace('_', ' ') + " has won!";
		} else {
			result = game.playerOnTurn().toString().replace('_', ' ') + " to go.";
		}
		result = "Turn " + game.turn() + ", " + result;
		result += " --- " + "Fivedown";
		return result;
	}

	private void showEnergy() {
		final StringBuilder sb = new StringBuilder();
		for (int i=game.getEnergy(StandardPlayer.PLAYER_X); i-->0; ) sb.append("X");
		sb.append(" ");
		for (int i=game.getEnergy(StandardPlayer.PLAYER_O); i-->0; ) sb.append("O");
		energyLabel.setText(sb.toString());
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
			checkNotNull(move);
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

	private static final int WIDTH = Fivedown.WIDTH;
	private final int HEIGHT = Fivedown.HEIGHT;

	private static final Dimension BUTTON_SIZE = new Dimension(50, 50);
	private static final int MAX_AUTOPLAY_DELAY_MILLIS = 2000;
	private static final double SPEEDUP_FACTOR = 1.2;

	private static final Fivedown INITIAL_GAME = Fivedown.INITIAL_GAME;

	private final JFrame frame = new JFrame();
	private final JPanel controlPanel = new JPanel();
	private final JLabel energyLabel = new JLabel();
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

	@NonNull private Fivedown game;

	/** The worker currently running the AI computing the move, or null. */
	@Nullable private SwingWorker<String, Void> swingWorker;

	private final GameActor[] actors = new GameActor[2];
}
