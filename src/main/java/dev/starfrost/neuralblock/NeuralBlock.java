package dev.starfrost.neuralblock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = NeuralBlock.MODID, dist = Dist.CLIENT) // TODO: make sure this mod does nothing on the server
public class NeuralBlock {
    public static final String MODID = "neuralblock";
    private static final Logger LOGGER = LogUtils.getLogger();

    private Minecraft mcInstance;

    private long stopwatch = System.currentTimeMillis();
    private static final int MS_TO_WAIT = 500;
    private static final int MAX_RAYCAST_DISTANCE = 64;
    private static File outputCSVFile;


    public NeuralBlock(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::clientSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        mcInstance = Minecraft.getInstance();
    }

    @SubscribeEvent
    public void handleRenderFrame(RenderLevelStageEvent event){
        if(System.currentTimeMillis() - stopwatch < MS_TO_WAIT || mcInstance.player == null){
            return;
        }
        stopwatch = System.currentTimeMillis();


        LOGGER.info("====");
        getBlockFromScreenPos(mcInstance.level, mcInstance.player, new Vec2(-1f, -1f), MAX_RAYCAST_DISTANCE);
        getBlockFromScreenPos(mcInstance.level, mcInstance.player, new Vec2(0f, 0f), MAX_RAYCAST_DISTANCE);
        getBlockFromScreenPos(mcInstance.level, mcInstance.player, new Vec2(1f, 1f), MAX_RAYCAST_DISTANCE);
    }

    /**
     * Returns a block with a screen offset, assuming a 90 degree fov
     * @param screenCoords Vec2 with a range of [-1,1], where 0,0 is the center of the screen. +x is right, +y is down.
     */
    public static Block getBlockFromScreenPos(Level level, Player player, Vec2 screenCoords, int maxDistance){
        Vec3 viewDirection = player.calculateViewVector(
            player.getXRot(), // + (float)(screenCoords.x * 0.5 * fov_h),
            player.getYRot() // + (float)(screenCoords.y * 0.5 * fov_v)
        ); //already normalized

        Vec3 viewLeftVector = player.calculateViewVector(0f, player.getYRot() + 90f).normalize();
        Vec3 viewUpVector = viewDirection.cross(viewLeftVector).normalize();

        Vec3 rayDirection = viewDirection
            .add(viewLeftVector.scale(screenCoords.x * 1.85)) //magic multiplication constant to make 90 degrees actually behave like 90 degrees
            .add(viewUpVector.scale(screenCoords.y))
            .normalize();

        Vec3 startPos = player.getEyePosition(1.0F);
        Vec3 toPos = startPos.add(rayDirection.scale(maxDistance));

        ClipContext clipContext = new ClipContext(startPos, toPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult hitResult = level.clip(clipContext);
        BlockState blockState = level.getBlockState(hitResult.getBlockPos());

        LOGGER.info(
            "("+hitResult.getBlockPos()+"): "+BuiltInRegistries.BLOCK.getKey(blockState.getBlock())//+ "\n"+
            // "rayDir: " + rayDirection.toString()+ "\n" +
            // "lookDir: " + viewDirection.toString()
        );

        return blockState.getBlock();
    }

    // Create the CSV on startup
    //TODO: Find some "level loaded" event and use that instead. (Changing the config after startup doesn't change grid size)
    private void clientSetup(final FMLClientSetupEvent event) {
        String outputFileName = "NeuralBlockGrids-" + LocalDateTime.now().toString().replace(":","-") + ".csv";
        outputCSVFile = new File(Paths.get(Config.csvPath, outputFileName).toString());

        //Create parent directories;
        if(!outputCSVFile.getParentFile().exists()){
            outputCSVFile.getParentFile().mkdirs();
        }

        try(FileWriter fw = new FileWriter(outputCSVFile)) {
            fw.append("xRot,yRot,");
            for(int x=1; x <= Config.gridSizeX; x++){
                for(int y=0; y < Config.gridSizeY; y++){
                    int index = x + y*Config.gridSizeX;

                    if(index < Config.gridSizeX * Config.gridSizeY){
                        fw.append("block_"+index+",block_"+index+"_dist,");
                    } else {
                        fw.append("block_"+index+",block_"+index+"_dist\n");
                    }
                }
            }
        } catch (IOException ioex) {
            LOGGER.error("Could not write CSV at " + outputCSVFile.getAbsolutePath(), ioex);
            throw new RuntimeException(ioex);
        }


        LOGGER.info("Created new file at " + outputCSVFile.toPath());
        LOGGER.info(Config.csvPath);
    }

    // Append data to the csv
    public static void saveGrid(Vec2 playerRotations, String[] blockNames, int[] blockDistances) {

    }


}
