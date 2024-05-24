package com.grab.lint.inspector

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.File

class ArgumentParser : CliktCommand() {

    private val outPut by option(
        "-o",
        "--output",
        help = "a json file result that contains custom lint rules infos"
    ).convert { File(it) }.required()

    private val lintChecks by option(
        "-lc",
        "--lint-checks",
        help = "jar files that contains custom lint checks"
    ).convert { it.split(",").map { File(it) } }.default(emptyList())

    private val aarDeps by option(
        "-ad",
        "--aars-dirs",
        help = "aars extract directories that contains custom lint checks"
    ).convert { it.split(",").map { File(it) }.filter { it.name == "lint.jar" } }.default(emptyList())

    override fun run() {
        Inspector(outPut, lintChecks, aarDeps).run()
    }
}
