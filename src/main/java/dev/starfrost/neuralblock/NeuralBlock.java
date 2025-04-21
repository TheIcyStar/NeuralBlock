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
    private static final int MS_TO_WAIT = 1000;
    private static final int MAX_RAYCAST_DISTANCE = 64;

    private static StringBuilder csvLineStringBuilder;
    private static File outputCSVFile;

    private record RaycastResult(Vec3 hitPos, String blockResourceName) {}


    public NeuralBlock(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::clientSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        mcInstance = Minecraft.getInstance();
        csvLineStringBuilder = new StringBuilder();
    }

    @SubscribeEvent
    @SuppressWarnings("null") // I check for it and it still yells at me >:C
    public void handleRenderFrame(RenderLevelStageEvent event){
        if(System.currentTimeMillis() - stopwatch < MS_TO_WAIT || mcInstance.player == null){
            return;
        }
        stopwatch = System.currentTimeMillis();

        csvLineStringBuilder.append(String.format( "%1$.2f,%2$.2f," , mcInstance.player.getXRot(), mcInstance.player.getYRot() % 90));

        for(int y=0; y < Config.gridSizeY; y++){
            for(int x=1; x <= Config.gridSizeX; x++){
                int index = x + y*Config.gridSizeX;
                Vec2 screenLocation = new Vec2( ((float)x / Config.gridSizeX)*2-1, ((float)(y+1) / Config.gridSizeY)*2-1 );
                RaycastResult raycastResult = getBlockFromScreenPos(mcInstance.level, mcInstance.player, screenLocation, MAX_RAYCAST_DISTANCE);

                csvLineStringBuilder.append(String.format(
                    "%s,%.2f",
                    raycastResult.blockResourceName,
                    raycastResult.hitPos.distanceTo(mcInstance.player.position())
                ));

                if(index < Config.gridSizeX * Config.gridSizeY){
                    csvLineStringBuilder.append(",");
                } else {
                    csvLineStringBuilder.append("\n");
                }
            }
        }

        try(FileWriter fw = new FileWriter(outputCSVFile, true)) {
            fw.append(csvLineStringBuilder);
        } catch (IOException ioex) {
            LOGGER.error("Could not write block grid to CSV. Path:" + outputCSVFile.getAbsolutePath(), ioex);
            throw new RuntimeException(ioex);
        }

        csvLineStringBuilder.setLength(0);
    }

    /**
     * Returns a block with a screen offset, assuming a 90 degree fov
     * @param screenCoords Vec2 with a range of [-1,1], where 0,0 is the center of the screen. +x is right, +y is down.
     */
    public static RaycastResult getBlockFromScreenPos(Level level, Player player, Vec2 screenCoords, int maxDistance){
        Vec3 viewDirection = player.calculateViewVector( player.getXRot(), player.getYRot() ); //already normalized

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

        return new RaycastResult(
            new Vec3(hitResult.getBlockPos().getX(), hitResult.getBlockPos().getY(), hitResult.getBlockPos().getZ()),
            BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString()
        );
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
            for(int i=1; i <= Config.gridSizeX * Config.gridSizeY; i++){
                fw.append("block_"+i+",block_"+i+"_dist");

                if(i < Config.gridSizeX * Config.gridSizeY){
                    fw.append(",");
                } else {
                    fw.append("\n");
                }
            }

        } catch (IOException ioex) {
            LOGGER.error("Could not write CSV at " + outputCSVFile.getAbsolutePath(), ioex);
            throw new RuntimeException(ioex);
        }


        LOGGER.info("Created new file at " + outputCSVFile.toPath());
        LOGGER.info(Config.csvPath);
    }
}
