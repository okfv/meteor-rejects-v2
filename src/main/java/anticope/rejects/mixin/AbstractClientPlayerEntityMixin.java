package anticope.rejects.mixin;

import anticope.rejects.modules.Rendering;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {
    @Inject(method = "hasExtraEars", at = @At("HEAD"), cancellable = true)
    private void rejects$allowExtraEars(CallbackInfoReturnable<Boolean> cir) {
        Rendering renderingModule = Modules.get().get(Rendering.class);
        if (renderingModule != null && renderingModule.deadmau5EarsEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
