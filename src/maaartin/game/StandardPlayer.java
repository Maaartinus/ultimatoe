package maaartin.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableList;


@RequiredArgsConstructor public enum StandardPlayer implements GamePlayer {
	PLAYER_X('X'), PLAYER_O('O'), NOBODY(' ');

	@Override public boolean isDummy() {
		return this == NOBODY;
	}

	public static final ImmutableList<GamePlayer> PLAYERS = ImmutableList.<GamePlayer>copyOf(values());

	@Getter private final char toChar;
}
