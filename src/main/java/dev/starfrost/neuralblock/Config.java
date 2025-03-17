package dev.starfrost.neuralblock;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = NeuralBlock.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<String> OUTPUT_CSV_DIR = BUILDER
            .comment("Path to generate the output CSVs in")
            .define("csvPath", "./");

    static final ModConfigSpec SPEC = BUILDER.build();

    public static String csvPath;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        csvPath = OUTPUT_CSV_DIR.get();
    }
}
