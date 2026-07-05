package com.example.flintnohitbox;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = FlintNoHitboxMod.MODID, name = FlintNoHitboxMod.NAME, version = FlintNoHitboxMod.VERSION)
public class FlintNoHitboxMod {

    public static final String MODID = "flintnohitbox";
    public static final String NAME = "Flint No Hitbox";
    public static final String VERSION = "1.0";

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new FlintInteractHandler());
    }
}
