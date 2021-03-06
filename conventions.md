While I stick mostly with Java conventions, I differ in the following points:

- Line length 160 rather than 80 as many wrapped lines are about the worse what you can do to readability.

- Single line conditional statements (if it's short and simple and fits in the line).

- Left out spaces between *innermost* operators and simple expressions like `a*b + c*d` (but not `a*b+c*d` and *definitely not* `a * b+c * d`).

*I wouldn't do it if it wasn't much more readable for me.*

Fields get placed at the bottom rather than somewhere in the middle (i.e., between inner classes and constructors).
*Again, I wouldn't do it if it wouldn't make them easier to find.*
