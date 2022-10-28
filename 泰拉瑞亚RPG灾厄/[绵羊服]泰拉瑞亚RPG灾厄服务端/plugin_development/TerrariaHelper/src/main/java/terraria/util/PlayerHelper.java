package terraria.util;

import net.minecraft.server.v1_12_R1.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import terraria.TerrariaHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

public class PlayerHelper {
    public static void threadGrapplingHook() {
        // every 3 ticks (~1/7 second)
        Bukkit.getScheduler().scheduleSyncRepeatingTask(TerrariaHelper.getInstance(), () -> {
            for (Player ply : Bukkit.getOnlinePlayers()) {
                try {
                    if (ply.getGameMode() != GameMode.SURVIVAL) continue;
                    String hookItemName = EntityHelper.getMetadata(ply, "grapplingHookItem").asString();
                    if (!ItemHelper.splitItemName(ply.getInventory().getItemInOffHand())[1].equals(hookItemName)) continue;
                    ArrayList<Entity> hooks = (ArrayList<Entity>) EntityHelper.getMetadata(ply, "hooks").value();
                    if (hooks.size() == 0 || !hooks.get(0).getWorld().equals(ply.getWorld())) continue;
                    World plyWorld = ply.getWorld();
                    Location center = new Location(plyWorld, 0, 0, 0);
                    int hookedAmount = 0;
                    YmlHelper.YmlSection config = YmlHelper.getFile("plugins/Data/hooks.yml");
                    double hookReach = config.getDouble(hookItemName + ".reach", 12),
                            hookPullSpeed = config.getDouble(hookItemName + ".playerSpeed", 0.1);
                    hookReach = hookReach * hookReach / 2;
                    HashSet<Entity> hooksToRemove = new HashSet<>();
                    for (Entity hook : hooks) {
                        if (ply.getLocation().subtract(hook.getLocation()).lengthSquared() > hookReach) {
                            hook.remove();
                        }
                        if (hook.isDead()) {
                            hooksToRemove.add(hook);
                            continue;
                        }
                        if (hook.isOnGround()) {
                            center.add(hook.getLocation());
                            hookedAmount ++;
                        }
                        // draw chain
                        Vector dVec = hook.getLocation().subtract(ply.getEyeLocation()).toVector();
                        if (dVec.lengthSquared() > 0) {
                            GenericHelper.handleParticleLine(dVec, dVec.length(), 0, 1, 1,
                                    ply.getEyeLocation(), EntityHelper.getMetadata(hook, "color").asString());
                        }
                    }
                    for (Entity hook : hooksToRemove) hooks.remove(hook);
                    if (hookedAmount >= 1) {
                        ply.setGravity(false);
                        EntityHelper.setMetadata(ply, "thrust", 0);
                        EntityHelper.setMetadata(ply, "thrustProgress", 0);
                        ply.setFallDistance(0);
                        center.multiply(1 / (double)hookedAmount);
                        Vector thrust = center.subtract(ply.getEyeLocation()).toVector();
                        if (thrust.lengthSquared() > hookPullSpeed * hookPullSpeed * 36)
                            thrust.normalize().multiply(hookPullSpeed);
                        else if (thrust.lengthSquared() > 0)
                            thrust.multiply(0.1666667);
                        ply.setVelocity(thrust);
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().log(Level.SEVERE, "[Player Helper] threadGrapplingHook ", e);
                }
            }
        }, 3, 0);
    }


    public static int getMaxHealthByTier(int tier) {
        if (tier < 21) return tier * 40;
        if (tier < 41) return 800 + (tier - 20) * 10;
        switch (tier) {
            case 41:
                return 1050;
            case 42:
                return 1100;
            case 43:
                return 1150;
            default:
                return 1200;
        }
    }
    public static int getMaxManaByTier(int tier) {
        if (tier < 11) return tier * 20;
        switch (tier) {
            case 11:
                return 250;
            case 12:
                return 300;
            default:
                return 350;
        }
    }
    public static boolean hasDefeated(Player player, String progressToCheck) {
        YmlHelper.YmlSection fileSection = YmlHelper.getFile("plugins/PlayerData/" + player.getName() + ".yml");
        return fileSection.getBoolean("bossDefeated" + progressToCheck, false);
    }
    public static void handleGrapplingHook(Player ply) {
        try {
            List<Entity> hooks = (ArrayList<Entity>) EntityHelper.getMetadata(ply, "hooks").value();
            String hookItemName = EntityHelper.getMetadata(ply, "grapplingHookItem").asString();
            World hookWorld = ply.getWorld();
            YmlHelper.YmlSection config = YmlHelper.getFile("plugins/Data/hooks.yml");
            int hookAmount = config.getInt(hookItemName + ".amount", 0);
            if (hooks.size() >= hookAmount) {
                // removed the first hook on blocks if trying to launch more hooks than the player has
                Entity removed = null;
                for (Entity hook : hooks) {
                    if (hook.isOnGround()) {
                        hook.remove();
                        removed = hook;
                        break;
                    }
                }
                if (removed != null) hooks.remove(removed);
                else return;
            }
            Arrow hookEntity = (Arrow) hookWorld.spawnEntity(ply.getEyeLocation(), EntityType.ARROW);
            hookEntity.setShooter(ply);
            // velocity
            double hookSpeed = config.getDouble(hookItemName + ".velocity", 10) / 6;
            EntityPlayer nms_ply = ((CraftPlayer) ply).getHandle();
            double yaw = nms_ply.yaw,
                    pitch = nms_ply.pitch;
            Vector velocity = GenericHelper.vectorFromYawPitch_quick(yaw, pitch);
            velocity.multiply(hookSpeed);
            hookEntity.setGravity(false);
            hookEntity.setVelocity(velocity);
            // pre-set particle item
            List<String> hookColors = config.getStringList(hookItemName + ".particleItem");
            for (Entity hook : hooks) {
                hookColors.remove(EntityHelper.getMetadata(hook, "color").asString());
            }
            String color = hookColors.size() > 0 ? hookColors.get(0) : "125|125|125";
            EntityHelper.setMetadata(hookEntity, "color", color);
            // mark hook entity as a hook
            hookEntity.addScoreboardTag("isHook");
            hooks.add(hookEntity);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Player Helper] handleGrapplingHook ", e);
        }
    }

}
