Slides and source code for the [Live Coding Kotlin/Native Snake talk](https://www.youtube.com/watch?time_continue=3&v=U-gdJQeOVAk).

Folders structure:
 - `snake` - the source code written during the talk. 
 See also the same snake game written in
 [Rust](https://github.com/dkandalov/rust-snake), [Scala Native](https://github.com/dkandalov/scala-native-snake),
 [Graal VM](https://github.com/dkandalov/graalvm-snake) and [Go](https://github.com/dkandalov/go-snake).
 - `snake-sdl` - snake implementation using [SDL](https://www.libsdl.org)
 - `slides` - Keynote slides (and [Rubik Font](https://fonts.google.com/specimen/Rubik?selection.family=Rubik) which is not available on OSX out of the box)
 - `plugins/kotlin-snake-snippets` - [live plugin](https://github.com/dkandalov/live-plugin) to show code snippets, e.g. source code of `KString`
 (see also [Man page viewer plugin](https://plugins.jetbrains.com/plugin/11167-man-page-viewer)).
 
Links mentioned in the talk:
 - [Kotlin/Native github page](https://github.com/JetBrains/kotlin-native)
 - [Kotlin/Native examples](https://github.com/JetBrains/kotlin-native/tree/master/samples)
 - [Tutorials](https://kotlinlang.org/docs/tutorials/native/basic-kotlin-native-app.html)
 - `@kotlin-native` channel in [Kotlin slack](https://kotlinlang.slack.com) (if you're not already on Kotlin slack, you can sign up [here](https://t.co/kwvW0nQzRf))
 - [Intro to ncurses](https://invisible-island.net/ncurses/ncurses-intro.html), [ncurses examples](https://github.com/tony/NCURSES-Programming-HOWTO-examples)
 - [High Performance Managed Languages](https://www.infoq.com/presentations/performance-managed-languages) talk on InfoQ
 - [LLVM](https://llvm.org/)

Version of the tools used in the talk:
 - IntelliJ IDEA 2019.2.2 (see [Previous IntelliJ Versions](https://www.jetbrains.com/idea/download/other.html))
 - Kotlin/Native 1.3.50 

In the older versions of the talk I used:
 - CLion 2018.2.1 (see [Previous CLion Versions](https://www.jetbrains.com/clion/download/previous.html))
 - Kotlin/Native [0.8.2](https://github.com/JetBrains/kotlin-native/releases/tag/v0.8.2) 
   with [kotlin-native-gradle-plugin](https://kotlinlang.org/docs/reference/native/gradle_plugin.html)
