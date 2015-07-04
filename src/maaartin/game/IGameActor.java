package maaartin.game;

import javax.annotation.Nullable;

public interface IGameActor<T> {
	@Nullable String selectMove(T state);
}
