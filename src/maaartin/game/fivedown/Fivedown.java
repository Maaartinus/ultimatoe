package maaartin.game.fivedown;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Random;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableBiMap;

import maaartin.game.GamePlayer;
import maaartin.game.StandardGame;
import maaartin.game.StandardPlayer;

@RequiredArgsConstructor(access=AccessLevel.PRIVATE) @EqualsAndHashCode(callSuper=false)
public final class Fivedown extends StandardGame<Fivedown> {
	@Override public boolean isFinished() {
		return !winner.isDummy() || turn >= WIDTH * HEIGHT && isFull();
	}

	private boolean isFull() {
		for (final int x : data) {
			if (Integer.bitCount(x) < WIDTH) return false;
		}
		return true;
	}

	@Override public ImmutableBiMap<Fivedown, String> children() {
		final ImmutableBiMap.Builder<Fivedown, String> result = ImmutableBiMap.builder();
		for (int y=0; y<HEIGHT; ++y) {
			for (int x=0; x<WIDTH; ++x) {
				if (isPlayable(x, y)) result.put(play(x, y), FivedownUtils.coordinatesToMoveString(x, y));
			}
		}
		return result.build();
	}

	@Override public Fivedown play(String move) {
		checkNotNull(move);
		return play(FivedownUtils.stringToX(move), FivedownUtils.stringToY(move));
	}

	private Fivedown play(int x, int y) {
		checkState(!isFinished());
		checkArgument(areInBounds(x, y));
		checkArgument(isPlayable(x, y));

		final int[] data = this.data.clone();
		if (isEmpty(x, y)) {
			data[y] |= (playerOnTurn().ordinal() + 1) << (2*x);
		} else {
			final int playerInternal = getPlayerInternal(x, y);
			data[y] ^= playerInternal << (2*x);
			for (int y2=y; ; ++y2) {
				if (y2!=HEIGHT-1 && isEmpty(x, y2+1)) continue;
				data[y2] ^= playerInternal << (2*x);
				break;
			}
		}

		final boolean needsBaloon = needsBaloon(x, y);
		final int consumedEnergy = isEmpty(x, y) ? needsBaloon ? ENERGY_FOR_BALOON : ENERGY_FOR_NORMAL : ENERGY_FOR_LANDING;
		final int newEnergy = playerOnTurnEnergy - consumedEnergy;
		final GamePlayer winner = isWinningTurn(x, y) ? playerOnTurn() : this.winner;

		return new Fivedown(turn+1, winner, otherPlayerEnergy, newEnergy, data);
	}

	private boolean areInBounds(int x, int y) {
		return 0<=x && x<WIDTH && 0<=y && y<HEIGHT;
	}

	private boolean isWinningTurn(int x, int y) {
		final int playerInternal = playerOnTurn().ordinal() + 1;
		if (sequenceLength(playerInternal, x, y, 1, 0) >= WINNING_SEQUENCE_LENGTH) return true;
		if (sequenceLength(playerInternal, x, y, 0, 1) >= WINNING_SEQUENCE_LENGTH) return true;
		if (sequenceLength(playerInternal, x, y, 1, 1) >= WINNING_SEQUENCE_LENGTH) return true;
		if (sequenceLength(playerInternal, x, y, 1, -1) >= WINNING_SEQUENCE_LENGTH) return true;
		return false;
	}

	private int sequenceLength(int playerInternal, int x, int y, int dx, int dy) {
		return 1 + sequenceLength1(playerInternal, x, y, dx, dy) + sequenceLength1(playerInternal, x, y, -dx, -dy);
	}

	private int sequenceLength1(int playerInternal, int x, int y, int dx, int dy) {
		int result = 0;
		while (true) {
			x += dx;
			y += dy;
			if (!areInBounds(x, y)) break;
			if (getPlayerInternal(x, y) != playerInternal) break;
			++result;
		}
		return result;
	}

	private boolean isPlayable(int x, int y) {
		if (isFinished()) return false;
		final boolean needsBaloon = needsBaloon(x, y);
		final int playerInternal = getPlayerInternal(x, y);
		if (playerInternal==0) {
			if (playerOnTurnEnergy<ENERGY_FOR_BALOON && needsBaloon) return false;
		} else if (playerInternal == playerOnTurn().ordinal() + 1) {
			if (!needsBaloon) return false;
		} else {
			return false;
		}
		return true;
	}

	private boolean needsBaloon(int x, int y) {
		return y != data.length-1 && isEmpty(x, y+1);
	}

	private boolean isEmpty(int x, int y) {
		return getPlayerInternal(x, y) == 0;
	}

	private int getPlayerInternal(int x, int y) {
		return (data[y] >> (2*x)) & 3;
	}


	@Override public Fivedown play(Random random) {
		while (true) {
			final int x = random.nextInt(WIDTH);
			final int y = random.nextInt(HEIGHT);
			if (isPlayable(x, y)) return play(x, y);
		}
	}

	@Override public String asString() {
		final StringBuilder result = new StringBuilder();
		for (int y=0; y<HEIGHT; ++y) {
			if (y>0) result.append("\n");
			for (int x=0; x<WIDTH; ++x) result.append(getCharFor(x, y));
		}
		return result.toString();
	}

	private char getCharFor(int x, int y) {
		final GamePlayer p = PLAYERS_INTERNAL[getPlayerInternal(x, y)];
		return FivedownUtils.toChar(p, isPlayable(x, y));
	}

	int getEnergy(GamePlayer player) {
		checkArgument(player==StandardPlayer.PLAYER_O || player==StandardPlayer.PLAYER_X);
		return player == playerOnTurn() ? playerOnTurnEnergy : otherPlayerEnergy;
	}

	static final int WIDTH = 15;
	static final int HEIGHT = 8;

	private static final int WINNING_SEQUENCE_LENGTH = 5;
	private static final int ENERGY_FOR_LANDING = -3;
	private static final int ENERGY_FOR_NORMAL = -1;
	private static final int ENERGY_FOR_BALOON = 2;

	private static final GamePlayer[] PLAYERS_INTERNAL =
		{StandardPlayer.NOBODY, StandardPlayer.PLAYER_X, StandardPlayer.PLAYER_O};

	public static final Fivedown INITIAL_GAME = new Fivedown(0, StandardPlayer.NOBODY, 0, 5, new int[HEIGHT]);

	@Getter private final int turn;
	@Getter @NonNull private final GamePlayer winner;
	private final int playerOnTurnEnergy;
	private final int otherPlayerEnergy;
	@NonNull private final int[] data;
}
