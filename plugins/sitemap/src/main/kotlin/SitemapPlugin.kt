package org.jetbrains.dokka.sitemap

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.allModulesPage.AllModulesPagePlugin
import org.jetbrains.dokka.plugability.ConfigurableBlock
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.sitemap.transformers.pages.SitemapPageTransformer
import org.jetbrains.dokka.sitemap.transformers.pages.SitemapTemplateProcessingStrategy
import org.jetbrains.dokka.templates.TemplatingPlugin
import kotlin.contracts.contract

data class SitemapConfiguration(
    var baseUrl: String? = defaultBaseUrl,
    var relativeOutputLocation: String? = defaultRelativeOutputLocation
) : ConfigurableBlock {
    companion object {
        val defaultBaseUrl: String? = null
        const val defaultRelativeOutputLocation: String = "sitemap.txt"
    }
}

class SitemapPlugin : DokkaPlugin() {
    private val templatingPlugin by lazy { plugin<TemplatingPlugin>() }
    val sitemapPageTransformer by extending {
        CoreExtensions.pageTransformer providing ::SitemapPageTransformer
    }
    val sitemapMultiModulePageTransformer by extending {
        with(plugin<AllModulesPagePlugin>()) {
            allModulesPageTransformer providing ::SitemapPageTransformer
        }
    }

    val sourcesetDependencyProcessingStrategy by extending {
        templatingPlugin.templateProcessingStrategy providing ::SitemapTemplateProcessingStrategy order {
            before(templatingPlugin.fallbackProcessingStrategy)
        }
    }
}
