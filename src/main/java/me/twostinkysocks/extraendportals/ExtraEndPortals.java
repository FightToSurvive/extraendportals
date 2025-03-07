package me.twostinkysocks.extraendportals;

import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public final class ExtraEndPortals extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    @Override
    public void onEnable() {
        load();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("endportals").setExecutor(this);
        getCommand("endportals").setTabCompleter(this);
    }

    public void load() {
        if(!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }
        File config = new File(this.getDataFolder(), "config.yml");
        if(!config.exists()) {
            saveDefaultConfig();
        }
        this.reloadConfig();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if(alias.equals("endportals")) {
            return List.of("reload");
        }
        return List.of();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(label.equals("endportals")) {
            if(!sender.hasPermission("endportals.reload")) {
                sender.sendMessage(NamedTextColor.RED + "You don't have permission!");
                return true;
            }
            if(args.length == 0 || !args[0].equals("reload")) {
                sender.sendMessage(NamedTextColor.RED + "Usage: /endportals reload");
                return true;
            }
            load();
            sender.sendMessage(NamedTextColor.GREEN + "Reloaded ExtraEndPortals");
        }
        return true;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onEye(PlayerInteractEvent e) {
        // if you click the air, or you click a block that's either not an end portal, or is a filled end portal, then do the thing
        if(e.getItem() == null || (e.getItem().getType() != Material.ENDER_EYE)) return;
        if(e.getAction() == Action.RIGHT_CLICK_AIR || (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock().getType() != Material.END_PORTAL_FRAME)) {
            Player p = e.getPlayer();
            e.setCancelled(true);
            e.setUseItemInHand(Event.Result.DENY);
            EnderSignal es = e.getPlayer().getWorld().spawn(p.getEyeLocation(), EnderSignal.class);
            Location currentLoc = p.getEyeLocation();
            Location initialTarget = getNearestStronghold(currentLoc, p);
            Location closestPortal = initialTarget;
            for(String worldName : getConfig().getKeys(false)) {
                World w = Bukkit.getWorld(worldName);
                for(String portalName : getConfig().getConfigurationSection(worldName).getKeys(false)) {
                    int x = getConfig().getInt(new StringBuilder(worldName).append(".").append(portalName).append(".x").toString());
                    int y = getConfig().getInt(new StringBuilder(worldName).append(".").append(portalName).append(".y").toString());
                    int z = getConfig().getInt(new StringBuilder(worldName).append(".").append(portalName).append(".z").toString());
                    Location currentPortalLocation = new Location(w, x, y, z);
                    if(currentLoc.distanceSquared(currentPortalLocation) < currentLoc.distanceSquared(closestPortal)) {
                        closestPortal = currentPortalLocation;
                    }
                }
            }
            es.setTargetLocation(closestPortal);
            if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
                return;
            }

            EquipmentSlot slot = e.getHand();
            ItemStack item = slot == EquipmentSlot.OFF_HAND ? p.getInventory().getItemInOffHand() : p.getInventory().getItemInMainHand();
            if (item.getAmount() > 0) {
                item.setAmount(item.getAmount() - 1);
                if (slot == EquipmentSlot.OFF_HAND) {
                    p.getInventory().setItemInOffHand(item);
                } else {
                    p.getInventory().setItemInMainHand(item);
                }
            }
        }
    }

    private Location getNearestStronghold(Location currentLoc, Player p) {
        ServerPlayer nmsPlayer = ((CraftPlayer)p).getHandle();
        World world = currentLoc.getWorld();
        ServerLevel nmsWorld = ((CraftWorld)world).getHandle();
        BlockPos nmsLocation = nmsWorld.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, nmsPlayer.blockPosition(), 100, false);
        Location bukkitLocation = CraftLocation.toBukkit(nmsLocation);
        bukkitLocation.setWorld(currentLoc.getWorld());
        return bukkitLocation;
    }
}
