package  org.jetbrains.dokka.sitemap

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.plugability.ConfigurableBlock
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.sitemap.transformers.pages.SitemapPageTransformer

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
    val sitemapPageTransformer by extending {
        CoreExtensions.pageTransformer providing ::SitemapPageTransformer
    }
}