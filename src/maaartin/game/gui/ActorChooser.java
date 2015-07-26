package maaartin.game.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.experimental.Delegate;

import maaartin.game.GameActor;
import maaartin.game.ai.GameMonteCarloActor;
import maaartin.game.ai.GameRandomActor;
import maaartin.game.ai.zomis.GameZomisActor;

import de.grajcar.dout.Dout;

public class ActorChooser extends JPanel implements GameActor {
	public ActorChooser(GameActor initialActor, boolean isSecond) {
		this.initialActor = initialActor;
		this.isSecond = isSecond;
		delegateActor = initialActor;
		add(comboBox);
		model.addElement("HUMAN");
		model.addElement("RANDOM");
		model.addElement("MCTS");
		model.addElement("ZONIS");
		comboBox.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				final Object selectedItem = model.getSelectedItem();
				delegateActor = toActor((String) selectedItem);
				Dout.a(delegateActor);
			}
		});
		//		model.setSelectedItem(!isSecond ? "MCTS" : "ZONIS");
		model.setSelectedItem(!isSecond ? "HUMAN" : "ZONIS");
	}

	private GameActor toActor(String selection) {
		switch (selection) {
			case "HUMAN": return initialActor;
			case "RANDOM": return new GameRandomActor();
			case "MCTS": return new GameMonteCarloActor();
			case "ZONIS": return new GameZomisActor(false, "maaartinus-" + (isSecond ? 1 : 0), isSecond);
		}
		throw new IllegalArgumentException("Unknown:" + selection);
	}

	public boolean isHuman() {
		return "HUMAN".equals(model.getSelectedItem());
	}

	private final GameActor initialActor;
	private final boolean isSecond;

	@Delegate private GameActor delegateActor;
	private final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
	private final JComboBox<String> comboBox = new JComboBox<String>(model);
}
