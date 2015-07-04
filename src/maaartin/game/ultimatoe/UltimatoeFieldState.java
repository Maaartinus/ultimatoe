package maaartin.game.ultimatoe;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor public enum UltimatoeFieldState {
	BORDER('*'), O('O'), X('X'), PLAYABLE('·'), NON_PLAYABLE(' ');

	@Getter private final char toChar;
}
