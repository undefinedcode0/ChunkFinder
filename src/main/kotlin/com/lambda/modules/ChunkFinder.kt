package com.lambda.modules

import com.lambda.ExamplePlugin
import com.lambda.client.event.SafeClientEvent
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.threads.safeListener
import net.minecraft.util.math.ChunkPos
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.sqrt

/**
 * Flags chunks that stay client-loaded longer than expected given their
 * distance from the player. Sustained loading far outside render distance
 * is a signature of active redstone/hoppers/spawners - i.e. a base.
 **/
internal object ChunkFinder : PluginModule(
    name = "ChunkFinder",
    category = Category.MISC,
    description = "Flags chunks staying loaded abnormally long relative to player distance",
    pluginMain = ExamplePlugin
) {
    private val minDistance by setting("Min Distance", 64, 16..512, 16)
    private val decaySeconds by setting("Decay Seconds", 60, 10..600, 10)
    private val maxTracked by setting("Max Tracked", 12, 3..30, 1)

    private val loadedSince = HashMap<ChunkPos, Long>()
    val suspects = mutableListOf<Pair<ChunkPos, Double>>()

    init {
        safeListener<ChunkEvent.Load> {
            loadedSince[it.chunk.pos] = System.currentTimeMillis()
        }

        safeListener<ChunkEvent.Unload> {
            loadedSince.remove(it.chunk.pos)
        }

        safeListener<TickEvent.ClientTickEvent> {
            if (it.phase != TickEvent.Phase.END) return@safeListener

            val now = System.currentTimeMillis()
            val px = player.posX
            val pz = player.posZ

            val scored = loadedSince.mapNotNull { (pos, since) ->
                val dx = pos.xStart + 8.0 - px
                val dz = pos.zStart + 8.0 - pz
                val dist = sqrt(dx * dx + dz * dz)
                if (dist < minDistance) return@mapNotNull null

                val ageSeconds = (now - since) / 1000.0
                if (ageSeconds < decaySeconds) return@mapNotNull null

                // longer loaded + farther out than render distance explains = higher score
                pos to ageSeconds * (dist / minDistance)
            }

            suspects.clear()
            suspects.addAll(scored.sortedByDescending { it.second }.take(maxTracked))
        }
    }
}
