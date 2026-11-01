package com.zythera.client.mixin;

import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

/** Allow the simulation-distance option itself (and its codec) to accept 2–32. */
@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
    @ModifyArg(
        method = "<init>",
        slice = @Slice(
            from = @At(value = "CONSTANT", args = "stringValue=options.simulationDistance"),
            to = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;simulationDistance:Lnet/minecraft/client/option/SimpleOption;")
        ),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption$ValidatingIntSliderCallbacks;<init>(II)V"),
        index = 0
    )
    private int zythera$allowTwoSimulationChunks(int vanillaMinimum) { return 2; }
}
