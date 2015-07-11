package maaartin.game.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.google.common.collect.Maps;

public enum GameCommand {
	USER_NAME, // userName
	GAME_NAME, // gameName
	PLAYER_INDEX, // "X" or "O"
	POST, // any data
	;

	static GameCommand forString(String s) {
		return checkNotNull(FOR_STRING.get(s));
	}

	private static final Map<String, GameCommand> FOR_STRING = Maps.newHashMap();
	static {
		for (final GameCommand c : GameCommand.values()) FOR_STRING.put(c.name(), c);
	}
}
