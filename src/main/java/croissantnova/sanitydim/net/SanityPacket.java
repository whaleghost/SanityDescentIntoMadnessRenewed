package croissantnova.sanitydim.net;

import croissantnova.sanitydim.SanityMod;
import croissantnova.sanitydim.capability.Sanity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SanityPacket(float sanityVal, float passive) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<SanityPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "sanity"));

    public static final StreamCodec<FriendlyByteBuf, SanityPacket> STREAM_CODEC = StreamCodec.ofMember(
            (SanityPacket payload, FriendlyByteBuf buf) ->
            {
                buf.writeFloat(payload.sanityVal);
                buf.writeFloat(payload.passive);
            },
            (FriendlyByteBuf buf) -> new SanityPacket(buf.readFloat(), buf.readFloat())
    );

    public static SanityPacket fromCapability(Sanity cap)
    {
        return new SanityPacket(cap.getSanity(), cap.getPassiveIncrease());
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(SanityPacket payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null)
                return;

            Sanity s = mc.player.getData(Sanity.ATTACHMENT);
            s.setSanity(payload.sanityVal);
            s.setPassiveIncrease(payload.passive);
        });
    }
}