package whaleghost.sanitydimr.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import whaleghost.sanitydimr.capability.InnerEntityCapImpl;
import whaleghost.sanitydimr.capability.Sanity;
import whaleghost.sanitydimr.config.ConfigProxy;
import whaleghost.sanitydimr.entity.InnerEntity;
import whaleghost.sanitydimr.entity.InnerEntitySpawner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

public class RendererInnerEntity<T extends InnerEntity & GeoAnimatable> extends GeoEntityRenderer<T> {

    private final Minecraft mc = Minecraft.getInstance();
    private final AtomicBoolean shouldRender = new AtomicBoolean(false);
    private final AtomicBoolean isTargetMe = new AtomicBoolean(false);

    public RendererInnerEntity(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
    }

    public boolean shouldRender(T entity) {
        if (mc.player == null || entity == null) {
            return false;
        }
        if (
            ConfigProxy.getSaneSeeInnerEntities(mc.player.level().dimension().location()) ||
            mc.player.isCreative() || mc.player.isSpectator()
        ) {
            return true;
        }
        InnerEntityCapImpl iec = entity.getData(InnerEntityCapImpl.ATTACHMENT);
        isTargetMe.set(iec.getPlayerTargetUUID() != null && iec.getPlayerTargetUUID().equals(mc.player.getUUID()));
        if (isTargetMe.get()) {
            return true;
        }
        Sanity s = mc.player.getData(Sanity.ATTACHMENT);
        shouldRender.set(s.getSanity() >= InnerEntitySpawner.SPAWN_THRESHOLD);
        return shouldRender.get();
    }

    @Override
    public void render(
            T entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight
    ) {
        if (shouldRender(entity)) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }
    }

    @Override
    public RenderType getRenderType(
            T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick
    ) {
        return RenderType.entityTranslucent(getTextureLocation(animatable), false);
    }

}
