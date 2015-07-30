package maaartin.game.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import lombok.experimental.Delegate;

import maaartin.game.GameActor;
import maaartin.game.ai.GameMonteCarloActor;
import maaartin.game.ai.GameRandomActor;
import maaartin.game.ai.zomis.GameZomisActor;

public class ActorChooser extends JPanel implements GameActor {
	public ActorChooser(GameActor initialActor, boolean isSecond) {
		this.initialActor = initialActor;
		this.isSecond = isSecond;
		delegateActor = initialActor;
		add(comboBox);
		model.addElement("HUMAN");
		model.addElement("RANDOM");
		model.addElement("MCTS");
		model.addElement("ZONIS_Idiot");
		model.addElement("ZONIS_Imp3");
		model.addElement("ZONIS_Latest");
		model.addElement("ZONIS_V3");
		comboBox.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				final Object selectedItem = model.getSelectedItem();
				SwingUtilities.invokeLater(new Runnable() {
					@Override public void run() {
						delegateActor = toActor((String) selectedItem);
					}
				});
			}
		});
		model.setSelectedItem(!isSecond ? "HUMAN" : "MCTS");
	}

	private GameActor toActor(String selection) {
		switch (selection) {
			case "HUMAN": return initialActor;
			case "RANDOM": return new GameRandomActor();
			case "MCTS": return new GameMonteCarloActor();
		}
		if (selection.startsWith(ZONIS_PREFIX)) {
			final String partnerName = "#AI_UTTT_" + selection.substring(ZONIS_PREFIX.length());
			return new GameZomisActor(false, partnerName, isSecond);
		}
		throw new IllegalArgumentException("Unknown:" + selection);
	}

	public boolean isHuman() {
		return "HUMAN".equals(model.getSelectedItem());
	}

	private static final String ZONIS_PREFIX = "ZONIS_";

	private final GameActor initialActor;
	private final boolean isSecond;

	@Delegate private GameActor delegateActor;
	private final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
	private final JComboBox<String> comboBox = new JComboBox<String>(model);
}
