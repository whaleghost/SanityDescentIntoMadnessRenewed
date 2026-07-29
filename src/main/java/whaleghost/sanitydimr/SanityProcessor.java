package whaleghost.sanitydimr;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import whaleghost.sanitydimr.capability.*;
import whaleghost.sanitydimr.config.ConfigProxy;
import whaleghost.sanitydimr.entity.InnerEntity;
import whaleghost.sanitydimr.entity.InnerEntitySpawner;
import whaleghost.sanitydimr.item.ItemRegistry;
import whaleghost.sanitydimr.net.InnerEntityCapImplPacket;
import whaleghost.sanitydimr.net.SanityPacket;

import java.util.*;

/**
 * Central coordinator for sanity processing.
 * Delegates event handling to {@link SanityEventHandlers} and passive calculation to {@link PassiveSanityCalculator}.
 */
public final class SanityProcessor {

    public static final float SANITY_TARGET_THRESHOLD = .87f;

    private SanityProcessor() {}

    // ── Utility methods ──────────────────────────────────────────

    public static float getGarlandMultiplier(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ItemRegistry.GARLAND.get()) ? .92f : 1.0f;
    }

    public static float getSanityMultiplier(ServerPlayer player, float value) {
        ResourceLocation dim = player.level().dimension().location();
        return value >= 0 ? ConfigProxy.getNegMul(dim) * getGarlandMultiplier(player) : ConfigProxy.getPosMul(dim);
    }

    public static void addSanity(@NotNull ISanity sanity, float value, @NotNull ServerPlayer player) {
        if (value == 0.0f)
            return;
        sanity.setSanity(sanity.getSanity() + value * getSanityMultiplier(player, value));
    }

    // ── Player & Level tick ──────────────────────────────────────

    public static void tickPlayer(final ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator())
            return;

        Sanity s = player.getData(Sanity.ATTACHMENT);
        float passive = PassiveSanityCalculator.calcPassive(player, s);
        float snapshot = s.getSanity();
        // passive is pre-multiplied so no need for addSanity
        s.setSanity(s.getSanity() + passive);

        if (s instanceof IPassiveSanity ps) {
            ps.setPassiveIncrease(snapshot != s.getSanity() ? passive : 0);
        }
        if (s instanceof IPersistentSanity pst) {
            decrementCooldowns(pst);
        }
        if (s instanceof Sanity) {
            shareSanity(player, (Sanity) s);
        }

        InnerEntitySpawner.trySpawnForPlayer(player);
    }

    private static void decrementCooldowns(IPersistentSanity pst) {
        int[] cds = pst.getActiveSourcesCooldowns();
        for (int i = 0; i < cds.length; ++i)
            cds[i] = Mth.clamp(cds[i] - 1, 0, Integer.MAX_VALUE);

        decrementMapCooldowns(pst.getItemCooldowns());
        decrementMapCooldowns(pst.getBrokenBlocksCooldowns());
    }

    private static void decrementMapCooldowns(Map<Integer, Integer> cooldowns) {
        for (Iterator<Map.Entry<Integer, Integer>> it = cooldowns.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, Integer> entry = it.next();
            cooldowns.put(entry.getKey(), entry.getValue() - 1);
            if (cooldowns.get(entry.getKey()) <= 0)
                it.remove();
        }
    }

    private static void shareSanity(ServerPlayer player, Sanity cap) {
        if (cap.getDirty()) {
            PacketDistributor.sendToPlayer(player, SanityPacket.fromCapability(cap));
            cap.setDirty(false);
        }
    }

    public static void tickLevel(final ServerLevel level) {
        for (Entity ent : level.getEntities().getAll()) {
            if (ent instanceof InnerEntity ie) {
                InnerEntityCapImpl iec = ie.getData(InnerEntityCapImpl.ATTACHMENT);
                if (iec instanceof InnerEntityCapImpl ieci) {
                    boolean hasTarget = ie.getTarget() != null;
                    if (ieci.hasTarget() != hasTarget) {
                        ieci.setHasTarget(hasTarget);
                        ieci.setPlayerTargetUUID(
                                hasTarget && ie.getTarget() instanceof ServerPlayer sp ? sp.getUUID() : null);
                    }
                    if (ieci.getDirty()) {
                        PacketDistributor.sendToPlayersTrackingEntity(ent,
                                InnerEntityCapImplPacket.fromCapability(ent.getId(), ieci));
                    }
                }
            }
        }
    }

    // ── Query methods ────────────────────────────────────────────

    public static List<Player> getInsanePlayersInArea(final Level levelIn, BlockPos center, int blockRadius) {
        if (levelIn == null || center == null)
            return List.of();

        List<Player> list = new ArrayList<>();
        for (Player player : levelIn.getEntitiesOfClass(Player.class,
                new AABB(Vec3.atCenterOf(center).add(blockRadius, blockRadius, blockRadius),
                        Vec3.atCenterOf(center).add(-blockRadius, -blockRadius, -blockRadius)))) {
            Sanity s = player.getData(Sanity.ATTACHMENT);
            if (s.getSanity() >= SANITY_TARGET_THRESHOLD)
                list.add(player);
        }
        return list;
    }

    public static Player getMostInsanePlayer(final Level levelIn) {
        return getMostInsanePlayer(levelIn, SANITY_TARGET_THRESHOLD);
    }

    public static Player getMostInsanePlayer(final Level levelIn, float sanityThreshold) {
        if (levelIn == null)
            return null;

        Player result = null;
        float maxSanity = Float.MIN_VALUE;
        for (Player player : levelIn.players()) {
            if (player.isCreative() || player.isSpectator())
                continue;
            ISanity s = player.getData(Sanity.ATTACHMENT);
            if (s == null)
                continue;
            float sanity = s.getSanity();
            if (sanity >= sanityThreshold && sanity > maxSanity) {
                maxSanity = sanity;
                result = player;
            }
        }
        return result;
    }
}