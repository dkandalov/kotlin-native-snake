import platform.posix.*

fun main(args: Array<String>) {
    println("Hello from ${getpid()}")
}