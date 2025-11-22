package anticope.rejects.mixin;

import anticope.rejects.modules.bots.elytrabot.events.CancellablePlayerMoveEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;


@Mixin(Entity.class)
public class ElytraBotEntityMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void onMove(MovementType type, Vec3d movement, CallbackInfo info) {
        if ((Object) this == mc.player) {
            if (MeteorClient.EVENT_BUS.post(CancellablePlayerMoveEvent.get(type, movement)).isCancelled())
                info.cancel();
        }
    }
}
