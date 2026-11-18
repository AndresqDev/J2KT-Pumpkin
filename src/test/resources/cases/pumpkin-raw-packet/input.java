package example;
import pumpkin.*;
import java.util.List;
public class RawPacketPlugin extends PumpkinPlugin {
    public PluginMetadata metadata() { return new PluginMetadata("raw", "1", List.of(), "", List.of(), List.of()); }
    public void onLoad(PluginContext context) {
        Player player = context.getServer().getPlayer("x");
        if (player != null) {
            JavaPlayer java = player.asJava();
            if (java != null) {
                java.sendPacket(new JavaPackets.CCustomPayload("my:channel", new byte[]{1, 2, 3}));
            }
            BedrockPlayer bedrock = player.asBedrock();
            if (bedrock != null) {
                bedrock.sendPacket(new BedrockPackets.Unknown());
            }
        }
    }
}
