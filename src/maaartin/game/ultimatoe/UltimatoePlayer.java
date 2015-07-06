package maaartin.game.ultimatoe;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import maaartin.game.GamePlayer;

//@Deprecated//TODO
@RequiredArgsConstructor public enum UltimatoePlayer implements GamePlayer<Ultimatoe> {
	O('O'), X('X'), NONE('.');

	@Override public boolean isNone() {
		return this == NONE;
	}

	@Getter private final char toChar;
}
