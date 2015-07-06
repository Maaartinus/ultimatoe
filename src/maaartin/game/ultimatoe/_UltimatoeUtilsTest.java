package maaartin.game.ultimatoe;

import junit.framework.TestCase;

public final class _UltimatoeUtilsTest extends TestCase {
	public void testIndexesToMoveString() {
		for (int majorIndex=0; majorIndex<9; ++majorIndex) {
			for (int minorIndex=0; minorIndex<9; ++minorIndex) {
				final String move = UltimatoeUtils.indexesToMoveString(majorIndex, minorIndex);
				assertEquals(majorIndex, UltimatoeUtils.stringToMajorIndex(move));
				assertEquals(minorIndex, UltimatoeUtils.stringToMinorIndex(move));
			}
		}
	}
}
