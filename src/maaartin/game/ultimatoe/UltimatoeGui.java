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

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import maaartin.game.GameActor;
import maaartin.game.GameListener;
import maaartin.game.ai.GameMonteCarloActor;
import maaartin.game.ai.GameRandomActor;

public final class UltimatoeGui implements GameListener<Ultimatoe> {
	private final class MyListener implements ActionListener {
		@Override public void actionPerformed(ActionEvent e) {
			if (swingWorker!=null) return;
			final int guiIndex = buttons.indexOf(e.getSource());
			final int guiX = guiIndex % 11;
			final int guiY = guiIndex / 11;
			final int x = guiX - guiX/4;
			final int y = guiY - guiY/4;
			final String move = UltimatoeUtils.coordinatesToMoveString(x, y);
			setState(game.play(move));
		}
	}

	private final class MyButton extends JButton {
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
				delayMillis *= 0.75;
			}
		});

		final ActionListener autoListener = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				final JToggleButton b = (JToggleButton) e.getSource();
				final boolean isOn = b.getModel().isSelected();
				final boolean isX = b.getText().endsWith("X");
				if (isX) {
					actors[1] = isOn ? new GameMonteCarloActor<>(EMPTY_STATE) : null;
				} else {
					actors[0] = isOn ? new GameRandomActor<>(EMPTY_STATE) : null;
				}

				final boolean isFullAuto = actors[0]!=null && actors[1]!=null;
				fasterButton.setEnabled(isFullAuto);
				delayMillis = MAX_DELAY_MILLIS;
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

		mainPanel.setLayout(new GridLayout(11, 11));
		for (int i=0; i<11*11; ++i) {
			final MyButton button = new MyButton();
			button.setPreferredSize(BUTTON_SIZE);
			button.addActionListener(listener);
			buttons.add(button);
			mainPanel.add(button);
		}

		frame.add(controlPanel, BorderLayout.NORTH);
		frame.add(mainPanel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setState(EMPTY_STATE);
		frame.setVisible(true);
		timer.start();
	}

	public static void main(String[] args) {
		new UltimatoeGui();
	}

	@Override public void setState(final Ultimatoe game) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				setStateInternal(game);
				autoplay();
			}
		});
	}

	private void setStateInternal(Ultimatoe game) {
		final String newString = game.toString().replace("\n", "");
		final String oldString = this.game==null ? Strings.repeat("?", newString.length()) : this.game.toString().replace("\n", "");
		for (int i=0; i<buttons.size(); ++i) {
			final char c = newString.charAt(i);
			if (c == oldString.charAt(i)) continue;
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

	private void autoplayInit() {
		if (swingWorker!=null) return;
		final GameActor<Ultimatoe> actor = actors[game.playerOnTurn().ordinal()];
		if (actor==null) return;
		swingWorker = apply(actor);
		swingWorker.execute();
	}

	private void autoplayFinish() {
		if (swingWorker==null) return;
		if (!swingWorker.isDone()) return;
		if (System.currentTimeMillis() < lastAutoplayMillis + delayMillis) return;
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

	private static final Dimension BUTTON_SIZE = new Dimension(50, 50);
	private static final int MAX_DELAY_MILLIS = 2000;

	private static final Ultimatoe EMPTY_STATE = Ultimatoe.INITIAL_GAME;

	private final JFrame frame = new JFrame();
	private final JPanel controlPanel = new JPanel();
	private final JPanel mainPanel = new JPanel();
	private final MyListener listener = new MyListener();
	private final List<JButton> buttons = Lists.newArrayList();

	private final Timer timer = new Timer(10, new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) {
			autoplay();
		}
	});
	private long lastAutoplayMillis;
	private long delayMillis;

	private Ultimatoe game;

	private SwingWorker<String, Void> swingWorker;
	@SuppressWarnings("unchecked")
	private final GameActor<Ultimatoe>[] actors = (GameActor<Ultimatoe>[]) new GameActor<?>[2];
}
