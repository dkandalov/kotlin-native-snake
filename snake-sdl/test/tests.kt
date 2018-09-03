import Direction.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class SnakeTests {
    private val snake = Snake(
        cells = listOf(Cell(2, 0), Cell(1, 0), Cell(0, 0)),
        direction = right
    )

    @Test fun `snake moves right`() {
        assertEquals(
            actual = snake.move(),
            expected = Snake(
                cells = listOf(Cell(3, 0), Cell(2, 0), Cell(1, 0)),
                direction = right
            )
        )
    }

    @Test fun `snake changes direction`() {
        assertEquals(
            actual = snake.turn(down).move(),
            expected = Snake(
                cells = listOf(Cell(2, 1), Cell(2, 0), Cell(1, 0)),
                direction = down
            )
        )
        assertEquals(
            actual = snake.turn(left).move(),
            expected = Snake(
                cells = listOf(Cell(3, 0), Cell(2, 0), Cell(1, 0)),
                direction = right
            )
        )
    }

    @Test fun `snake eats an apple`() {
        val apples = Apples(20, 10, cells = setOf(Cell(2, 0)))

        val (newSnake, newApples) = snake.eat(apples)

        assertEquals(
            actual = newApples.cells,
            expected = emptySet()
        )
        assertEquals(
            actual = newSnake.eatenApples,
            expected = 1
        )
        assertEquals(
            actual = newSnake.move().cells,
            expected = listOf(Cell(3, 0), Cell(2, 0), Cell(1, 0), Cell(0, 0))
        )
    }
}

class ApplesTests {
    @Test fun `apples grow at random locations`() {
        Random.seed = 42
        val apples = Apples(fieldWidth = 20, fieldHeight = 10)

        assertEquals(
            actual = apples.grow().grow().grow().cells,
            expected = setOf(Cell(x = 8, y = 4), Cell(x = 5, y = 5))
        )
    }
}