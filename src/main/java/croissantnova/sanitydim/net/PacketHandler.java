package croissantnova.sanitydim.net;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler
{
    public static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                SanityPacket.TYPE,
                SanityPacket.STREAM_CODEC,
                SanityPacket::handle
        );

        registrar.playToClient(
                InnerEntityCapImplPacket.TYPE,
                InnerEntityCapImplPacket.STREAM_CODEC,
                InnerEntityCapImplPacket::handle
        );
    }
}