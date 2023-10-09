/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.plugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import java.io.Closeable
import java.io.File

@Suppress("FunctionName", "UNUSED_PARAMETER")
internal fun SamplesKotlinAnalysis(
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    context: DokkaContext,
    projectKotlinAnalysis: KotlinAnalysis
): KotlinAnalysis {
    val applicationDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.application")
    val projectDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project")

    return createAnalysisSession(sourceSets, applicationDisposable, projectDisposable) // TODO
}

internal fun ProjectKotlinAnalysis(
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    @Suppress("UNUSED_PARAMETER") context: DokkaContext,
): KotlinAnalysis {
    val applicationDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.application")
    val projectDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project")

    return createAnalysisSession(sourceSets, applicationDisposable, projectDisposable)
}

internal class KotlinAnalysis(
    val sourceModules: SourceSetDependent<KtSourceModule>,
    val analysisSession: StandaloneAnalysisAPISession,
    private val applicationDisposable: Disposable,
    private val projectDisposable: Disposable
) : Closeable {

    fun getModule(sourceSet: DokkaConfiguration.DokkaSourceSet) =
        sourceModules[sourceSet] ?: throw IllegalStateException("Missing a source module for sourceSet ${sourceSet.displayName} with id ${sourceSet.sourceSetID}")
    val project: Project
        get() = analysisSession.project

    override fun close() {
        Disposer.dispose(applicationDisposable)
        Disposer.dispose(projectDisposable)
    }
}