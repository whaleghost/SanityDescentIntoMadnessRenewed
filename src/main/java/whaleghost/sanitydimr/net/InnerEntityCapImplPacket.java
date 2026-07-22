package whaleghost.sanitydimr.net;

import whaleghost.sanitydimr.SanityMod;
import whaleghost.sanitydimr.capability.InnerEntityCapImpl;
import whaleghost.sanitydimr.entity.InnerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record InnerEntityCapImplPacket(int entityId, boolean hasTarget, UUID playerUuid) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<InnerEntityCapImplPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "inner_entity_cap"));

    public static final StreamCodec<FriendlyByteBuf, InnerEntityCapImplPacket> STREAM_CODEC = StreamCodec.ofMember(
            (InnerEntityCapImplPacket payload, FriendlyByteBuf buf) ->
            {
                buf.writeInt(payload.entityId);
                buf.writeBoolean(payload.hasTarget);
                if (payload.playerUuid != null)
                    buf.writeUUID(payload.playerUuid);
            },
            (FriendlyByteBuf buf) ->
            {
                int id = buf.readInt();
                boolean hasTarget = buf.readBoolean();
                UUID uuid = buf.isReadable() ? buf.readUUID() : null;
                return new InnerEntityCapImplPacket(id, hasTarget, uuid);
            }
    );

    public static InnerEntityCapImplPacket fromCapability(int entityId, InnerEntityCapImpl cap)
    {
        return new InnerEntityCapImplPacket(entityId, cap.hasTarget(), cap.getPlayerTargetUUID());
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(InnerEntityCapImplPacket payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null)
                return;

            Entity ent = mc.player.level().getEntity(payload.entityId);
            if (!(ent instanceof InnerEntity))
                return;

            InnerEntityCapImpl ieci = ent.getData(InnerEntityCapImpl.ATTACHMENT);
            ieci.setHasTarget(payload.hasTarget);
            ieci.setPlayerTargetUUID(payload.playerUuid);
        });
    }
}