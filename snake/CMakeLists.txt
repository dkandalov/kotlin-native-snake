cmake_minimum_required(VERSION 3.8)

set(CMAKE_MODULE_PATH ${CMAKE_CURRENT_LIST_DIR}/KotlinCMakeModule)

project(snake Kotlin)

konanc_executable(
    NAME snake
    SOURCES src/main.kt)

konanc_test(
    NAME snakeTest
    SOURCES src/main.kt test/tests.kt)

