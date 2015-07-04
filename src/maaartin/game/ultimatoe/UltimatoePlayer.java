package maaartin.game.ultimatoe;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor public enum UltimatoePlayer {
	NONE('.'), O('O'), X('X');

	boolean isNone() {
		return this == NONE;
	}

	@Getter private final char toChar;
}
