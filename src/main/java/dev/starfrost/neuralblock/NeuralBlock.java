package dev.starfrost.neuralblock;

import org.joml.Vector4f;
import org.slf4j.Logger;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(NeuralBlock.MODID)
public class NeuralBlock {
    public static final String MODID = "neuralblock";
    private static final Logger LOGGER = LogUtils.getLogger();

    private Minecraft mcInstance;
    private Window mcWindow;

    private long stopwatch = System.currentTimeMillis();
    private static final int MS_TO_WAIT = 500;
    private static final int MAX_RAYCAST_DISTANCE = 64;
    private static final float DEG_TO_RAD = Mth.PI/180;
    private static final float RAD_TO_DEG = 180/Mth.PI;


    public NeuralBlock(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        mcInstance = Minecraft.getInstance();
        mcWindow = mcInstance.getWindow();
    }

    @SubscribeEvent
    public void handleRenderFrame(RenderLevelStageEvent event){
        if(System.currentTimeMillis() - stopwatch < MS_TO_WAIT || mcInstance.player == null){
            return;
        }
        stopwatch = System.currentTimeMillis();


        Vec2 playerFov = new Vec2(
            mcInstance.options.fov().get(),
            (int)(2*Math.atan(Math.tan(mcInstance.options.fov().get()*DEG_TO_RAD/2)*mcWindow.getHeight()/mcWindow.getWidth())*RAD_TO_DEG) //https://github.com/themetalmuncher/fov-calc/blob/gh-pages/index.html#L22
        );

        LOGGER.info("====");
        getBlockFromScreenPos(mcInstance.level, mcInstance.player, new Vec2(-1f, -1f), playerFov, MAX_RAYCAST_DISTANCE);
        getBlockFromScreenPos(mcInstance.level, mcInstance.player, new Vec2(0f, 0f), playerFov, MAX_RAYCAST_DISTANCE);
        getBlockFromScreenPos(mcInstance.level, mcInstance.player, new Vec2(1f, 1f), playerFov, MAX_RAYCAST_DISTANCE);

        // getBlockFromScreenPos(
        //     mcInstance.level,
        //     mcInstance.player,
        //     new Vec2(-1f, 0f),
        //     fov,
        //     2*Math.atan(Math.tan(fov*DEG_TO_RAD/2)*mcWindow.getHeight()/mcWindow.getWidth())*RAD_TO_DEG, //https://github.com/themetalmuncher/fov-calc/blob/gh-pages/index.html#L22
        //     MAX_RAYCAST_DISTANCE
        // );

    }

    /**
     * Returns a block at a given screen coordinate
     * @param screenCoords Vec2 with a range of [-1,1], where 0,0 is the center of the screen. +x is right, +y is down.
     * @param fov_2d Vec2 with x as horizontal FOV and y as vertical FOV. FOV cannot be > 180 degrees, supporting that would be stupid
     */
    public static Block getBlockFromScreenPos(Level level, Player player, Vec2 screenCoords, Vec2 fov_2d, int maxDistance){
        Vec3 viewDirection = player.calculateViewVector(
            player.getXRot(), // + (float)(screenCoords.x * 0.5 * fov_h),
            player.getYRot() // + (float)(screenCoords.y * 0.5 * fov_v)
        ); //already normalized

        Vec3 viewLeftVector = player.calculateViewVector(0f, player.getYRot() - 90f).normalize();
        Vec3 viewUpVector = viewDirection.cross(viewLeftVector).normalize();



        Vec3 rayDirection = viewDirection
            .add(viewLeftVector.scale(1 - ((90 - (fov_2d.x/2 * screenCoords.x * -1))/90))) //left-right offset
            .add(viewUpVector.scale(1 - ((90 - (fov_2d.y/2 * screenCoords.y * -1))/90))) //up-down offset
            .normalize();

        //todo: test if this is correct

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

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}
