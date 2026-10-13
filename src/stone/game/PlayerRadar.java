package stone.game;

import stone.core.Vec3;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.EntityLivingBase;

public final class PlayerRadar {
    private static final float FORGET_SECONDS = 900.0f;
    private static final int MAX_HISTORY = 48;

    public static final class Item {
        public String id;
        public String name;

        public int count;
        public int sightings;
        public int damage;
        public int maxDamage;

        public Game.Slot slot;
        public float lastSeen;
        public List<Game.EnchantView> enchantments;
    }

    public static final class Player {
        public String uuid;
        public String name = "";

        public boolean self;

        public boolean loaded;
        public int entityId = -1;
        public float firstSeen;
        public float lastSeen;

        public Vec3 pos = Vec3.ZERO;
        public double distance;
        public float health;
        public float maxHealth = 20.0f;
        public float absorption;
        public int armor;
        public int ping = -1;

        public final Game.EquipmentView[] slots = new Game.EquipmentView[Game.Slot.values().length];
        public List<Game.EffectView> effects = List.of();
        public final List<Item> history = new ArrayList<>();
        public Game.SkinView skin = Game.SkinView.NONE;
    }

    private static final Map<String, Player> players = new LinkedHashMap<>();
    private static int loaded;

    private PlayerRadar() {}

    public static Collection<Player> players() {
        return players.values();
    }

    public static Player find(String uuid) {
        return players.get(uuid);
    }

    public static int loadedCount() {
        return loaded;
    }

    public static void clear() {
        players.clear();
        loaded = 0;
    }

    public static void update(FrameState state, float time) {
        for (Player player : players.values()) {
            player.loaded = false;
        }
        loaded = 0;

        if (state.player.valid) {
            Player me = sighted(state.player.handle, time);
            me.self = true;
            me.entityId = -1;
            me.pos = state.player.pos;
            me.distance = 0.0;
            me.health = state.player.health;
            me.maxHealth = state.player.maxHealth;
            me.armor = state.player.armor;
            me.ping = state.ping;
        }

        for (EntityView entity : state.entities) {
            if (!entity.isPlayer) {
                continue;
            }
            Player player = sighted((AbstractClientPlayer) entity.handle, time);
            player.entityId = entity.id;
            player.pos = entity.pos;
            player.distance = entity.distance;
            player.health = entity.health;
            player.maxHealth = entity.maxHealth;
            player.absorption = entity.absorption;
            player.armor = entity.armor;
            player.ping = Game.entityLatency((AbstractClientPlayer) entity.handle);
        }

        players.values().removeIf(player -> !player.loaded && time - player.lastSeen > FORGET_SECONDS);
    }

    private static Player sighted(AbstractClientPlayer handle, float time) {
        String uuid = Game.entityUuid(handle);
        Player player = players.get(uuid);
        if (player == null) {
            player = new Player();
            player.uuid = uuid;
            player.firstSeen = time;
            players.put(uuid, player);
        }
        if (player.name.isEmpty()) {
            player.name = Game.entityName(handle);
        }

        if (player.skin.texture() == 0) {
            player.skin = Game.playerSkin(handle);
        }
        player.loaded = true;
        player.lastSeen = time;
        readEquipment(player, handle, time);
        ++loaded;
        return player;
    }

    private static void readEquipment(Player player, EntityLivingBase handle, float time) {
        for (Game.Slot slot : Game.Slot.values()) {
            Game.EquipmentView item = Game.equipment(handle, slot);
            remember(player, item, slot, time);
            player.slots[slot.ordinal()] = item;
        }
        player.effects = Game.statusEffects(handle);
    }

    private static void remember(Player player, Game.EquipmentView item, Game.Slot slot, float time) {
        if (!item.present() || item.id().isEmpty()) {
            return;
        }
        for (Item known : player.history) {
            if (known.id.equals(item.id()) && known.name.equals(item.name())
                    && known.enchantments.equals(item.enchantments())) {
                known.count = Math.max(known.count, item.count());
                known.damage = item.damage();
                known.maxDamage = item.maxDamage();
                known.lastSeen = time;
                known.slot = slot;
                ++known.sightings;
                return;
            }
        }
        if (player.history.size() >= MAX_HISTORY) {
            return;
        }
        Item fresh = new Item();
        fresh.id = item.id();
        fresh.name = item.name();
        fresh.count = item.count();
        fresh.damage = item.damage();
        fresh.maxDamage = item.maxDamage();
        fresh.enchantments = item.enchantments();
        fresh.slot = slot;
        fresh.sightings = 1;
        fresh.lastSeen = time;
        player.history.add(fresh);
    }
}
