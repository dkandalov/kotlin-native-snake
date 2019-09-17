import Direction.*
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
}