Currently, there's an immutable fast game state representation (`UltimatoeState`) and an ugly GUI (`UltimatoeGui`)
allowing to play against a stupid UI (`UltimatoeRandomActor` playing "O" and/or `UltimatoeMonteCarloActor` playing "X").

To run it, all you need is Gradle.

Use

    gradle game

to play or

    gradle match

to let the (stupid) AI run against another (stupid) AI.
