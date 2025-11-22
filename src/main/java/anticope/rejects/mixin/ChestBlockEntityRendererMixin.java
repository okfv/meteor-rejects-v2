package anticope.rejects.mixin;

import anticope.rejects.modules.Rendering;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChestBlockEntityRenderer.class)
public class ChestBlockEntityRendererMixin {
    @ModifyArg(
        method = "updateRenderState(Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/client/render/block/entity/state/ChestBlockEntityRenderState;FLnet/minecraft/util/math/Vec3d;Lnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/block/entity/ChestBlockEntityRenderer;getVariant(Lnet/minecraft/block/entity/BlockEntity;Z)Lnet/minecraft/client/render/block/entity/state/ChestBlockEntityRenderState$Variant;"
        ),
        index = 1
    )
    private boolean rejects$forceChristmas(BlockEntity blockEntity, boolean original) {
        if (Modules.get() == null) return original;
        Rendering rendering = Modules.get().get(Rendering.class);
        if (rendering != null && rendering.chistmas()) return true;
        return original;
    }
}
