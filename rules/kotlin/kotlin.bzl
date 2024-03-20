load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", _kt_jvm_library = "kt_jvm_library")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", _kt_compiler_plugin = "kt_compiler_plugin")
load("@grab_bazel_common//rules/android/lint:defs.bzl", "lint", "lint_sources")
load("@grab_bazel_common//rules/android/lint:providers.bzl", "LINT_ENABLED")

kt_jvm_library = _kt_jvm_library
kt_compiler_plugin = _kt_compiler_plugin

def kt_jvm_library_interal(
        name,
        exec_properties = None,
        lint_options = {},
        **attrs):
    srcs = attrs.get("srcs", default = [])
    lint_sources_target = "_" + name + "_lint_sources"
    lint_baseline = lint_options.get("baseline", None)
    lint_enabled = lint_options.get("enabled", False) and len(srcs) > 0
    tags = attrs.get("tags", default = [])
    deps = attrs.get("deps", default = [])

    if lint_enabled:
        lint_sources(
            name = lint_sources_target,
            srcs = attrs.get("srcs"),
            baseline = lint_baseline,
            lint_config = lint_options.get("config", None),
        )

        lint(
            name = name,
            linting_target = name,
            lint_baseline = lint_baseline,
        )
        tags = tags + [LINT_ENABLED]
        deps = deps + [lint_sources_target]

        attrs["deps"] = deps
        attrs["tags"] = tags

    kt_jvm_library(
        name = name,
        **attrs
    )
