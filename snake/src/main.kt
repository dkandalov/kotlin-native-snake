@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

import Direction.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.memScoped
import ncurses.*
import kotlin.random.Random

fun main(): Unit = memScoped {
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

    val window = newwin(game.height + 2, game.width + 2, 0, 0)!!
    defer { delwin(window) }

    var input = 0
    while (input.toChar() != 'q') {
        window.draw(game)

        input = wgetch(window)
        val direction = when (input.toChar()) {
            'i'  -> up
            'j'  -> left
            'k'  -> down
            'l'  -> right
            else -> null
        }

        game = game.update(direction)
    }
}

private fun CPointer<WINDOW>.draw(game: Game) {
    wclear(this)
    box(this, 0u, 0u)

    game.apples.cells.forEach { mvwprintw(this, it.y + 1, it.x + 1, ".") }
    game.snake.tail.forEach { mvwprintw(this, it.y + 1, it.x + 1, "o") }
    game.snake.head.let { mvwprintw(this, it.y + 1, it.x + 1, "Q") }
    if (game.isOver) {
        mvwprintw(this, 0, 6, "Game Over")
        mvwprintw(this, 1, 3, "Your score is ${game.score}")
    }

    wrefresh(this)
}

data class Game(
    val width: Int,
    val height: Int,
    val snake: Snake,
    val apples: Apples = Apples(width, height)
) {
    val score = snake.cells.size
    val isOver =
        snake.tail.contains(snake.head) ||
        snake.cells.any {
            it.x !in 0.until(width) || it.y !in 0.until(height)
        }

    fun update(direction: Direction?): Game {
        if (isOver) return this

        val (newSnake, newApples) = snake
            .turn(direction)
            .move()
            .eat(apples.grow())

        return copy(snake = newSnake, apples = newApples)
    }
}

data class Apples(
    val width: Int,
    val height: Int,
    val cells: Set<Cell> = emptySet(),
    val growthSpeed: Int = 3,
    val random: Random = Random
) {
    fun grow(): Apples {
        if (random.nextInt(10) >= growthSpeed) return this
        return copy(cells = cells + Cell(random.nextInt(width), random.nextInt(height)))
    }
}

data class Snake(
    val cells: List<Cell>,
    val direction: Direction,
    val eatenApples: Int = 0
) {
    val head = cells.first()
    val tail = cells.drop(1)

    fun move(): Snake {
        val newHead = head.move(direction)
        val newTail = if (eatenApples == 0) cells.dropLast(1) else cells
        return copy(
            cells = listOf(newHead) + newTail,
            eatenApples = maxOf(0, eatenApples - 1)
        )
    }

    fun turn(newDirection: Direction?) =
        if (newDirection == null || newDirection.isOppositeTo(direction)) this
        else copy(direction = newDirection)

    fun eat(apples: Apples): Pair<Snake, Apples> {
        if (head !in apples.cells) return Pair(this, apples)
        return Pair(
            copy(eatenApples = eatenApples + 1),
            apples.copy(cells = apples.cells - head)
        )
    }
}

data class Cell(val x: Int, val y: Int) {
    fun move(direction: Direction) =
        Cell(x + direction.dx, y + direction.dy)
}

enum class Direction(val dx: Int, val dy: Int) {
    // -->
    // |
    // v
    up(0, -1),
    down(0, 1),
    left(-1, 0),
    right(1, 0);

    fun isOppositeTo(that: Direction) =
        dx + that.dx == 0 && dy + that.dy == 0
}