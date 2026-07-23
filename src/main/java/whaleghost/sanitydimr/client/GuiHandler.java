package whaleghost.sanitydimr.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;
import whaleghost.sanitydimr.SanityMod;
import whaleghost.sanitydimr.capability.IPassiveSanity;
import whaleghost.sanitydimr.capability.ISanity;
import whaleghost.sanitydimr.capability.Sanity;
import whaleghost.sanitydimr.config.ConfigProxy;
import whaleghost.sanitydimr.config.SanityIndicatorLocation;
import whaleghost.sanitydimr.sound.SwishSoundInstance;
import whaleghost.sanitydimr.util.MathHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Random;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class GuiHandler {

    private static final float PASSIVE_THRESHOLD = 0.0002f;
    private static final float BLOOD_TENDRILS_DELAY = 5f * 20;
    private static final float HINT_FLASH_DURATION = 199f;
    private static final float HINT_INTERVAL_LOW_SANITY = 2000f;
    private static final float HINT_INTERVAL_HIGH_SANITY = 600f;
    private static final float SANITY_FLASH_DURATION = 20f;
    private static final float SANITY_HINT_THRESHOLD = 0.5f;
    private static final float SANITY_TWITCH_THRESHOLD = 0.7f;
    private static final float SANITY_POST_PROCESS_THRESHOLD = 0.4f;
    private static final float SANITY_POST_PROCESS_MAX = 0.8f;

    public static final ResourceLocation SANITY_INDICATOR = ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "textures/sanity_indicator.png");
    public static final ResourceLocation BLOOD_TENDRILS_OVERLAY = ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "textures/overlay/blood_tendrils.png");
    public static final MutableComponent[] LOW_SANITY_HINTS;
    public static final MutableComponent[] HIGH_SANITY_HINTS;

    private final Minecraft minecraft;
    private ISanity sanityCapability;
    private PostProcessor postProcessor;
    private final Random random = new Random();
    private int indicatorOffset;
    private int hintOffsetX;
    private int hintOffsetY;
    private float deltaTime;
    private float previousSanity;
    private float sanityGain;
    private float flashTimer;
    private float flashSanityGain;
    private float arrowTimer;
    private float hintTimer;
    private float showingHintTimer;
    private float maxShowingHintTimer;

    private float bloodTendrilsGainedAlpha;
    private float bloodTendrilsDelay;
    private float bloodTendrilsAlpha;
    private double bloodTendrilsTimer;

    private MutableComponent currentHint;

    public GuiHandler() {
        minecraft = Minecraft.getInstance();
    }

    static {
        LOW_SANITY_HINTS = new MutableComponent[12];
        for (int i = 0; i < LOW_SANITY_HINTS.length; i++) {
            LOW_SANITY_HINTS[i] = Component.translatable("gui." + SanityMod.MODID + ".hint0" + i);
        }
        HIGH_SANITY_HINTS = new MutableComponent[9];
        for (int i = 0; i < HIGH_SANITY_HINTS.length; i++) {
            HIGH_SANITY_HINTS[i] = Component.translatable("gui." + SanityMod.MODID + ".hint1" + i);
        }
    }

    private void initSanityPostProcess() {
        Minecraft mc = Minecraft.getInstance();
        postProcessor.addSinglePassEntry("insanity", pass ->
            processPlayer(mc.player, cap -> {
                if (cap.getSanity() < SANITY_POST_PROCESS_THRESHOLD) {
                    return false;
                }
                float factor = MathHelper.clampNorm(
                        Mth.inverseLerp(cap.getSanity(), SANITY_POST_PROCESS_THRESHOLD, SANITY_POST_PROCESS_MAX)
                );
                pass.getEffect().safeGetUniform("DesaturateFactor").set(factor * 0.69f);
                pass.getEffect().safeGetUniform("SpreadFactor").set(factor * 1.43f);
                return true;
            })
        );
        postProcessor.addSinglePassEntry("chromatical", pass ->
            processPlayer(mc.player, cap -> {
                if (cap.getSanity() < SANITY_POST_PROCESS_THRESHOLD) {
                    return false;
                }
                float factor = MathHelper.clampNorm(
                        Mth.inverseLerp(cap.getSanity(), SANITY_POST_PROCESS_THRESHOLD, SANITY_POST_PROCESS_MAX)
                );
                pass.getEffect().safeGetUniform("Factor").set(factor * 0.1f);
                pass.getEffect().safeGetUniform("TimeTotal").set(postProcessor.getTime() / 20.0f);
                return true;
            })
        );
    }

    private boolean processPlayer(LocalPlayer player, Function<ISanity, Boolean> action) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return false;
        }
        ISanity cap = player.getData(Sanity.ATTACHMENT);
        return cap.getSanity() > 0 && action.apply(cap);
    }

    private void renderSanityIndicator(GuiGraphics graphics, DeltaTracker deltaTracker) {
        int scaledWidth = minecraft.getWindow().getGuiScaledWidth();
        int scaledHeight = minecraft.getWindow().getGuiScaledHeight();
        if (
            minecraft.player == null || minecraft.player.isCreative() || minecraft.player.isSpectator() ||
            sanityCapability == null || !ConfigProxy.getRenderIndicator(minecraft.player.level().dimension().location())
        ) {
            return;
        }
        ResourceLocation dim = minecraft.player.level().dimension().location();
        float scale = ConfigProxy.getIndicatorScale(dim);
        if (scale <= 0f) {
            return;
        }
        SanityIndicatorLocation loc = ConfigProxy.getIndicatorLocation(dim);
        graphics.pose().pushPose();
        applyIndicatorTranslation(graphics, loc, scaledWidth, scaledHeight);
        graphics.pose().scale(scale, scale, 1f);
        final int textureWidth = 256;
        final int textureHeight = 128;
        final int spriteWidth = 33;
        final int spriteHeight = 24;
        int x = 0;
        int y = 0;
        switch (loc)
        {
            case HOTBAR_LEFT, BOTTOM_RIGHT -> { x = -spriteWidth; y = -spriteHeight; }
            case HOTBAR_RIGHT, BOTTOM_LEFT -> y = -spriteHeight;
            case TOP_RIGHT                 -> x = -spriteWidth;
            default                        -> {}
        }
        if (ConfigProxy.getTwitchIndicator(minecraft.player.level().dimension().location())) {
            y += indicatorOffset;
        }
        int verticalOffset = Math.round(sanityCapability.getSanity() * (spriteHeight - 2)) + 1;
        RenderSystem.setShaderTexture(0, SANITY_INDICATOR);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        // background
        graphics.blit(
            SANITY_INDICATOR, x, y, 0, 0, 0,
            spriteWidth, spriteHeight, textureWidth, textureHeight
        );
        if (flashTimer > 0 && ((int) flashTimer / 3) % 2 == 0) {
            // background flash
            graphics.blit(
                SANITY_INDICATOR, x, y, 0, spriteWidth, 0,
                spriteWidth, spriteHeight, textureWidth, textureHeight
            );
            if (flashSanityGain > 0) {
                int flashOffset = Math.round((sanityCapability.getSanity() - flashSanityGain) * (spriteHeight - 2)) + 1;
                // brain flash
                graphics.blit(
                    SANITY_INDICATOR, x, y + flashOffset, 0, spriteWidth * 3, flashOffset,
                    spriteWidth, spriteHeight - flashOffset, textureWidth, textureHeight
                );
            }
        }
        // brain fill
        graphics.blit(
            SANITY_INDICATOR, x, y + verticalOffset, 0, spriteWidth * 2, verticalOffset,
            spriteWidth, spriteHeight - verticalOffset, textureWidth, textureHeight
        );
        if (sanityCapability instanceof IPassiveSanity passive) {
            renderPassiveArrows(
                graphics, passive, x, y, verticalOffset,
                spriteWidth, spriteHeight, textureWidth, textureHeight
            );
        }
        graphics.pose().popPose();
    }

    private void applyIndicatorTranslation(
            GuiGraphics graphics, SanityIndicatorLocation loc, int scaledWidth, int scaledHeight
    ) {
        boolean hasOffhand = false;
        if (minecraft.player != null) {
            hasOffhand = !minecraft.player.getOffhandItem().isEmpty();
        }
        switch (loc)
        {
            case HOTBAR_LEFT  -> graphics.pose().translate(
                    scaledWidth / 2f - 97f - (hasOffhand ? 29f : 0f), scaledHeight - 5f, 0f
            );
            case HOTBAR_RIGHT -> graphics.pose().translate(97f, 0f, 0f);
            case TOP_LEFT     -> graphics.pose().translate(5f, 5f, 0f);
            case TOP_RIGHT    -> graphics.pose().translate(scaledWidth - 5f, 5f, 0f);
            case BOTTOM_LEFT  -> graphics.pose().translate(5f, scaledHeight - 5f, 0f);
            case BOTTOM_RIGHT -> graphics.pose().translate(scaledWidth - 5f, scaledHeight - 5f, 0f);
        }
    }

    private void renderPassiveArrows(
            GuiGraphics graphics, IPassiveSanity passive, int x, int y,
            int verticalOffset, int spriteWidth, int spriteHeight,
            int textureWidth, int textureHeight
    ) {
        float passiveIncrease = passive.getPassiveIncrease();
        if (passiveIncrease == 0) {
            return;
        }
        float absIncrease = Math.abs(passiveIncrease);
        int arrowOffset;
        if (absIncrease >= PASSIVE_THRESHOLD) {
            arrowTimer = Mth.clamp(arrowTimer, 0f, 23.99f);
            arrowOffset = (arrowTimer >= 12f && arrowTimer <= 15f) || (arrowTimer >= 0f && arrowTimer <= 3f) ? 0 :
            (
                ((int) arrowTimer / 3) % 2 == 0 ? 2 : 1
            );
            arrowOffset *= arrowTimer > 12f ? 1 : -1;
        } else {
            arrowTimer = Mth.clamp(arrowTimer, 0f, 15.99f);
            arrowOffset = ((int) arrowTimer / 4) % 2;
            arrowOffset *= arrowTimer > 8f ? 1 : -1;
        }
        if (passiveIncrease > 0) {
            if (absIncrease < PASSIVE_THRESHOLD) {
                graphics.blit(
                    SANITY_INDICATOR, x, y + arrowOffset, 0, 0, spriteHeight,
                    spriteWidth, spriteHeight, textureWidth, textureHeight
                );
                graphics.blit(
                    SANITY_INDICATOR, x, y + verticalOffset, 0, spriteWidth, spriteHeight + verticalOffset - arrowOffset, spriteWidth,
                    spriteHeight - verticalOffset + arrowOffset, textureWidth, textureHeight
                );
            } else {
                graphics.blit(
                    SANITY_INDICATOR, x, y + arrowOffset, 0, spriteWidth * 2, spriteHeight,
                    spriteWidth, spriteHeight, textureWidth, textureHeight
                );
                graphics.blit(
                    SANITY_INDICATOR, x, y + verticalOffset, 0, spriteWidth * 3, spriteHeight + verticalOffset - arrowOffset, spriteWidth,
                    spriteHeight - verticalOffset + arrowOffset, textureWidth, textureHeight
                );
            }
        } else {
            if (absIncrease < PASSIVE_THRESHOLD) {
                graphics.blit(
                    SANITY_INDICATOR, x, y + arrowOffset, 0, 0, spriteHeight * 2,
                    spriteWidth, spriteHeight, textureWidth, textureHeight
                );
                graphics.blit(
                    SANITY_INDICATOR, x, y + verticalOffset, 0, spriteWidth, spriteHeight * 2 + verticalOffset - arrowOffset,
                    spriteWidth, spriteHeight - verticalOffset + arrowOffset, textureWidth, textureHeight
                );
            } else {
                graphics.blit(
                    SANITY_INDICATOR, x, y + arrowOffset, 0, spriteWidth * 2, spriteHeight * 2,
                    spriteWidth, spriteHeight, textureWidth, textureHeight
                );
                graphics.blit(
                    SANITY_INDICATOR, x, y + verticalOffset, 0, spriteWidth * 3, spriteHeight * 2 + verticalOffset - arrowOffset,
                    spriteWidth, spriteHeight - verticalOffset + arrowOffset, textureWidth, textureHeight
                );
            }
        }
    }

    private void renderHint(GuiGraphics graphics, DeltaTracker deltaTracker) {
        int scaledWidth = minecraft.getWindow().getGuiScaledWidth();
        int scaledHeight = minecraft.getWindow().getGuiScaledHeight();
        if (
            minecraft.player == null || minecraft.player.isCreative()|| minecraft.player.isSpectator() ||
            currentHint == null || sanityCapability == null || sanityCapability.getSanity() < SANITY_HINT_THRESHOLD ||
            !ConfigProxy.getRenderHint(minecraft.player.level().dimension().location())
        ) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(scaledWidth / 2.0, scaledHeight / 2.0, 0.0);
        graphics.pose().scale(2f, 2f, 1f);
        float flicker = ((int) showingHintTimer % 10) / 10f;
        flicker = ((int) showingHintTimer / 10) % 2 == 0 ? flicker : 1 - flicker;
        float alpha = Mth.lerp(flicker, (showingHintTimer >= maxShowingHintTimer - 9f) || showingHintTimer < 10f ? 0f : 0.5f, 1f);
        int opacity = Mth.clamp((int) (alpha * 0xFF), 0x10, 0xEF) << 24;
        var posX = -minecraft.font.width(currentHint) / 2f;
        var posY = -minecraft.font.lineHeight / 2f;
        if (ConfigProxy.getTwitchHint(minecraft.player.level().dimension().location())) {
            posX += hintOffsetX;
            posY += hintOffsetY;
        }
        graphics.drawString(minecraft.font, currentHint, (int) posX, (int) posY, 0xFFFFFF | opacity);
        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private void renderBloodTendrilsOverlay(GuiGraphics graphics, DeltaTracker deltaTracker) {
        int scaledWidth = minecraft.getWindow().getGuiScaledWidth();
        int scaledHeight = minecraft.getWindow().getGuiScaledHeight();
        if (minecraft.player == null || minecraft.player.isCreative() || minecraft.player.isSpectator()) {
            return;
        }
        ResourceLocation dim = minecraft.player.level().dimension().location();
        if (
            !ConfigProxy.getRenderBtOverlay(dim) ||
            !(ConfigProxy.getFlashBtOnShortBurst(dim) ||
            ConfigProxy.getRenderBtPassive(dim))
        ) {
            return;
        }
        RenderSystem.setShaderTexture(0, BLOOD_TENDRILS_OVERLAY);
        if (bloodTendrilsAlpha > 0f) {
            renderFullscreen(
                graphics, scaledWidth, scaledHeight, 100, 58,
                0, 0, 100, 58, bloodTendrilsAlpha
            );
        }
    }

    public void tick(float dt) {
        if (
            minecraft.player == null || minecraft.player.isCreative() || minecraft.player.isSpectator() ||
            minecraft.isPaused()
        ) {
            return;
        }
        sanityCapability = minecraft.player.getData(Sanity.ATTACHMENT);
        deltaTime = dt;
        if (flashTimer > 0) {
            flashTimer -= dt;
        }
        sanityGain = sanityCapability.getSanity() - previousSanity;
        if (Math.abs(sanityGain) >= 0.01f) {
            flashTimer = SANITY_FLASH_DURATION;
        }
        flashSanityGain = flashTimer <= 0 ? 0 : flashSanityGain + sanityGain;
        if (sanityCapability instanceof IPassiveSanity passive) {
            if (arrowTimer <= 0) {
                arrowTimer = 23.99f;
            }
            if (passive.getPassiveIncrease() != 0) {
                arrowTimer -= dt;
            }
        }
        if (sanityCapability.getSanity() >= SANITY_TWITCH_THRESHOLD) {
            indicatorOffset = random.nextInt(3) - 1;
            hintOffsetX = random.nextInt(3) - 1;
            hintOffsetY = random.nextInt(3) - 1;
        } else {
            indicatorOffset = 0;
            hintOffsetX = 0;
            hintOffsetY = 0;
        }
        tickHint(dt);
        tickBloodTendrils(dt);
        previousSanity = sanityCapability.getSanity();
    }

    private void tickHint(float dt) {
        if (
            sanityCapability.getSanity() <= SANITY_POST_PROCESS_THRESHOLD ||
            !ConfigProxy.getRenderHint(minecraft.player.level().dimension().location())
        ) {
            return;
        }
        if (hintTimer <= 0f && showingHintTimer <= 0f) {
            int id;
            if (sanityCapability.getSanity() <= SANITY_TWITCH_THRESHOLD) {
                id = random.nextInt(LOW_SANITY_HINTS.length);
                currentHint = LOW_SANITY_HINTS[id];
                hintTimer = HINT_INTERVAL_LOW_SANITY;
                if (ConfigProxy.getPlaySounds(minecraft.player.level().dimension().location()) && id == 2) {
                    minecraft.getSoundManager().play(new SwishSoundInstance());
                }
            } else {
                id = random.nextInt(HIGH_SANITY_HINTS.length);
                currentHint = HIGH_SANITY_HINTS[id];
                hintTimer = HINT_INTERVAL_HIGH_SANITY;
                if (ConfigProxy.getPlaySounds(minecraft.player.level().dimension().location()) && id == 0) {
                    minecraft.getSoundManager().play(new SwishSoundInstance());
                }
            }
            showingHintTimer = HINT_FLASH_DURATION;
            maxShowingHintTimer = HINT_FLASH_DURATION;
        }
        if (showingHintTimer > 0f) {
            showingHintTimer -= dt;
        } else {
            hintTimer = MathHelper.clamp(hintTimer - dt, 0, Float.MAX_VALUE);
        }
    }

    private void tickBloodTendrils(float dt) {
        ResourceLocation dim = minecraft.player.level().dimension().location();
        boolean flashEnabled = ConfigProxy.getFlashBtOnShortBurst(dim);
        boolean passiveEnabled = ConfigProxy.getRenderBtPassive(dim);
        if (!ConfigProxy.getRenderBtOverlay(dim) || !(flashEnabled || passiveEnabled)) {
            return;
        }
        if (sanityGain >= 0.002f && flashEnabled) {
            bloodTendrilsGainedAlpha = Mth.lerp(
                MathHelper.clampNorm(Mth.inverseLerp(sanityGain, 0.002f, 0.02f)), 0.4f, 0.75f
            );
        }
        if (passiveEnabled) {
            bloodTendrilsDelay = Mth.clamp(
                bloodTendrilsDelay + (sanityCapability instanceof IPassiveSanity ps && ps.getPassiveIncrease() > 0f ? dt : -dt), 0, BLOOD_TENDRILS_DELAY
            );
        }
        if (bloodTendrilsGainedAlpha > 0f && flashEnabled) {
            if (bloodTendrilsAlpha < bloodTendrilsGainedAlpha) {
                bloodTendrilsAlpha = Mth.clamp(bloodTendrilsAlpha + 0.5f, 0f, bloodTendrilsGainedAlpha);
            } else {
                bloodTendrilsGainedAlpha = 0f;
            }
        } else if (bloodTendrilsDelay >= BLOOD_TENDRILS_DELAY && passiveEnabled) {
            if (bloodTendrilsAlpha < 0.15f) {
                bloodTendrilsTimer = 0;
                bloodTendrilsAlpha = Mth.clamp(bloodTendrilsAlpha + 0.1f, bloodTendrilsAlpha, 0.15f);
            } else if (bloodTendrilsAlpha > 0.3f) {
                bloodTendrilsTimer = Mth.PI / 0.2f;
                bloodTendrilsAlpha = Mth.clamp(bloodTendrilsAlpha - 0.1f, 0.3f, bloodTendrilsAlpha);
            } else {
                bloodTendrilsAlpha = Mth.lerp((-Mth.cos((float) bloodTendrilsTimer * 0.2f) + 1f) * 0.5f, 0.15f, 0.3f);
                bloodTendrilsTimer += deltaTime;
            }
        } else {
            bloodTendrilsAlpha = Mth.clamp(bloodTendrilsAlpha - 0.1f, 0f, bloodTendrilsAlpha);
        }
    }

    public void initOverlays(final RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "sanity_indicator"), this::renderSanityIndicator);
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "hint"), this::renderHint);
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "blood_tendrils_overlay"), this::renderBloodTendrilsOverlay);
    }

    public void initPostProcessor() {
        if (postProcessor != null) {
            return;
        }
        postProcessor = new PostProcessor();
        initSanityPostProcess();
    }

    public PostProcessor getPostProcessor() {
        return postProcessor;
    }

    public void renderPostProcess(float partialTicks) {
        if (postProcessor == null) {
            return;
        }
        postProcessor.render(partialTicks);
    }

    public void resize(int width, int height) {
        if (postProcessor == null) {
            return;
        }
        postProcessor.resize(width, height);
    }

    private static void renderFullscreen(
            GuiGraphics graphics, int screenWidth, int screenHeight,
            int textureWidth, int textureHeight,
            int uOffset, int vOffset, int spriteWidth, int spriteHeight, float alpha
    ) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder bufferBuilder =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder
                .addVertex(matrix, 0f, 0f, 0f)
                .setColor(1f, 1f, 1f, alpha)
                .setUv((float) uOffset / textureWidth, (float) vOffset / textureHeight);
        bufferBuilder
                .addVertex(matrix, 0f, (float) screenHeight, 0f)
                .setColor(1f, 1f, 1f, alpha)
                .setUv((float) uOffset / textureWidth, (float) (vOffset + spriteHeight) / textureHeight);
        bufferBuilder
                .addVertex(matrix, (float) screenWidth, (float) screenHeight, 0f)
                .setColor(1f, 1f, 1f, alpha)
                .setUv((float) (uOffset + spriteWidth) / textureWidth, (float) (vOffset + spriteHeight) / textureHeight);
        bufferBuilder
                .addVertex(matrix, (float) screenWidth, 0f, 0f)
                .setColor(1f, 1f, 1f, alpha)
                .setUv((float) (uOffset + spriteWidth) / textureWidth, (float) vOffset / textureHeight);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }
}