package whaleghost.sanitydimr.event;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import whaleghost.sanitydimr.capability.Sanity;
import whaleghost.sanitydimr.client.SoundPlayback;
import whaleghost.sanitydimr.client.render.layer.Blackout;
import whaleghost.sanitydimr.sound.SoundRegistry;
import whaleghost.sanitydimr.SanityMod;

import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ClientEventHandler {

    private static final Set<SoundEvent> MUFFLED_AMBIENT_SOUNDS = Set.of(
            SoundEvents.CHICKEN_AMBIENT, SoundEvents.COW_AMBIENT,
            SoundEvents.PIG_AMBIENT, SoundEvents.SHEEP_AMBIENT);

    private static final Set<SoundEvent> DISTORTED_HURT_SOUNDS = Set.of(
            SoundEvents.CHICKEN_HURT, SoundEvents.CHICKEN_DEATH,
            SoundEvents.COW_HURT, SoundEvents.COW_DEATH,
            SoundEvents.PIG_HURT, SoundEvents.PIG_DEATH,
            SoundEvents.SHEEP_HURT, SoundEvents.SHEEP_DEATH);

    @SubscribeEvent
    public void tickLocalPlayer(final PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof LocalPlayer localPlayer) {
            SoundPlayback.playSounds(localPlayer);
        }
    }

    @SubscribeEvent
    public void onRenderFrame(final RenderFrameEvent.Post event) {
        DeltaTracker delta = event.getPartialTick();
        float deltaInTicks = delta.getGameTimeDeltaTicks();
        SanityMod.getInstance().getGui().tick(deltaInTicks);
    }

    @SubscribeEvent
    public void localLevelLoad(final LevelEvent.Load event) {
        if (event.getLevel() instanceof ClientLevel) {
            SoundPlayback.onClientLevelLoad((ClientLevel) event.getLevel());
        }
    }

    @SubscribeEvent
    public void onPlaySound(final PlayLevelSoundEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        Sanity s = player.getData(Sanity.ATTACHMENT);
        if (s.getSanity() < Blackout.THRESHOLD) {
            return;
        }
        Holder<SoundEvent> soundHolder = event.getSound();
        if (soundHolder == null) {
            return;
        }
        SoundEvent soundEvent = soundHolder.value();
        if (MUFFLED_AMBIENT_SOUNDS.contains(soundEvent)) {
            event.setCanceled(true);
        } else if (DISTORTED_HURT_SOUNDS.contains(soundEvent)) {
            event.setSound(SoundRegistry.INNER_ENTITY_HURT);
        }
    }
}
