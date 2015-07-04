package maaartin.game.ultimatoe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import maaartin.game.IGameActor;

public final class UltimatoeGui {
	private final class MyListener implements ActionListener {
		@Override public void actionPerformed(ActionEvent e) {
			final Object source = e.getSource();
			final int totalIndex = buttons.indexOf(source);
			final int x = totalIndex % 11;
			final int y = totalIndex / 11;
			final int majorIndex = (x/4) + 3 * (y/4);
			final int minorIndex = (x%4) + 3 * (y%4);
			setState(state.play(majorIndex, minorIndex));
		}
	}

	private final class MyButton extends JButton {
		@Override protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			final String text = getText();
			if (text.isEmpty() || text.charAt(0) != UltimatoeFieldState.BORDER.toChar()) return;
			setForeground(new Color(0x20FFC0C0, true));
			g.fillRect(0, 0, getWidth(), getHeight());
		}
	}

	private UltimatoeGui() {
		final ActionListener autoListener = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				final JToggleButton b = (JToggleButton) e.getSource();
				final boolean isOn = b.getModel().isSelected();
				final boolean isX = b.getText().endsWith("X");
				if (isX) {
					actorX = isOn ? new UltimatoeMonteCarloActor() : null;
				} else {
					actorO = isOn ? new UltimatoeRandomActor() : null;
				}

				final boolean isFullAuto = actorX!=null && actorO!=null;
				fastButton.setEnabled(isFullAuto);
				fasterButton.setEnabled(isFullAuto);
				fastButton.getModel().setSelected(false);
				fasterButton.getModel().setSelected(false);
				autoplay();
			}
		};
		for (final String s : "auto-X auto-O".split(" ")) {
			final JToggleButton autoButton = new JToggleButton(s);
			autoButton.addActionListener(autoListener);
			controlPanel.add(autoButton);
		}
		fastButton.setEnabled(false);
		fasterButton.setEnabled(false);
		controlPanel.add(fastButton);
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

		setState(UltimatoeState.EMPTY_STATE);
		frame.setVisible(true);
		timer.start();
	}

	public static void main(String[] args) {
		new UltimatoeGui();
	}

	private void setState(final UltimatoeState state) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				setStateInternal(state);
				autoplay();
			}
		});
	}

	private void setStateInternal(UltimatoeState state) {
		final String newString = state.toString().replace("\n", "");
		final String oldString = this.state==null ? Strings.repeat("?", newString.length()) : this.state.toString().replace("\n", "");
		for (int i=0; i<buttons.size(); ++i) {
			final char c = newString.charAt(i);
			if (c == oldString.charAt(i)) continue;
			final JButton b = buttons.get(i);
			b.setForeground(Color.BLACK);
			b.setText("" + c);
			b.setEnabled(c == UltimatoeFieldState.PLAYABLE.toChar());
			if (c == UltimatoeFieldState.BORDER.toChar()) b.setForeground(Color.DARK_GRAY);
		}
		frame.setTitle("Player on turn: " + state.playerOnTurn());
		if (state.isFinished()) frame.setTitle("*** The winner is: " + state.winner());

		this.state = state;
	}

	private void autoplay() {
		if (state.isFinished()) return;
		if (busy) return;
		final IGameActor<UltimatoeState> actor = state.playerOnTurn() == UltimatoePlayer.X ? actorX : actorO;
		if (actor==null) return;
		if (System.currentTimeMillis() < lastAutoplayMillis + delayMillis()) return;
		busy = true;

		apply(actor).execute();
	}

	private SwingWorker<Void, Void> apply(final IGameActor<UltimatoeState> actor) {
		return new SwingWorker<Void, Void>() {
			@Override protected Void doInBackground() throws Exception {
				setState(state.play(actor.selectMove(state)));
				lastAutoplayMillis = System.currentTimeMillis();
				return null;
			}

			@Override protected void done() {
				busy = false;
			}
		};
	}

	private long delayMillis() {
		final int index = (fastButton.getModel().isSelected() ? 1 : 0) + (fasterButton.getModel().isSelected() ? 2 : 0);
		return delaysMillis[3-index];
	}

	private static final Dimension BUTTON_SIZE = new Dimension(50, 50);
	private static final long[] delaysMillis = {20, 200, 800, 2000};


	private final JFrame frame = new JFrame();
	private final JPanel controlPanel = new JPanel();
	private final JPanel mainPanel = new JPanel();
	private final MyListener listener = new MyListener();
	private final List<JButton> buttons = Lists.newArrayList();

	private final JToggleButton fastButton = new JToggleButton("fast");
	private final JToggleButton fasterButton = new JToggleButton("faster");

	private final Timer timer = new Timer(10, new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) {
			autoplay();
		}
	});
	private long lastAutoplayMillis;

	private UltimatoeState state;

	private boolean busy;
	private IGameActor<UltimatoeState> actorX;
	private IGameActor<UltimatoeState> actorO;
}
