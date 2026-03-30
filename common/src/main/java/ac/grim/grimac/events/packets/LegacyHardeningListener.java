package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight pre-check hardening pass for legacy hostile environments.
 *
 * <p>Grim already has deeper checks (BadPackets/Crash/Exploit). This listener performs fast,
 * low-allocation sanity and abuse checks first to avoid expensive downstream work whenever possible.
 */
public class LegacyHardeningListener extends PacketListenerAbstract {

    private static final class PlayerHardeningState {
        long currentSecond;
        int payloadPackets;
        int movePackets;
        int placePackets;
        int interactPackets;
        int clickPackets;
        int chatPackets;
        int severeViolations;
        long ignoreUntilMs;
    }

    private final Map<UUID, PlayerHardeningState> perPlayer = new ConcurrentHashMap<UUID, PlayerHardeningState>();
    private final Map<String, Long> lastLogAt = new ConcurrentHashMap<String, Long>();

    public LegacyHardeningListener() {
        super(PacketListenerPriority.LOWEST);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!cfgBool("legacy-hardening.enabled", true) || event.getUser() == null || event.isCancelled()) {
            return;
        }

        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) {
            return;
        }

        final UUID uuid = player.getUniqueId();
        final long nowMs = System.currentTimeMillis();
        PlayerHardeningState state = perPlayer.get(uuid);
        if (state == null) {
            state = new PlayerHardeningState();
            perPlayer.put(uuid, state);
        }

        if (state.ignoreUntilMs > nowMs) {
            if (cfgBool("legacy-hardening.actions.cancel-packet", true)) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
            return;
        }

        final long second = nowMs / 1000L;
        if (state.currentSecond != second) {
            state.currentSecond = second;
            state.payloadPackets = 0;
            state.movePackets = 0;
            state.placePackets = 0;
            state.interactPackets = 0;
            state.clickPackets = 0;
            state.chatPackets = 0;
            state.severeViolations = 0;
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            state.movePackets++;
            if (state.movePackets > cfgInt("legacy-hardening.packet-rate.movement-per-second", 250)) {
                punish(event, player, state, "movement_spam", false);
                return;
            }

            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
            if (flying.hasPositionChanged()) {
                Vector3d position = flying.getLocation().getPosition();
                final double maxCoordinate = cfgDouble("legacy-hardening.sanity.max-absolute-coordinate", 3.0E7D);
                if (!Double.isFinite(position.getX()) || !Double.isFinite(position.getY()) || !Double.isFinite(position.getZ())
                        || Math.abs(position.getX()) > maxCoordinate
                        || Math.abs(position.getY()) > maxCoordinate
                        || Math.abs(position.getZ()) > maxCoordinate) {
                    punish(event, player, state, "invalid_coordinates", true);
                    return;
                }
            }

            if (flying.hasRotationChanged()) {
                final float yaw = flying.getLocation().getYaw();
                final float pitch = flying.getLocation().getPitch();
                if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(pitch) > 90.1F || Math.abs(yaw) > 1000000.0F) {
                    punish(event, player, state, "invalid_rotation", true);
                    return;
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            state.payloadPackets++;
            if (state.payloadPackets > cfgInt("legacy-hardening.packet-rate.custom-payload-per-second", 30)) {
                punish(event, player, state, "payload_spam", false);
                return;
            }

            WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);
            byte[] data = wrapper.getData();
            final int maxPayloadSize = cfgInt("legacy-hardening.limits.custom-payload-bytes", 2048);
            if (data != null && data.length > maxPayloadSize) {
                punish(event, player, state, "payload_oversize:" + data.length, true);
                return;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            state.chatPackets++;
            if (state.chatPackets > cfgInt("legacy-hardening.packet-rate.chat-per-second", 4)) {
                punish(event, player, state, "chat_spam", false);
                return;
            }

            String message = new WrapperPlayClientChatMessage(event).getMessage();
            final int maxChatLength = cfgInt("legacy-hardening.limits.chat-length", 100);
            if (message != null && message.length() > maxChatLength) {
                punish(event, player, state, "chat_oversize:" + message.length(), false);
                return;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            state.clickPackets++;
            if (state.clickPackets > cfgInt("legacy-hardening.packet-rate.window-click-per-second", 80)) {
                punish(event, player, state, "window_click_spam", false);
                return;
            }

            WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
            int slot = click.getSlot();
            // -999 (outside) is a valid legacy slot. We reject anything outside broad valid bounds.
            if (slot < -999 || slot > cfgInt("legacy-hardening.limits.max-window-slot", 127)) {
                punish(event, player, state, "invalid_slot:" + slot, true);
                return;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            state.interactPackets++;
            if (state.interactPackets > cfgInt("legacy-hardening.packet-rate.interact-entity-per-second", 120)) {
                punish(event, player, state, "interact_spam", false);
                return;
            }

            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getEntityId() <= 0) {
                punish(event, player, state, "invalid_entity_id:" + interact.getEntityId(), true);
                return;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                || event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            state.placePackets++;
            if (state.placePackets > cfgInt("legacy-hardening.packet-rate.block-place-per-second", 90)) {
                punish(event, player, state, "place_spam", false);
            }
        }
    }

    private void punish(PacketReceiveEvent event, GrimPlayer player, PlayerHardeningState state, String reason, boolean severe) {
        state.severeViolations++;

        if (cfgBool("legacy-hardening.actions.cancel-packet", true)) {
            event.setCancelled(true);
            player.onPacketCancel();
        }

        if (cfgBool("legacy-hardening.actions.log-warning", true)) {
            logWithRateLimit("legacy-hardening:" + reason,
                    "[LegacyHardening] Cancelled packet from " + player.getName() + " reason=" + reason);
        }

        if (cfgBool("legacy-hardening.actions.alert-staff", true)) {
            player.sendMessage(MessageUtil.miniMessage("<gray>[<red>Exploit<gray>] <white>Packet blocked: " + reason));
        }

        int malformedThreshold = cfgInt("legacy-hardening.actions.malformed-threshold", 6);
        if (state.severeViolations >= malformedThreshold) {
            int ignoreSeconds = cfgInt("legacy-hardening.actions.ignore-seconds-after-threshold", 6);
            if (ignoreSeconds > 0) {
                state.ignoreUntilMs = System.currentTimeMillis() + (ignoreSeconds * 1000L);
            }
        }

        if (severe && cfgBool("legacy-hardening.actions.kick-on-severe", false)) {
            player.disconnect(MessageUtil.miniMessage("<red>Malformed packet stream blocked by anti-exploit."));
        }
    }

    private void logWithRateLimit(String key, String message) {
        final long now = System.currentTimeMillis();
        final long periodMs = cfgInt("legacy-hardening.actions.log-rate-limit-ms", 3000);
        Long last = lastLogAt.get(key);
        if (last == null || now - last.longValue() >= periodMs) {
            lastLogAt.put(key, now);
            LogUtil.warn(message);
        }
    }

    private boolean cfgBool(String key, boolean def) {
        return GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse(key, def);
    }

    private int cfgInt(String key, int def) {
        return GrimAPI.INSTANCE.getConfigManager().getConfig().getIntElse(key, def);
    }

    private double cfgDouble(String key, double def) {
        return GrimAPI.INSTANCE.getConfigManager().getConfig().getDoubleElse(key, def);
    }
}
