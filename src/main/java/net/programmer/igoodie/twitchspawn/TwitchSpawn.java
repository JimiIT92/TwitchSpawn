package net.programmer.igoodie.twitchspawn;

import net.minecraft.command.arguments.ArgumentSerializer;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkDirection;
import net.programmer.igoodie.twitchspawn.client.gui.StatusIndicatorOverlay;
import net.programmer.igoodie.twitchspawn.command.RulesetNameArgumentType;
import net.programmer.igoodie.twitchspawn.command.StreamerArgumentType;
import net.programmer.igoodie.twitchspawn.command.TwitchSpawnCommand;
import net.programmer.igoodie.twitchspawn.configuration.ConfigManager;
import net.programmer.igoodie.twitchspawn.network.NetworkManager;
import net.programmer.igoodie.twitchspawn.network.packet.StatusChangedPacket;
import net.programmer.igoodie.twitchspawn.tracer.TraceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TwitchSpawn.MOD_ID)
public class TwitchSpawn {

    public static final String MOD_ID = "twitchspawn";
    public static final Logger LOGGER = LogManager.getLogger(TwitchSpawn.class);

    public static MinecraftServer SERVER;
    public static TraceManager TRACE_MANAGER;

    public TwitchSpawn() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::dedicatedServerSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        try {
            ConfigManager.loadConfigs();
            NetworkManager.initialize();

            ArgumentTypes.register("twitchspawn:streamer", StreamerArgumentType.class,
                    new ArgumentSerializer<>(StreamerArgumentType::streamerNick));
            ArgumentTypes.register("twitchspawn:ruleset", RulesetNameArgumentType.class,
                    new ArgumentSerializer<>(RulesetNameArgumentType::rulesetName));

        } catch (TwitchSpawnLoadingErrors e) {
            e.bindFMLWarnings(ModLoadingStage.COMMON_SETUP);
            throw new RuntimeException("TwitchSpawn loading errors occurred");
        }
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(StatusIndicatorOverlay.class);
    }

    private void dedicatedServerSetup(final FMLDedicatedServerSetupEvent event) {}

    @SubscribeEvent
    public void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(new SoundEvent(new ResourceLocation(TwitchSpawn.MOD_ID, "pop_in")));
        event.getRegistry().register(new SoundEvent(new ResourceLocation(TwitchSpawn.MOD_ID, "pop_out")));
    }

    @SubscribeEvent
    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        SERVER = event.getServer();
        TRACE_MANAGER = new TraceManager();
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        TwitchSpawnCommand.register(event.getCommandDispatcher());
    }

    @SubscribeEvent
    public void onServerStopping(FMLServerStoppingEvent event) {
        SERVER = null;

        if (TRACE_MANAGER.isRunning())
            TRACE_MANAGER.stop(null, "Server stopping");

        ConfigManager.RULESET_COLLECTION.clearQueue();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity entity = event.getPlayer();

        String translationKey = TRACE_MANAGER.isRunning() ?
                "commands.twitchspawn.status.on" : "commands.twitchspawn.status.off";

        entity.sendMessage(new TranslationTextComponent(translationKey));

        NetworkManager.CHANNEL.sendTo(new StatusChangedPacket(TRACE_MANAGER.isRunning()),
                ((ServerPlayerEntity) entity).connection.netManager,
                NetworkDirection.PLAY_TO_CLIENT);
    }

}
