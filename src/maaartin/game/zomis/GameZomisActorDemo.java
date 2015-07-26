package maaartin.game.zomis;

import maaartin.game.ultimatoe.UltimatoeGui;

import de.grajcar.lang.Autokill;

public class GameZomisActorDemo {
	public static void main(String[] args) {
		new Autokill(123456);
		final GameZomisActorOld actor0 = new GameZomisActorOld(ACTOR_0, false);
		final GameZomisActorOld actor1 = new GameZomisActorOld(ACTOR_1, true);
		new Thread(actor0).start();
		new Thread(actor1).start();
		actor0.setListener(new UltimatoeGui());
		actor1.setListener(new UltimatoeGui());
		//		actor0.awaitPartner(1000, ACTOR_1);
		//		actor0.invite(ACTOR_1);
		//		actor0.awaitGame(1000);
		//		actor1.awaitGame(1000);
		//		actor0.awaitGame(1000);
		//		actor0.play("12");
	}

	private void go() {

	}



	public static final String ACTOR_0 = "maaartinus-0";
	public static final String ACTOR_1 = "maaartinus-1";
}
