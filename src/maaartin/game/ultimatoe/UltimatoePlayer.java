package maaartin.game.ultimatoe;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import maaartin.game.GamePlayer;

@RequiredArgsConstructor public enum UltimatoePlayer implements GamePlayer<Ultimatoe> {
	PLAYER_X('X'), PLAYER_O('O'), NOBODY(' ');

	@Override public boolean isDummy() {
		return this == NOBODY;
	}

	@Getter private final char toChar;
}
