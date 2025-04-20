package dev.starfrost.neuralblock;

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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(NeuralBlock.MODID)
public class NeuralBlock {
    public static final String MODID = "neuralblock";
    private static final Logger LOGGER = LogUtils.getLogger();

    private Minecraft mcInstance;

    private long stopwatch = System.currentTimeMillis();
    private static final int MS_TO_WAIT = 500;
    private static final int MAX_RAYCAST_DISTANCE = 64;


    public NeuralBlock(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
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


    private void commonSetup(final FMLCommonSetupEvent event) {

    }
}
