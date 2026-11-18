package example.uc1;

import pumpkin.AiGoal;
import pumpkin.Server;
import pumpkin.World;

/** A named custom AI goal so the generated class hierarchy is exercised. */
public class DemoAiGoal implements AiGoal {
    @Override
    public boolean canStart(Server server, World.Entity entity) {
        return true;
    }

    @Override
    public boolean shouldContinue(Server server, World.Entity entity) {
        return true;
    }

    @Override
    public void start(Server server, World.Entity entity) {
    }

    @Override
    public void tick(Server server, World.Entity entity) {
    }

    @Override
    public void stop(Server server, World.Entity entity) {
    }
}
