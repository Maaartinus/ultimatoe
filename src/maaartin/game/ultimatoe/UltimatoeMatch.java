package maaartin.game.ultimatoe;

import maaartin.game.ai.GameMatch;

public class UltimatoeMatch extends GameMatch<Ultimatoe> {
	private UltimatoeMatch() {
		super(Ultimatoe.INITIAL_GAME);
	}

	public static void main(String[] args) {
		System.out.println("Running two Monte Carlos against each other (endless loop).");
		new UltimatoeMatch().run();
	}
}
