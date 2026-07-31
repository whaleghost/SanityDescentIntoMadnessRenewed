package whaleghost.sanitydimr.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import whaleghost.sanitydimr.capability.SanityLevelChunk;

public class BlockEventHandler {

    @SubscribeEvent
    public void onVanillaGameEvent(final VanillaGameEvent event) {
        if (event.getVanillaEvent() == GameEvent.BLOCK_PLACE) {
            Vec3 pos = event.getEventPosition();
            BlockPos bPos = BlockPos.containing(pos.x, pos.y, pos.z);
            SanityLevelChunk slc = event.getLevel().getChunkAt(bPos).getData(SanityLevelChunk.ATTACHMENT);
            slc.getArtificiallyPlacedBlocks().add(bPos);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(final BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer sp) {
            Block block = event.getState().getBlock();
            boolean notCreative = !sp.isCreative();
            SanityEventHandlers.handlePlayerMinedBlock(sp, event.getPos(), event.getState(), block, notCreative);
        }
    }

    @SubscribeEvent
    public void onFarmlandTrample(final BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SanityEventHandlers.handlePlayerTrampledFarmland(sp);
        }
    }
}
