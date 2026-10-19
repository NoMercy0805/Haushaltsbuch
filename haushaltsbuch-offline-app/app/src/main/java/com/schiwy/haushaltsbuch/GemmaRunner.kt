package com.schiwy.haushaltsbuch

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.runBlocking

/**
 * Lokale Einmal-Inferenz. Die Engine wird ausschließlich während eines
 * vom Nutzer gestarteten Berichts geladen und danach wieder geschlossen.
 * Weder Prompts noch Finanzeinträge verlassen das Gerät.
 */
object GemmaRunner {
    @JvmStatic
    fun generate(modelPath: String, prompt: String): String = runBlocking {
        val config = EngineConfig(
            modelPath = modelPath,
            backend = Backend.CPU()
        )
        Engine(config).use { engine ->
            engine.initialize()
            engine.createConversation().use { conversation ->
                conversation.sendMessage(prompt).toString()
            }
        }
    }
}
