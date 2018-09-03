import Direction.*
import cnames.structs.SDL_Renderer
import cnames.structs.SDL_Texture
import cnames.structs.SDL_Window
import kotlinx.cinterop.*
import platform.posix.R_OK
import platform.posix.access
import sdl.*
import kotlin.math.max
import kotlin.random.Random

class SdlUI(width: Int, height: Int) {
    private val arena = Arena()
    private val window: CPointer<SDL_Window>
    private val renderer: CPointer<SDL_Renderer>
    private val font: Font
    private val sprites: Sprites

    private val pixelWidth = width * Sprites.w
    private val pixelHeight = height * Sprites.h

    init {
        if (SDL_Init(SDL_INIT_EVERYTHING) != 0) {
            println("SDL_Init Error: ${sdlError()}")
            throw Error()
        }
        arena.defer { SDL_Quit() }

        window = SDL_CreateWindow("Snake", SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED, pixelWidth, pixelHeight, SDL_WINDOW_SHOWN).failOnError()
        arena.defer { SDL_DestroyWindow(window) }

        renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED or SDL_RENDERER_PRESENTVSYNC).failOnError()
        arena.defer { SDL_DestroyRenderer(renderer) }

        font = Font(renderer, arena)
        sprites = Sprites(renderer, arena)
    }

    fun draw(game: Game) = memScoped {
        SDL_RenderClear(renderer)
        SDL_SetRenderDrawColor(renderer, 200 / 2, 230 / 2, 151 / 2, SDL_ALPHA_OPAQUE.toByte())

        val grassW = 256
        val grassScaledW = 400 // scale grass up to reduce its resolution so that it's similar to snake sprites
        0.until(pixelWidth / grassW + 1).forEach { x ->
            0.until(pixelHeight / grassW + 1).forEach { y ->
                sprites.render(sprites.grassRect, allocRect(x * grassW, y * grassW, grassScaledW, grassScaledW))
            }
        }

        game.apples.cells.forEach {
            sprites.render(sprites.appleRect, cellRect(it))
        }

        game.snake.tail.dropLast(1).forEachIndexed { i, it ->
            val index = i + 1
            val direction = direction(from = game.snake.cells[index - 1], to = it)
            val nextDirection = direction(from = game.snake.cells[index + 1], to = it)

            val srcRect = if (direction.isOpposite(nextDirection)) {
                when (direction) {
                    right, left -> sprites.bodyHRect
                    up, down    -> sprites.bodyVRect
                }
            } else if ((direction == down && nextDirection == right) || (direction == right && nextDirection == down)) {
                sprites.bodyDRRect
            } else if ((direction == up && nextDirection == right) || (direction == right && nextDirection == up)) {
                sprites.bodyURRect
            } else if ((direction == down && nextDirection == left) || (direction == left && nextDirection == down)) {
                sprites.bodyDLRect
            } else if ((direction == up && nextDirection == left) || (direction == left && nextDirection == up)) {
                sprites.bodyULRect
            } else {
                sprites.emptyRect
            }
            sprites.render(srcRect, cellRect(it))
        }

        val tipRect = when (game.snake.cells.let { direction(it[it.size - 2], it.last()) }) {
            up    -> sprites.tipURect
            down  -> sprites.tipDRect
            left  -> sprites.tipLRect
            right -> sprites.tipRRect
        }
        sprites.render(tipRect, cellRect(game.snake.tail.last()))

        val headRect = when (game.snake.direction) {
            up    -> sprites.headURect
            down  -> sprites.headDRect
            left  -> sprites.headLRect
            right -> sprites.headRRect
        }
        sprites.render(headRect, cellRect(game.snake.head))

        if (game.isOver) {
            renderStringCentered(3, game.width, "game over")
            renderStringCentered(5, game.width, "your score is ${game.score}")
        }

        // Print all letter to check spacing between letters
        // because the current font is not monospace and some letters look too wide (e.g. i,v,u)
//        renderString(Cell(0, 0), "abcdefghijklmnopqrstuvwxyz")
//        renderString(Cell(0, 1), "abcdefghijklmnopqrstuvwxyz".reversed())

        SDL_RenderPresent(renderer)
    }

    fun delay() {
        SDL_Delay(1000 / 60)
    }

    fun readCommands(): List<UserCommand> = memScoped {
        val result = ArrayList<UserCommand>()
        val event = alloc<SDL_Event>()
        while (SDL_PollEvent(event.ptr) != 0) {
            when (event.type) {
                SDL_QUIT    -> result.add(UserCommand.quit)
                SDL_KEYDOWN -> {
                    val keyboardEvent = event.ptr.reinterpret<SDL_KeyboardEvent>().pointed
                    val command = when (keyboardEvent.keysym.scancode) {
                        SDL_SCANCODE_I -> UserCommand.up
                        SDL_SCANCODE_J -> UserCommand.left
                        SDL_SCANCODE_K -> UserCommand.down
                        SDL_SCANCODE_L -> UserCommand.right
                        SDL_SCANCODE_R -> UserCommand.restart
                        SDL_SCANCODE_Q -> UserCommand.quit
                        else           -> null
                    }
                    if (command != null) result.add(command)
                }
            }
        }
        return result
    }

    fun destroy() {
        arena.clear()
    }

    private fun direction(from: Cell, to: Cell): Direction = when {
        from.x == to.x && from.y < to.y -> up
        from.x == to.x && from.y > to.y -> down
        from.x < to.x && from.y == to.y -> left
        from.x > to.x && from.y == to.y -> right
        else                            -> error("")
    }

    private fun NativePlacement.cellRect(cell: Cell): SDL_Rect {
        val x = cell.x * Sprites.w
        val y = cell.y * Sprites.h
        return allocRect(x, y, Sprites.w, Sprites.h)
    }

    private fun renderStringCentered(y: Int, width: Int, s: String) {
        var x = (width / 2) - (s.length / 2)
        if (x.rem(2) != 0) x--
        renderString(Cell(x, y), s)
    }

    private fun renderString(atCell: Cell, s: String) = memScoped {
        s.toCharArray().forEachIndexed { i, c ->
            font.render(c, cellRect(atCell.copy(x = atCell.x + i)))
        }
    }

    private fun <T> T?.failOnError(): T = failOnError(arena)

    enum class UserCommand {
        up, down, left, right, restart, quit
    }


    class Font(private val renderer: CPointer<SDL_Renderer>, private val arena: Arena) {
        companion object {
            const val w = 48
            const val h = 46
        }

        private val texture = renderer.loadTexture("Font16_42_Normal4_sheet.bmp", arena)
        private val letters: Map<Char, SDL_Rect>

        init {
            letters = mapOf(
                'A' to textureRect(0, 0, -7),
                'B' to textureRect(1, 0),
                'C' to textureRect(2, 0, -9),
                'D' to textureRect(3, 0),
                'E' to textureRect(4, 0, -5),
                'F' to textureRect(5, 0, -5),
                'G' to textureRect(6, 0),
                'H' to textureRect(7, 0, -7),
                'I' to textureRect(8, 0, -15),
                'J' to textureRect(9, 0, -5),
                'K' to textureRect(0, 1, -10),
                'L' to textureRect(1, 1, -5),
                'M' to textureRect(2, 1),
                'N' to textureRect(3, 1),
                'O' to textureRect(4, 1, -7),
                'P' to textureRect(5, 1, -7),
                'Q' to textureRect(6, 1),
                'R' to textureRect(7, 1),
                'S' to textureRect(8, 1),
                'T' to textureRect(9, 1),
                'U' to textureRect(0, 2, -13),
                'V' to textureRect(1, 2, -10),
                'W' to textureRect(2, 2),
                'X' to textureRect(3, 2),
                'Y' to textureRect(4, 2, -5),
                'Z' to textureRect(5, 2),
                '0' to textureRect(2, 5),
                '1' to textureRect(3, 5, -15),
                '2' to textureRect(4, 5),
                '3' to textureRect(5, 5),
                '4' to textureRect(6, 5),
                '5' to textureRect(7, 5),
                '6' to textureRect(8, 5),
                '7' to textureRect(9, 5),
                '8' to textureRect(0, 6),
                '9' to textureRect(1, 6),
                ' ' to arena.allocRect(0, 0, 0, 0)
            )
        }

        fun render(char: Char, cellRect: SDL_Rect) {
            val charRect = letters[char.toUpperCase()] ?: letters[' ']!!
            SDL_RenderCopy(renderer, texture, charRect.ptr, cellRect.ptr)
        }

        private fun textureRect(x: Int, y: Int, wAdjust: Int = 0): SDL_Rect {
            val xShift = x * w
            val yShift = y * h
            return arena.allocRect(xShift, yShift, w + wAdjust, h)
        }
    }

    class Sprites(private val renderer: CPointer<SDL_Renderer>, private val arena: Arena) {
        companion object {
            const val w = 64
            const val h = 64
        }

        private val texture = renderer.loadTexture("snake-graphics.bmp", arena)
        private val grassTexture = renderer.loadTexture("grass.bmp", arena)

        val headURect = textureRect(3, 0)
        val headRRect = textureRect(4, 0)
        val headLRect = textureRect(3, 1)
        val headDRect = textureRect(4, 1)

        val tipURect = textureRect(3, 2)
        val tipRRect = textureRect(4, 2)
        val tipLRect = textureRect(3, 3)
        val tipDRect = textureRect(4, 3)

        val bodyHRect = textureRect(1, 0)
        val bodyVRect = textureRect(2, 1)
        val bodyDRRect = textureRect(0, 0)
        val bodyURRect = textureRect(0, 1)
        val bodyDLRect = textureRect(2, 0)
        val bodyULRect = textureRect(2, 2)

        val appleRect = textureRect(0, 3)
        val emptyRect = textureRect(0, 2)

        val grassRect = arena.allocRect(0, 0, 256, 256)

        private fun textureRect(x: Int, y: Int) = arena.allocRect(x * w, y * h, w, h)

        fun render(srcRect: SDL_Rect, dstRect: SDL_Rect) {
            if (srcRect == grassRect) SDL_RenderCopy(renderer, grassTexture, srcRect.ptr, dstRect.ptr)
            else SDL_RenderCopy(renderer, texture, srcRect.ptr, dstRect.ptr)
        }
    }

    companion object {
        fun CPointer<SDL_Renderer>.loadTexture(fileName: String, arena: Arena): CPointer<SDL_Texture> = memScoped {
            val paths = listOf(fileName, "resources/$fileName", "../resources/$fileName")
            val filePath = paths.find { access(it, R_OK) == 0 } ?: error("Can't find image file.")

            val bmp = SDL_LoadBMP_RW(SDL_RWFromFile(filePath, "rb"), 1).failOnError(arena)
            defer { SDL_FreeSurface(bmp) }

            SDL_CreateTextureFromSurface(this@loadTexture, bmp).failOnError(arena)
        }

        fun sdlError() = SDL_GetError()!!.toKString()

        fun <T> T?.failOnError(arena: Arena): T {
            if (this == null) {
                println("Error: ${sdlError()}")
                arena.clear()
                throw Error()
            }
            return this
        }

        fun NativePlacement.allocRect(x: Int, y: Int, w: Int, h: Int): SDL_Rect = alloc<SDL_Rect>().also {
            it.x = x
            it.y = y
            it.w = w
            it.h = h
        }
    }
}

fun main(args: Array<String>) = memScoped {
    val initialGame = Game(
        width = 20,
        height = 10,
        snake = Snake(
            cells = listOf(Cell(4, 4), Cell(3, 4), Cell(2, 4), Cell(1, 4), Cell(0, 4)),
            direction = right
        )
    )
    var game = initialGame

    val sdlUI = SdlUI(game.width, game.height)
    defer { sdlUI.destroy() }

    var ticks = 0
    val speed = 10
    while (true) {

        sdlUI.draw(game)
        sdlUI.delay()

        ticks++
        if (ticks >= speed) {
            game = game.update()
            ticks -= speed
        }

        sdlUI.readCommands().forEach { command ->
            var direction: Direction? = null
            when (command) {
                SdlUI.UserCommand.up      -> direction = up
                SdlUI.UserCommand.down    -> direction = down
                SdlUI.UserCommand.left    -> direction = left
                SdlUI.UserCommand.right   -> direction = right
                SdlUI.UserCommand.restart -> game = initialGame
                SdlUI.UserCommand.quit    -> return
            }
            game = game.update(direction)
            sdlUI.draw(game)
        }
    }
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

    fun update(direction: Direction? = null): Game {
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