package maaartin.game.ai.zomis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Map;

import lombok.Getter;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class GameZomisMessage {
	enum Type {
		OUT_USER("xxx username password"),
		IN_USER("x*"),
		IN_WELC("username"),
		OUT_INVT("gametype username"),
		IN_INVT("id gametype username"),
		OUT_INVR("id yesno"),
		IN_NEWG("gameid playerIndex"),
		IN_STUS("username onoffline"),
		IN_MOVE("gameid x y"),
		OUT_MOVE("gameid x y"),
		IN_FAIL("message*"),
		IN_ABORT(""),
		OUT_ABORT(""),
		IN_GEND("gameId"),
		;

		private Type(String args) {
			selector = name().replaceFirst(".*_", "");
			this.argNames = args.isEmpty() ? ImmutableList.<String>of() : ImmutableList.copyOf(args.split("\\s+"));
			allowsExtras = args.endsWith("*");
		}

		private static Type forString(String s) {
			checkNotNull(s);
			return checkNotNull(FOR_STRING.get(s), "Unexpected: " + s);
		}

		GameZomisMessage newMessage(String... args) {
			return new GameZomisMessage(this, args);
		}

		private boolean accept(ImmutableList<String> args) {
			final int diff = args.size() - argNames.size();
			return diff==0 || diff>0 && allowsExtras;
		}

		private static final Map<String, Type> FOR_STRING = Maps.newHashMap();

		static {
			for (final Type t : Type.values()) {
				if (t.name().startsWith("IN_")) FOR_STRING.put(t.selector, t);
			}
		}

		private final String selector;
		private final ImmutableList<String> argNames;
		private final boolean allowsExtras;
	}

	private GameZomisMessage(Type type, String... args) {
		this.type = type;
		this.args = ImmutableList.copyOf(args);
		checkArgument(type.accept(this.args), "Expected=[%s], got [%s]", type.argNames, this.args);
	}

	static GameZomisMessage forLine(String line) {
		final String[] split = line.trim().split("\\s+");
		final Type type = Type.forString(split[0]);
		return new GameZomisMessage(type, Arrays.copyOfRange(split, 1, split.length));
	}

	@Override public String toString() {
		return Joiner.on(' ').join(Iterables.concat(ImmutableList.of(type.selector), args));
	}

	@Getter private final Type type;
	@Getter private final ImmutableList<String> args;
}
