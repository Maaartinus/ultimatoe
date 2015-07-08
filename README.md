Currently, there's an immutable fast game state representation (`Ultimatoe`) and an ugly GUI (`UltimatoeGui`)
allowing to play against a stupid UI (`GameRandomActor` playing "O" and/or `GameMonteCarloActor` playing "X").

---

To run it, all you need is Git and Gradle.

Download it via

    git clone https://github.com/Maaartinus/ultimatoe.git && cd ultimatoe

and start a game via

    gradle game

---

**Before reviewing, please read about my [slightly differing coding conventions](https://github.com/Maaartinus/ultimatoe/blob/master/conventions.md).**

---

Those willing to tune the stupid AI can use

    gradle match

to let the (stupid) AI run against another (stupid) AI.
