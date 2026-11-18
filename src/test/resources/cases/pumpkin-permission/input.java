package example;
import pumpkin.Permission;
import pumpkin.PluginContext;
import pumpkin.PluginMetadata;
import pumpkin.PumpkinPlugin;
import java.util.List;
public class PermissionPlugin extends PumpkinPlugin {
    public PluginMetadata metadata() { return new PluginMetadata("p", "1", List.of(), "", List.of(), List.of()); }
    public void onLoad(PluginContext context) {
        context.registerPermission(new Permission("example.hello", "Allows hello", Permission.PermissionDefault.ALLOW, List.of()));
    }
}
