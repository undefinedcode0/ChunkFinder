package com.lambda.modules

import com.lambda.ExamplePlugin
import com.lambda.client.event.SafeClientEvent
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.threads.safeListener
import net.minecraft.util.math.ChunkPos
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Flags chunks that stay client-loaded longer than expected given their
 * distance from the player, AND are clustered with other suspicious chunks
 * in two dimensions - not just a straight line of chunks from a travel path.
 *
 * A travel corridor (walking/flying through) leaves a 1D line of stale
 * loaded chunks along your path. A real base leaves a 2D cluster (hopper
 * clock + storage + whatever else sits in a small area). Filtering for
 * clustering is what tells the two apart.
 **/
internal object ChunkFinder : PluginModule(
    name = "ChunkFinder",
    category = Category.MISC,
    description = "Flags clustered chunks staying loaded abnormally long relative to player distance",
    pluginMain = ExamplePlugin
) {
    private val minDistance by setting("Min Distance", 64, 16..512, 16)
    private val decaySeconds by setting("Decay Seconds", 60, 10..600, 10)
    private val maxTracked by setting("Max Tracked", 12, 3..30, 1)
    private val clusterRadius by setting("Cluster Radius", 3, 1..10, 1)
    private val minClusterNeighbors by setting("Min Cluster Neighbors", 2, 0..10, 1)

    private val loadedSince = HashMap<ChunkPos, Long>()

    // Pair<pos, real age in seconds> - age only, no more mixed score, so the HUD reads true time
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

            // Stage 1: distance + age filter, same as before
            val candidates = loadedSince.mapNotNull { (pos, since) ->
                val dx = pos.xStart + 8.0 - px
                val dz = pos.zStart + 8.0 - pz
                val dist = sqrt(dx * dx + dz * dz)
                if (dist < minDistance) return@mapNotNull null

                val ageSeconds = (now - since) / 1000.0
                if (ageSeconds < decaySeconds) return@mapNotNull null

                pos to ageSeconds
            }

            // Stage 2: clustering filter. Only count neighbors that are NOT on the
            // same row/column - that excludes a straight travel-path corridor from
            // inflating another corridor chunk's cluster score.
            val scored = candidates.filter { (pos, _) ->
                val neighborCount = candidates.count { (other, _) ->
                    other != pos &&
                        other.x != pos.x && other.z != pos.z &&
                        abs(other.x - pos.x) <= clusterRadius &&
                        abs(other.z - pos.z) <= clusterRadius
                }
                neighborCount >= minClusterNeighbors
            }

            suspects.clear()
            suspects.addAll(scored.sortedByDescending { it.second }.take(maxTracked))
        }
    }
}