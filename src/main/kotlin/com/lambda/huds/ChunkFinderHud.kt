package com.lambda.huds

import com.lambda.ExamplePlugin
import com.lambda.client.event.SafeClientEvent
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginLabelHud
import com.lambda.modules.ChunkFinder

internal object ChunkFinderHud : PluginLabelHud(
    name = "ChunkFinderHud",
    category = Category.MISC,
    description = "Lists chunks flagged as possible bases",
    pluginMain = ExamplePlugin
) {
    override fun SafeClientEvent.updateText() {
        if (ChunkFinder.suspects.isEmpty()) {
            displayText.add("ChunkFinder: no suspects", secondaryColor)
            return
        }
        ChunkFinder.suspects.forEachIndexed { i, (pos, score) ->
            displayText.add("${i + 1}. (${pos.xStart}, ${pos.zStart}) - %.0fs".format(score), primaryColor)
        }
    }
}
