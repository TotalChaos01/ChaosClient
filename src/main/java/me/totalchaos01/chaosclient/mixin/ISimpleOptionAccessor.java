package me.totalchaos01.chaosclient.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin for SimpleOption to bypass value validation.
 * MC 1.21.11 rejects gamma values > 1.0 via setValue(),
 * so we set the raw field directly.
 */
@Mixin(SimpleOption.class)
public interface ISimpleOptionAccessor {

    @Accessor("value")
    void setRawValue(Object value);

    @Accessor("value")
    Object getRawValue();
}
