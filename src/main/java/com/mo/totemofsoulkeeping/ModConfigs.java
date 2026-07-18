package com.mo.totemofsoulkeeping;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModConfigs {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue KEEP_EXPERIENCE;

    static {
        BUILDER.comment("Totem of Soul Keeping configuration").push("general");

        KEEP_EXPERIENCE = BUILDER.comment("Whether a triggered charm also preserves experience.")
                .define("keep_experience", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
