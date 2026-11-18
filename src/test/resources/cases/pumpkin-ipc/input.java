package example;
import pumpkin.PluginContext;
import pumpkin.PluginMetadata;
import pumpkin.PumpkinPlugin;
import java.util.List;
public class IpcPlugin extends PumpkinPlugin {
    public PluginMetadata metadata() { return new PluginMetadata("i", "1", List.of(), "", List.of(), List.of()); }
    public void onLoad(PluginContext context) {
        getIpc().sendIpcMessage("other-plugin", new byte[]{1, 2, 3});
        getIpc().onMessage((sender, message) -> {
            return message;
        });
    }
}
