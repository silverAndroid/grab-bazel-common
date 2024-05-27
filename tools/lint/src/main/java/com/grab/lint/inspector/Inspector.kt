package com.grab.lint.inspector

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Severity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.Attributes
import java.util.jar.JarFile

/**
 * Inspect the lint checks from the given jars and aar dependencies
 * adapted from https://android.googlesource.com/platform/tools/base/+/studio-master-dev/lint/libs/lint-api/src/main/java/com/android/tools/lint/client/api/JarFileIssueRegistry.kt
 */
class Inspector(
    private val outPut: File,
    private val lintChecks: List<File>,
    private val aarDeps: List<File>
) {
    private val issueAdapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(Array<LintCheckModel>::class.java)

    fun run() {
        println(
            """
            ************ Lint Inspector Jars ************
            lintChecks: 
            ${lintChecks.joinToString { it.path + "\n" }} 
        """.trimIndent()
        )
        println(
            """
            aarDeps: 
            ${aarDeps.joinToString { it.path + "\n" }}
            ************** End **************
        """.trimIndent()
        )

        outPut.parentFile?.mkdirs()
        outPut.createNewFile()

        outPut.writeText(
            issueAdapter.toJson(
                (lintChecks + aarDeps).loadIssues().map { (jar, issues) ->
                    LintCheckModel.fromJar(jar, issues)
                }.toTypedArray()
            )
        )
    }

    fun List<File>.loadIssues(): List<Pair<String, List<Issue>>> {
        val registeredIssues = map { jarFile ->
            val registry = findRegistries(jarFile)
            if (registry != null) {
                jarFile.path to registeredIssues(jarFile, registry)
            } else {
                jarFile.path to emptyList()
            }
        }

        registeredIssues.forEachIndexed { index, it ->
            println("lint jarFile=$index ${it.first}")
            it.second.forEachIndexed { index, issue ->
                println("Issue: $index ${issue.id} ${issue}")
            }
        }

        return registeredIssues
    }

    private fun registeredIssues(jarFile: File, className: String): List<Issue> {
        LintClient.clientName = "jarClassLoader"
        val loader =
            createUrlClassLoader(listOf(jarFile), ArgumentParser::class.java.classLoader)
        val registryClass = Class.forName(className, true, loader)
        val registry = registryClass.getDeclaredConstructor().newInstance() as IssueRegistry
        val issues =
            try {
                registry.issues
            } catch (e: Throwable) {
                e.printStackTrace()
                emptyList()
            }

        return issues
    }

    private val SERVICE_KEY = "META-INF/services/com.android.tools.lint.client.api.IssueRegistry"
    private val MF_LINT_REGISTRY = "Lint-Registry-v2"
    private val MF_LINT_REGISTRY_OLD = "Lint-Registry"

    private fun findRegistries(jarFile: File): String? {
        if (jarFile.exists().not()) {
            throw IllegalArgumentException("File not found: ${jarFile.absolutePath}")
        }
        JarFile(jarFile).use { file ->
            val manifest = file.manifest
            if (manifest != null) {
                val attrs = manifest.mainAttributes
                var attribute: Any? = attrs[Attributes.Name(MF_LINT_REGISTRY)]
                var isLegacy = false
                if (attribute == null) {
                    attribute = attrs[Attributes.Name(MF_LINT_REGISTRY_OLD)]
                    if (attribute != null) {
                        isLegacy = true
                    }
                }
                if (attribute is String) {
                    val className = attribute

                    // Store class name -- but it may not be unique (there could be
                    // multiple separate jar files pointing to the same issue registry
                    // (due to the way local lint.jar files propagate via project
                    // dependencies) so only store this file if it hasn't already
                    // been found, or if it's a v2 version (e.g. not legacy)
                    if (!isLegacy) {
                        return className
                    }
                    return@use
                }
            }

            // Load service keys. We're reading it manually instead of using
            // ServiceLoader because we don't want to put these jars into
            // the class loaders yet (since there can be many duplicates
            // when a library is available through multiple dependencies)
            val services = file.getJarEntry(SERVICE_KEY)
            if (services != null) {
                file.getInputStream(services).use {
                    val reader = InputStreamReader(it, Charsets.UTF_8)
                    reader.useLines { lines ->
                        for (line in lines) {
                            val comment = line.indexOf("#")
                            val className =
                                if (comment >= 0) {
                                    line.substring(0, comment).trim()
                                } else {
                                    line.trim()
                                }
                            if (className.isNotEmpty()) {
                                return className
                            }
                        }
                    }
                }
            } else if (jarFile.name == "lint.jar") {
                println(
                    Severity.ERROR.toString() + ":" +
                            null + ":" +
                            "Custom lint rule jar %1\$s does not contain a valid " +
                            "registry manifest key (%2\$s).\n" +
                            "Either the custom jar is invalid, or it uses an outdated " +
                            "API not supported this lint client" + ":" +
                            jarFile.path + ":"
                )
            }
        }
        return null
    }

    private fun createUrlClassLoader(files: List<File>, parent: ClassLoader): ClassLoader =
        URLClassLoader(files.mapNotNull { fileToUrl(it) }.toTypedArray(), parent)

    @Throws(MalformedURLException::class)
    private fun fileToUrl(file: File): URL {
        return file.toURI().toURL()
    }
}