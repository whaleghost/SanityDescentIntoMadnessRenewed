package whaleghost.sanitydimr.event;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import whaleghost.sanitydimr.ActiveSanitySource;
import whaleghost.sanitydimr.SanityMod;
import whaleghost.sanitydimr.SanityProcessor;
import whaleghost.sanitydimr.capability.IPersistentSanity;
import whaleghost.sanitydimr.capability.Sanity;
import whaleghost.sanitydimr.capability.SanityLevelChunk;
import whaleghost.sanitydimr.config.*;
import whaleghost.sanitydimr.util.MathHelper;

import java.util.Map;
import java.util.function.Function;

import net.minecraft.core.component.DataComponents;

public final class SanityEventHandlers {

    private SanityEventHandlers() {}

    static void handleActiveSourceForPlayer(
            ServerPlayer player,
            ActiveSanitySource source,
            Function<ResourceLocation, Integer> cdSupplier,
            Function<ResourceLocation, Float> sanitySupplier) {

        if (player == null || player.isCreative() || player.isSpectator())
            return;

        Sanity s = player.getData(Sanity.ATTACHMENT);
        ResourceLocation dimLoc = player.level().dimension().location();
        int cd = cdSupplier.apply(dimLoc);

        if (s instanceof IPersistentSanity ps && cd > 0.0f) {
            int id = source.ordinal();
            int timePassed = cd - ps.getActiveSourcesCooldowns()[id];
            SanityProcessor.addSanity(
                    s, sanitySupplier.apply(dimLoc) * MathHelper.clampNorm((float) timePassed / cd), player);
            ps.getActiveSourcesCooldowns()[id] = cd;
        } else {
            SanityProcessor.addSanity(s, sanitySupplier.apply(dimLoc), player);
        }
    }

    public static void handlePlayerSlept(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator())
                continue;
            handleActiveSourceForPlayer(player, ActiveSanitySource.SLEEPING,
                    ConfigProxy::getSleepingCooldown, ConfigProxy::getSleeping);
        }
    }

    public static void handlePlayerHurt(ServerPlayer player, float amount) {
        if (player == null || player.isCreative() || player.isSpectator() || amount <= 0)
            return;
        Sanity s = player.getData(Sanity.ATTACHMENT);
        ResourceLocation dimLoc = player.level().dimension().location();
        SanityProcessor.addSanity(s, amount * ConfigProxy.getHurtRatio(dimLoc), player);
    }

    public static void handlePlayerHurtAnimal(ServerPlayer player, net.minecraft.world.entity.animal.Animal animal, float amount) {
        if (player == null || player.isCreative() || player.isSpectator() || amount <= 0)
            return;
        Sanity s = player.getData(Sanity.ATTACHMENT);
        ResourceLocation dimLoc = player.level().dimension().location();
        SanityProcessor.addSanity(s,
                amount * ConfigProxy.getAnimalHurtRatio(dimLoc) * (animal.isBaby() ? 2.0f : 1.0f), player);
    }

    public static void handlePlayerPetDeath(ServerPlayer player, net.minecraft.world.entity.TamableAnimal pet) {
        if (player == null || player.isCreative() || player.isSpectator() || pet.isOwnedBy(player))
            return;
        Sanity s = player.getData(Sanity.ATTACHMENT);
        ResourceLocation dimLoc = player.level().dimension().location();
        SanityProcessor.addSanity(s, ConfigProxy.getPetDeath(dimLoc), player);
    }

    public static void handlePlayerEnderManAngered(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator())
            return;
        Sanity s = player.getData(Sanity.ATTACHMENT);
        if (s instanceof IPersistentSanity ps && ps.getEnderManAngerTimer() <= 0) {
            ps.setEnderManAngerTimer(100);
        }
    }

    public static void handlePlayerGotAdvancement(ServerPlayer player, Advancement adv) {
        if (player == null || player.isCreative() || player.isSpectator())
            return;
        if (adv.display().isEmpty() || !adv.display().get().shouldAnnounceChat())
            return;
        Sanity s = player.getData(Sanity.ATTACHMENT);
        ResourceLocation dimLoc = player.level().dimension().location();
        SanityProcessor.addSanity(s, ConfigProxy.getAdvancement(dimLoc), player);
    }

    public static void handlePlayerBredAnimals(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator())
            return;
        handleActiveSourceForPlayer(player, ActiveSanitySource.BREEDING_ANIMALS,
                ConfigProxy::getAnimalBreedingCooldown, ConfigProxy::getAnimalBreeding);
    }

    public static void handlePlayerTradedWithVillager(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator())
            return;
        handleActiveSourceForPlayer(player, ActiveSanitySource.VILLAGER_TRADE,
                ConfigProxy::getVillagerTradeCooldown, ConfigProxy::getVillagerTrade);
    }

    public static void handlePlayerUsedShears(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator())
            return;
        handleActiveSourceForPlayer(player, ActiveSanitySource.SHEARING,
                ConfigProxy::getShearingCooldown, ConfigProxy::getShearing);
    }

    public static void handlePlayerSpawnedChicken(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator())
            return;
        handleActiveSourceForPlayer(player, ActiveSanitySource.SPAWNING_BABY_CHICKEN,
                ConfigProxy::getBabyChickenSpawningCooldown, ConfigProxy::getBabyChickenSpawning);
    }

    private static void handlePlayerAte(ServerPlayer player, ItemStack itemStack) {
        handleActiveSourceForPlayer(
                player,
                ActiveSanitySource.EATING,
                ConfigProxy::getEatingCooldown,
                dim -> itemStack.getFoodProperties(player).nutrition() * ConfigProxy.getEating(dim));
    }

    public static void handlePlayerUsedItem(ServerPlayer player, ItemStack itemStack) {
        if (player == null || player.isCreative() || player.isSpectator())
            return;

        Sanity s = player.getData(Sanity.ATTACHMENT);
        if (s instanceof IPersistentSanity ps) {
            ResourceLocation dim = player.level().dimension().location();

            for (ConfigItem citem : ConfigProxy.getItems(dim)) {
                if (!itemStack.is(BuiltInRegistries.ITEM.get(citem.m_name)))
                    continue;

                if (!ConfigProxy.getIdToItemCat(dim).containsKey(citem.m_cat)) {
                    SanityMod.LOGGER.warn("player " + player.getDisplayName().getString()
                            + " used " + citem.m_name + " from category " + citem.m_cat
                            + ", but no such category is present");
                    return;
                }

                ConfigItemCategory cat = ConfigProxy.getIdToItemCat(dim).get(citem.m_cat);
                if (cat.m_cd <= 0) {
                    SanityProcessor.addSanity(s, citem.m_sanity, player);
                    return;
                }

                Map<Integer, Integer> itemCds = ps.getItemCooldowns();
                if (!itemCds.containsKey(citem.m_cat) || itemCds.get(citem.m_cat) <= 0) {
                    SanityProcessor.addSanity(s, citem.m_sanity, player);
                } else {
                    int timePassed = cat.m_cd - itemCds.get(citem.m_cat);
                    SanityProcessor.addSanity(s,
                            citem.m_sanity * MathHelper.clampNorm((float) timePassed / cat.m_cd), player);
                }
                itemCds.put(citem.m_cat, cat.m_cd);
                return;
            }
        }

        if (itemStack.has(DataComponents.FOOD))
            handlePlayerAte(player, itemStack);
    }

    public static void handlePlayerFishedItem(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator())
            return;
        handleActiveSourceForPlayer(player, ActiveSanitySource.FISHING,
                ConfigProxy::getFishingCooldown, ConfigProxy::getFishing);
    }

    public static void handlePlayerMinedBlock(ServerPlayer player, BlockPos blockPos,
                                               BlockState blockState, Block block, boolean correctTool) {
        if (player == null)
            return;

        ServerLevel level = (ServerLevel) player.level();
        LevelChunk levelChunk = level.getChunkAt(blockPos);

        if (player.isCreative() || player.isSpectator()) {
            SanityLevelChunk sl = levelChunk.getData(SanityLevelChunk.ATTACHMENT);
            sl.getArtificiallyPlacedBlocks().remove(blockPos);
            levelChunk.setUnsaved(true);
            return;
        }

        Sanity s = player.getData(Sanity.ATTACHMENT);
        if (s instanceof IPersistentSanity ps) {
            ResourceLocation dim = level.dimension().location();

            for (ConfigBrokenBlock cbblock : ConfigProxy.getBrokenBlocks(dim)) {
                boolean matchesBlock = cbblock.m_isTag
                        ? blockState.getTags().anyMatch(tag -> tag.location().equals(cbblock.m_name))
                        : block.equals(BuiltInRegistries.BLOCK.get(cbblock.m_name));

                if (!matchesBlock)
                    continue;

                if (cbblock.m_toolRequired && !correctTool)
                    return;

                if (!ConfigProxy.getIdToBrokenBlockCat(dim).containsKey(cbblock.m_cat)) {
                    SanityMod.LOGGER.warn("player " + player.getDisplayName().getString()
                            + " mined " + cbblock.m_name + " from category " + cbblock.m_cat
                            + ", but no such category is present");
                    return;
                }

                if (cbblock.m_naturallyGend) {
                    SanityLevelChunk sl = levelChunk.getData(SanityLevelChunk.ATTACHMENT);
                    if (sl.getArtificiallyPlacedBlocks().remove(blockPos)) {
                        levelChunk.setUnsaved(true);
                        return;
                    }
                }

                ConfigBrokenBlockCategory cat = ConfigProxy.getIdToBrokenBlockCat(dim).get(cbblock.m_cat);
                if (cat.m_cd <= 0) {
                    SanityProcessor.addSanity(s, cbblock.m_sanity, player);
                    return;
                }

                Map<Integer, Integer> brokenBlockCds = ps.getBrokenBlocksCooldowns();
                if (!brokenBlockCds.containsKey(cbblock.m_cat) || brokenBlockCds.get(cbblock.m_cat) <= 0) {
                    SanityProcessor.addSanity(s, cbblock.m_sanity, player);
                } else {
                    int timePassed = cat.m_cd - brokenBlockCds.get(cbblock.m_cat);
                    SanityProcessor.addSanity(s,
                            cbblock.m_sanity * MathHelper.clampNorm((float) timePassed / cat.m_cd), player);
                }
                brokenBlockCds.put(cbblock.m_cat, cat.m_cd);
                return;
            }
        }
    }

    public static void handlePlayerTrampledFarmland(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator())
            return;
        Sanity s = player.getData(Sanity.ATTACHMENT);
        SanityProcessor.addSanity(s, ConfigProxy.getFarmlandTrample(player.level().dimension().location()), player);
    }

    public static void handlePlayerPottedFlower(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator())
            return;
        handleActiveSourceForPlayer(player, ActiveSanitySource.POTTING_FLOWER,
                ConfigProxy::getPottingFlowerCooldown, ConfigProxy::getPottingFlower);
    }

    public static void handlePlayerChangedDimensions(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator())
            return;
        Sanity s = player.getData(Sanity.ATTACHMENT);
        SanityProcessor.addSanity(s, ConfigProxy.getChangedDimension(player.level().dimension().location()), player);
    }

    public static void handlePlayerStruckByLightning(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator())
            return;
        Sanity s = player.getData(Sanity.ATTACHMENT);
        SanityProcessor.addSanity(s, ConfigProxy.getStruckByLightning(player.level().dimension().location()), player);
    }
}
