package example.packets;

import java.util.List;
import pumpkin.PluginContext;
import pumpkin.PluginMetadata;
import pumpkin.PumpkinPlugin;

/** Entry point for the five packet-handling examples. */
public class PacketUseCasePlugin extends PumpkinPlugin {
    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(
                "j2kt-packet-use-cases",
                "1.0.0",
                List.of("j2kt"),
                "Five packet handling examples",
                List.of(),
                List.of());
    }

    @Override
    public void onLoad(PluginContext context) {
        CustomPayloadExample.send(context.getServer());
        JavaTypedPacketExample.send(context.getServer());
        BedrockPacketExample.send(context.getServer());
        IncomingPayloadExample.install(this, context);
        PacketBroadcastExample.sendToAll(context.getServer());
    }
}
