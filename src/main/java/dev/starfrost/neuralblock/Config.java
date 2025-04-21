package dev.starfrost.neuralblock;

import java.nio.file.Paths;

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
        .define("csvPath", Paths.get(System.getProperty("user.home"), "Downloads", "NeuralBlockCSVs").toAbsolutePath().toString());

    private static final ModConfigSpec.IntValue GRID_SIZE_X = BUILDER
        .comment("Horizontal block grid size (X)")
        .defineInRange("gridSizeX", 80, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue GRID_SIZE_Y = BUILDER
        .comment("Vertical block grid size (Y)")
        .defineInRange("gridSizeY", 45, 0, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static String csvPath;
    public static int gridSizeX;
    public static int gridSizeY;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        csvPath = OUTPUT_CSV_DIR.get();
        gridSizeX = GRID_SIZE_X.get();
        gridSizeY = GRID_SIZE_Y.get();
    }
}
