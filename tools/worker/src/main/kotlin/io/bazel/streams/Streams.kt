package io.bazel.streems

import java.io.InputStream
import java.io.PrintStream

interface Streams : AutoCloseable {
    val input: InputStream
    val output: PrintStream
    val error: PrintStream

    class DefaultStreams : Streams {
        override val input: InputStream = System.`in`
        override val output: PrintStream = System.out
        override val error: PrintStream = System.err
    }

    fun redirectSystemStreams(): Streams {
        System.setOut(error)
        return this
    }

    override fun close() {
        System.setOut(output)
    }
}