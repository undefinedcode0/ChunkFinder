package com.lambda

import com.lambda.client.LambdaMod
import com.lambda.client.plugin.api.Plugin
import com.lambda.client.util.threads.BackgroundJob
import com.lambda.commands.ExampleCommand
import com.lambda.huds.ExampleLabelHud
import com.lambda.huds.ChunkFinderHud
import com.lambda.managers.ExampleManager
import com.lambda.modules.ExampleModule
import com.lambda.modules.ChunkFinder

internal object ExamplePlugin : Plugin() {

    override fun onLoad() {
        // Load any modules, commands, or HUD elements here
        modules.add(ExampleModule)
        modules.add(ChunkFinder)
        commands.add(ExampleCommand)
        hudElements.add(ExampleLabelHud)
        hudElements.add(ChunkFinderHud)
        managers.add(ExampleManager)

        bgJobs.add(BackgroundJob("ExampleJob", 10000L) { LambdaMod.LOG.info("Hello its me the BackgroundJob of your example plugin.") })
    }

    override fun onUnload() {
        // Here you can unregister threads etc...
    }
}