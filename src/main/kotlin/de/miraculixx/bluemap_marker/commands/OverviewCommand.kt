package de.miraculixx.bluemap_marker.commands

import de.miraculixx.bluemap_marker.map.MarkerManager
import de.miraculixx.bluemap_marker.map.gui.storageBuilder
import de.miraculixx.bluemap_marker.utils.messages.*
import net.axay.kspigot.extensions.bukkit.register
import net.axay.kspigot.extensions.onlinePlayers
import net.axay.kspigot.extensions.worlds
import net.axay.kspigot.items.itemStack
import net.axay.kspigot.items.meta
import net.axay.kspigot.items.name
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta

class OverviewCommand : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(msg("command.no-player"))
            return false
        }


        val markers = if (args.isEmpty()) MarkerManager.getMarkers(sender.uniqueId)
        else {
            val target = Bukkit.getOfflinePlayer(args[0])
            MarkerManager.getMarkers(target.uniqueId)
        }
        if (markers.isEmpty()) {
            sender.sendMessage(msg("command.no-marker", listOf(sender.name)))
            return false
        }

        //BUILD GUI
        storageBuilder {
            player = sender
            title = cmp("Banner Markers - ", cHighlight, bold = true) + cmp(sender.name, cHighlight)
            header = itemStack(Material.PLAYER_HEAD) {
                meta<SkullMeta> {
                    owningPlayer = sender
                    name = cmp("$sender's Marker", cHighlight, bold = true)
                }
            }
            filterable = true
            items = buildList {
                markers.forEach { (worldName, marker) ->
                    val world = worlds.firstOrNull { it.name.equals(worldName, true) } ?: return@forEach
                    val material = when (world.environment) {
                        World.Environment.NORMAL -> Material.GRASS_BLOCK
                        World.Environment.NETHER -> Material.NETHERRACK
                        World.Environment.THE_END -> Material.END_STONE
                        World.Environment.CUSTOM -> Material.CRAFTING_TABLE
                    }
                    add(
                        itemStack(material) {
                            meta {
                                name = cmp(marker.label, cHighlight)
                                val vector = marker.position
                                lore(
                                    listOf(
                                        cmp("World: ", cMark) + cmp(world.name),
                                        cmp("Location: ", cMark) + cmp("${vector.x} ${vector.y} ${vector.z}"),
                                        cmp("Marker Type: ", cMark) + cmp(marker.type),
                                        Component.empty(),
                                        cmp("Shift Click » ") + cmp("Delete Marker", cMark)
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }.open()
        sender.playSound(sender, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f)
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>?): MutableList<String> {
        return buildList {
            when (args?.size ?: 0) {
                0, 1 -> addAll(onlinePlayers.map { it.name })
            }
        }.filter { it.startsWith(args?.lastOrNull() ?: "") }.toMutableList()
    }

    init {
        register("banner-marker")
    }
}