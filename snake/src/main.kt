import Direction.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.memScoped
import platform.osx.*
import kotlin.math.max
import kotlin.random.Random

fun main(args: Array<String>) = memScoped {
    initscr()
    defer { endwin() }
    noecho()
    curs_set(0)
    halfdelay(2)

    var game = Game(
        width = 20,
        height = 10,
        snake = Snake(
            cells = listOf(Cell(4, 0), Cell(3, 0), Cell(2, 0), Cell(1, 0), Cell(0, 0)),
            direction = right
        )
    )

    val window = newwin(game.height + 2, game.width + 2, 0, 0)
    defer { delwin(window) }

    var c = 0
    while (c.toChar() != 'q') {
        game.draw(window)

        c = wgetch(window)
        val direction = when (c.toChar()) {
            'i' -> up
            'j' -> left
            'k' -> down
            'l' -> right
            else -> null
        }

        game = game.update(direction)
    }
}

fun Game.draw(window: CPointer<WINDOW>?) {
    wclear(window)
    box(window, 0, 0)

    apples.cells.forEach { mvwprintw(window, it.y + 1, it.x + 1, ".") }
    snake.tail.forEach { mvwprintw(window, it.y + 1, it.x + 1, "o") }
    snake.head.let { mvwprintw(window, it.y + 1, it.x + 1, "Q") }

    if (isOver) {
        mvwprintw(window, 0, 4, "Game is Over")
        mvwprintw(window, 1, 3, "Your score is $score")
    }

    wrefresh(window)
}

data class Game(
    val width: Int,
    val height: Int,
    val snake: Snake,
    val apples: Apples = Apples(width, height)
) {
    val isOver =
        snake.tail.contains(snake.head) ||
        snake.cells.any { it.x < 0 || it.x >= width || it.y < 0 || it.y >= height }

    val score = snake.cells.size

    fun update(direction: Direction?): Game {
        if (isOver) return this
        val (newSnake, newApples) = snake.turn(direction).move().eat(apples.grow())
        return copy(snake = newSnake, apples = newApples)
    }
}

data class Snake(
    val cells: List<Cell>,
    val direction: Direction,
    val eatenApples: Int = 0
) {
    val head = cells.first()
    val tail = cells.subList(1, cells.size)

    fun move(): Snake {
        val newHead = head.move(direction)
        val newTail = if (eatenApples == 0) cells.dropLast(1) else cells
        return copy(
            cells = listOf(newHead) + newTail,
            eatenApples = max(eatenApples - 1, 0)
        )
    }

    fun turn(newDirection: Direction?): Snake {
        if (newDirection == null || newDirection.isOpposite(direction)) return this
        return copy(direction = newDirection)
    }

    fun eat(apples: Apples): Pair<Snake, Apples> {
        if (!apples.cells.contains(head)) return Pair(this, apples)
        return Pair(
            copy(eatenApples = eatenApples + 1),
            apples.copy(cells = apples.cells - head)
        )
    }
}

data class Apples(
    val fieldWidth: Int,
    val fieldHeight: Int,
    val cells: List<Cell> = emptyList(),
    val growthSpeed: Int = 3,
    val random: Random = Random
) {
    fun grow(): Apples {
        if (random.nextInt(growthSpeed) != 0) return this
        return copy(cells = cells + Cell(random.nextInt(fieldWidth), random.nextInt(fieldHeight)))
    }
}

data class Cell(val x: Int, val y: Int) {
    fun move(direction: Direction) = Cell(x + direction.dx, y + direction.dy)
}

enum class Direction(val dx: Int, val dy: Int) {
    up(0, -1), down(0, 1), left(-1, 0), right(1, 0);

    fun isOpposite(that: Direction) = dx + that.dx == 0 && dy + that.dy == 0
}