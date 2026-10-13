package stone.script;

import stone.core.Maths;
import stone.core.Vec3;
import stone.game.BlockKind;
import stone.game.CrosshairTarget;
import stone.game.EntityView;
import stone.game.FrameState;
import stone.game.Game;
import stone.game.ItemKind;
import stone.game.LocalPlayerView;
import stone.game.Raycast;
import stone.game.RotationEngine;
import stone.game.RotationTarget;
import stone.game.TargetFinder;
import stone.hook.Input;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.util.EnumFacing;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

final class ApiGame {
    private static final int OFFHAND_SLOT = 40;

    private static final Map<String, ItemKind> ITEM_KINDS = Map.of(
            "totem", ItemKind.TOTEM, "crystal", ItemKind.END_CRYSTAL, "anchor", ItemKind.RESPAWN_ANCHOR,
            "glowstone", ItemKind.GLOWSTONE, "mace", ItemKind.MACE, "obsidian", ItemKind.OBSIDIAN,
            "axe", ItemKind.AXE, "sword", ItemKind.SWORD);

    private static final Map<String, BlockKind> BLOCK_KINDS = Map.of(
            "obsidian", BlockKind.OBSIDIAN, "bedrock", BlockKind.BEDROCK, "anchor", BlockKind.RESPAWN_ANCHOR);

    private static final class Handle {
        final int id;

        int cached = -1;

        Handle(int id) {
            this.id = id;
        }
    }

    private ApiGame() {}

    static LuaValue entity(Script script, int id) {
        return new LuaUserdata(new Handle(id), script.entityType);
    }

    private static <T> T lookup(Map<String, T> table, Varargs args, int index, String what) {
        String name = args.checkjstring(index);
        T value = table.get(name);
        if (value == null) {
            throw Lua.error("unknown %s '%s'", what, name);
        }
        return value;
    }

    private static EnumFacing checkFace(Varargs args, int index) {
        String name = args.checkjstring(index);
        EnumFacing face = EnumFacing.byName(name);
        if (face == null) {
            throw Lua.error("unknown face '%s'", name);
        }
        return face;
    }

    private static void requireWorld() {
        ApiCore.requireContext();
        if (Game.world() == null) {
            throw Lua.error("there is no world right now");
        }
    }

    private static EntityView resolve(FrameState state, Handle handle) {
        List<EntityView> entities = state.entities;
        if (handle.cached >= 0 && handle.cached < entities.size() && entities.get(handle.cached).id == handle.id) {
            return entities.get(handle.cached);
        }
        for (int index = 0; index < entities.size(); index++) {
            if (entities.get(index).id == handle.id) {
                handle.cached = index;
                return entities.get(index);
            }
        }
        handle.cached = -1;
        return null;
    }

    private static Handle checkHandle(Varargs args, int index) {
        return (Handle) args.checkuserdata(index, Handle.class);
    }

    private static EntityView requireEntity(Varargs args, int index) {
        Handle handle = checkHandle(args, index);
        EntityView entity = resolve(ApiCore.requireState(), handle);
        if (entity == null) {
            throw Lua.error("entity %d is no longer there", handle.id);
        }
        return entity;
    }

    private static LuaTable entityType(Script script) {
        LuaTable methods = new LuaTable();
        methods.set("attack", Lua.fn(args -> {
            EntityView entity = requireEntity(args, 1);
            ApiCore.requirePlayer();
            Game.attackEntity(entity.handle);
            if (Lua.optBoolean(args, 2, true)) {
                Game.swingHand();
            }

            Game.setAttackCooldown(0);
            return LuaValue.NONE;
        }));
        methods.set("distanceTo", Lua.fn(args -> {
            EntityView entity = requireEntity(args, 1);
            Vec3 point = Lua.readVec3(args, 2).value();
            return LuaValue.valueOf(TargetFinder.hitboxDistance(point, entity.tickBoxMin(), entity.tickBoxMax()));
        }));
        methods.set("aimPoint", Lua.fn(args -> {
            EntityView entity = requireEntity(args, 1);
            LocalPlayerView player = ApiCore.requirePlayer();
            return script.vec3(TargetFinder.aimPoint(player.eyePos, entity, Lua.optFloat(args, 2, 0.1f)));
        }));
        methods.set("box", Lua.fn(args -> {
            EntityView entity = requireEntity(args, 1);
            return LuaValue.varargsOf(script.vec3(entity.boxMin()), script.vec3(entity.boxMax()));
        }));
        methods.set("name", Lua.fn(args -> LuaValue.valueOf(Game.entityName(requireEntity(args, 1).handle))));

        LuaTable type = new LuaTable();
        type.set(LuaValue.INDEX, Lua.fn(args -> {
            Handle handle = checkHandle(args, 1);
            String key = args.checkjstring(2);
            LuaValue method = methods.rawget(key);
            if (!method.isnil()) {
                return method;
            }
            if (key.equals("id")) {
                return LuaValue.valueOf(handle.id);
            }
            EntityView entity = resolve(ApiCore.requireState(), handle);
            if (key.equals("valid")) {
                return LuaValue.valueOf(entity != null);
            }
            return entity == null ? LuaValue.NIL : entityField(script, entity, key);
        }));
        type.set(LuaValue.TOSTRING, Lua.fn(args -> LuaValue.valueOf("entity(" + checkHandle(args, 1).id + ")")));
        type.set(LuaValue.EQ, Lua.fn(args ->
                LuaValue.valueOf(checkHandle(args, 1).id == checkHandle(args, 2).id)));
        type.set("__name", "entity");
        return type;
    }

    private static LuaValue entityField(Script script, EntityView entity, String key) {
        return switch (key) {
            case "x" -> LuaValue.valueOf(entity.pos.x());
            case "y" -> LuaValue.valueOf(entity.pos.y());
            case "z" -> LuaValue.valueOf(entity.pos.z());
            case "pos" -> script.vec3(entity.pos);
            case "tickPos" -> script.vec3(entity.tickPos);
            case "eye" -> script.vec3(entity.eyePos);
            case "player" -> LuaValue.valueOf(entity.isPlayer);

            case "living" -> LuaValue.TRUE;
            case "crystal" -> LuaValue.FALSE;
            case "invisible" -> LuaValue.valueOf(entity.invisible);
            case "sneaking" -> LuaValue.valueOf(entity.sneaking);
            case "onGround" -> LuaValue.valueOf(entity.onGround);
            case "blocking" -> LuaValue.valueOf(entity.blocking);
            case "width" -> LuaValue.valueOf(entity.width);
            case "height" -> LuaValue.valueOf(entity.height);
            case "yaw" -> LuaValue.valueOf(entity.yaw);
            case "pitch" -> LuaValue.valueOf(entity.pitch);
            case "bodyYaw" -> LuaValue.valueOf(entity.bodyYaw);
            case "health" -> LuaValue.valueOf(entity.health);
            case "maxHealth" -> LuaValue.valueOf(entity.maxHealth);
            case "absorption" -> LuaValue.valueOf(entity.absorption);
            case "armor" -> LuaValue.valueOf(entity.armor);
            case "hurtTime" -> LuaValue.valueOf(entity.hurtTime);
            case "distance" -> LuaValue.valueOf(entity.distance);
            default -> LuaValue.NIL;
        };
    }

    private static TargetFinder.Filter readFilter(LuaValue options) {
        TargetFinder.Filter filter = new TargetFinder.Filter();
        filter.range = 4.0f;
        if (!options.istable()) {
            return filter;
        }
        filter.range = (float) Lua.fieldNumber(options, "range", filter.range);
        filter.players = Lua.fieldBoolean(options, "players", filter.players);
        filter.mobs = Lua.fieldBoolean(options, "mobs", filter.mobs);
        filter.invisibles = Lua.fieldBoolean(options, "invisibles", filter.invisibles);
        return filter;
    }

    private static LuaTable entities(Script script) {
        LuaTable entities = new LuaTable();
        entities.set("all", Lua.fn(args -> {
            FrameState state = ApiCore.requireState();
            boolean filtered = args.arg1().istable();
            TargetFinder.Filter filter = readFilter(args.arg1());
            LuaTable list = new LuaTable();
            for (EntityView entity : state.entities) {
                if (filtered && ((entity.isPlayer ? !filter.players : !filter.mobs)
                        || (!filter.invisibles && entity.invisible) || entity.distance > filter.range)) {
                    continue;
                }
                list.insert(0, entity(script, entity.id));
            }
            return list;
        }));
        entities.set("target", Lua.fn(args -> {
            FrameState state = ApiCore.requireState();
            TargetFinder.Sort sort = TargetFinder.Sort.DISTANCE;
            if (args.arg(2).isstring()) {
                sort = switch (args.arg(2).tojstring()) {
                    case "health" -> TargetFinder.Sort.HEALTH;
                    case "angle" -> TargetFinder.Sort.ANGLE;
                    case "hurttime", "hurtTime" -> TargetFinder.Sort.HURT_TIME;
                    default -> TargetFinder.Sort.DISTANCE;
                };
            }
            EntityView target = TargetFinder.selectTarget(state, readFilter(args.arg1()), sort);
            return target == null ? LuaValue.NIL : entity(script, target.id);
        }));
        entities.set("byId", Lua.fn(args -> {
            FrameState state = ApiCore.requireState();
            int id = args.checkint(1);
            for (EntityView entity : state.entities) {
                if (entity.id == id) {
                    return entity(script, id);
                }
            }
            return LuaValue.NIL;
        }));
        entities.set("count", Lua.fn(args -> LuaValue.valueOf(ApiCore.requireState().entities.size())));
        return entities;
    }

    private static LuaValue playerField(Script script, String key) {
        LocalPlayerView player = ApiCore.requireState().player;
        if (key.equals("valid")) {
            return LuaValue.valueOf(player.valid);
        }
        if (!player.valid) {
            return LuaValue.NIL;
        }
        return switch (key) {
            case "x" -> LuaValue.valueOf(player.pos.x());
            case "y" -> LuaValue.valueOf(player.pos.y());
            case "z" -> LuaValue.valueOf(player.pos.z());
            case "pos" -> script.vec3(player.pos);
            case "eye" -> script.vec3(player.eyePos);
            case "velocity" -> script.vec3(player.velocity);
            case "speed" -> LuaValue.valueOf(player.velocity.horizontalLength());
            case "yaw" -> LuaValue.valueOf(player.yaw);
            case "pitch" -> LuaValue.valueOf(player.pitch);
            case "lastYaw" -> LuaValue.valueOf(player.lastYaw);
            case "lastPitch" -> LuaValue.valueOf(player.lastPitch);
            case "eyeHeight" -> LuaValue.valueOf(player.eyeHeight);
            case "health" -> LuaValue.valueOf(player.health);
            case "maxHealth" -> LuaValue.valueOf(player.maxHealth);
            case "armor" -> LuaValue.valueOf(player.armor);
            case "onGround" -> LuaValue.valueOf(player.onGround);
            case "sneaking" -> LuaValue.valueOf(player.sneaking);
            case "sprinting" -> LuaValue.valueOf(player.sprinting);
            case "jumping" -> LuaValue.valueOf(player.jumping);
            case "usingItem" -> LuaValue.valueOf(player.usingItem);
            case "fallDistance" -> LuaValue.valueOf(player.fallDistance);

            case "cooldown" -> LuaValue.valueOf(1.0);
            case "age" -> LuaValue.valueOf(player.age);
            default -> LuaValue.NIL;
        };
    }

    private static Game.StackView stackAt(int slot) {
        ApiCore.requirePlayer();
        if (slot < 0 || slot > OFFHAND_SLOT) {
            throw Lua.error("slot %d is outside the inventory", slot);
        }

        return slot == OFFHAND_SLOT ? Game.StackView.EMPTY : Game.stack(slot);
    }

    private static double durability(Game.StackView stack) {
        return Maths.clamp(1.0 - (double) stack.damage() / stack.maxDamage(), 0.0, 1.0);
    }

    private static LuaTable stackTable(Game.StackView stack, int slot) {
        LuaTable table = new LuaTable();
        table.set("slot", slot);
        table.set("id", stack.id());
        table.set("count", stack.count());
        table.set("damage", stack.damage());
        table.set("maxDamage", stack.maxDamage());

        table.set("durability", stack.maxDamage() > 0 ? durability(stack) : 1.0);
        return table;
    }

    private static int[] slotRange(String where) {
        return switch (where) {
            case "", "all" -> new int[] {0, OFFHAND_SLOT};
            case "hotbar" -> new int[] {0, 8};
            case "main" -> new int[] {9, 35};
            case "armor", "armour" -> new int[] {36, 39};
            case "offhand" -> new int[] {OFFHAND_SLOT, OFFHAND_SLOT};
            default -> null;
        };
    }

    private static int[] rangeArgument(Varargs args, int index, boolean strict) {
        LuaValue first = args.arg(index);
        if (first.type() == LuaValue.TSTRING) {
            int[] range = slotRange(first.tojstring());
            if (range == null && strict) {
                throw Lua.error("unknown part of the inventory '%s'", first.tojstring());
            }
            return range != null ? range : new int[] {0, OFFHAND_SLOT};
        }
        if (first.isnumber()) {
            int from = first.toint();
            return new int[] {from, args.optint(index + 1, strict ? from : OFFHAND_SLOT)};
        }
        return new int[] {0, OFFHAND_SLOT};
    }

    private static LuaTable inventory() {
        LuaTable inventory = new LuaTable();

        inventory.set("holding", Lua.fn(args -> {
            ApiCore.requirePlayer();
            return LuaValue.valueOf(Game.holdingItem(lookup(ITEM_KINDS, args, 1, "item")));
        }));
        inventory.set("find", Lua.fn(args -> {
            ApiCore.requirePlayer();
            int slot = Game.findSlot(lookup(ITEM_KINDS, args, 1, "item"), args.optint(2, 0), args.optint(3, 35));
            return slot < 0 ? LuaValue.NIL : LuaValue.valueOf(slot);
        }));
        inventory.set("slot", Lua.fn(args -> {
            ApiCore.requirePlayer();
            return LuaValue.valueOf(Game.selectedSlot());
        }));
        inventory.set("select", Lua.fn(args -> {
            ApiCore.requirePlayer();
            int slot = args.checkint(1);
            if (slot < 0 || slot > 8) {
                throw Lua.error("hotbar slots are 0 to 8, not %d", slot);
            }
            Game.setSelectedSlot(slot);
            return LuaValue.NONE;
        }));
        inventory.set("swap", Lua.fn(args -> {
            ApiCore.requirePlayer();
            return LuaValue.valueOf(Game.swapSlot(args.checkint(1), args.optint(2, -1)));
        }));
        inventory.set("handEmpty", Lua.fn(args -> {
            ApiCore.requirePlayer();
            return LuaValue.valueOf(Game.handEmpty());
        }));
        inventory.set("holdingBlock", Lua.fn(args -> {
            ApiCore.requirePlayer();
            return LuaValue.valueOf(Game.holdingBlock());
        }));
        inventory.set("open", Lua.fn(args -> {
            ApiCore.requirePlayer();
            Game.openInventory();
            return LuaValue.NONE;
        }));
        inventory.set("isOpen", Lua.fn(args -> {
            ApiCore.requireContext();
            return LuaValue.valueOf(Game.inventoryOpen());
        }));
        inventory.set("stack", Lua.fn(args -> {
            int slot = args.checkint(1);
            Game.StackView stack = stackAt(slot);
            return stack.present() ? stackTable(stack, slot) : LuaValue.NIL;
        }));
        inventory.set("id", Lua.fn(args -> {
            Game.StackView stack = stackAt(args.checkint(1));
            return stack.present() ? LuaValue.valueOf(stack.id()) : LuaValue.NIL;
        }));
        inventory.set("count", Lua.fn(args -> {
            Game.StackView stack = stackAt(args.checkint(1));
            return LuaValue.valueOf(stack.present() ? stack.count() : 0);
        }));
        inventory.set("durability", Lua.fn(args -> {
            Game.StackView stack = stackAt(args.checkint(1));
            return stack.present() && stack.maxDamage() > 0 ? LuaValue.valueOf(durability(stack)) : LuaValue.NIL;
        }));
        inventory.set("items", Lua.fn(args -> {
            int[] range = rangeArgument(args, 1, true);
            LuaTable list = new LuaTable();
            for (int slot = range[0]; slot <= range[1]; slot++) {
                Game.StackView stack = stackAt(slot);
                if (stack.present()) {
                    list.insert(0, stackTable(stack, slot));
                }
            }
            return list;
        }));
        inventory.set("findItem", Lua.fn(args -> {
            String wanted = args.checkjstring(1);
            int[] range = rangeArgument(args, 2, false);
            for (int slot = range[0]; slot <= range[1]; slot++) {
                Game.StackView stack = stackAt(slot);
                if (stack.present() && stack.id().equals(wanted)) {
                    return LuaValue.valueOf(slot);
                }
            }
            return LuaValue.NIL;
        }));
        inventory.set("total", Lua.fn(args -> {
            String wanted = args.checkjstring(1);
            int[] range = args.arg(2).type() == LuaValue.TSTRING ? rangeArgument(args, 2, false)
                    : new int[] {0, OFFHAND_SLOT};
            int total = 0;
            for (int slot = range[0]; slot <= range[1]; slot++) {
                Game.StackView stack = stackAt(slot);
                if (stack.present() && stack.id().equals(wanted)) {
                    total += stack.count();
                }
            }
            return LuaValue.valueOf(total);
        }));
        inventory.set("hold", Lua.fn(args -> {
            ApiCore.requirePlayer();
            String wanted = args.checkjstring(1);
            for (int slot = 0; slot <= 8; slot++) {
                Game.StackView stack = Game.stack(slot);
                if (stack.present() && stack.id().equals(wanted)) {
                    Game.setSelectedSlot(slot);
                    return LuaValue.valueOf(slot);
                }
            }
            return LuaValue.NIL;
        }));
        return inventory;
    }

    private static LuaTable effectTable(Game.EffectView effect) {
        LuaTable table = new LuaTable();
        table.set("id", effect.id());

        table.set("level", effect.amplifier() + 1);
        table.set("amplifier", effect.amplifier());
        table.set("duration", effect.duration());
        table.set("seconds", effect.duration() / 20.0);
        return table;
    }

    private static List<Game.EffectView> effectsOf(Varargs args, int index) {
        if (args.isnoneornil(index)) {
            return Game.statusEffects(ApiCore.requirePlayer().handle);
        }
        return Game.statusEffects(requireEntity(args, index).handle);
    }

    private static Game.EffectView findEffect(Varargs args) {
        String wanted = args.checkjstring(1);
        for (Game.EffectView effect : effectsOf(args, 2)) {
            if (effect.id().equals(wanted)) {
                return effect;
            }
        }
        return null;
    }

    private static LuaTable effects() {
        LuaTable effects = new LuaTable();
        effects.set("list", Lua.fn(args -> {
            LuaTable list = new LuaTable();
            for (Game.EffectView effect : effectsOf(args, 1)) {
                list.insert(0, effectTable(effect));
            }
            return list;
        }));
        effects.set("get", Lua.fn(args -> {
            Game.EffectView effect = findEffect(args);
            return effect == null ? LuaValue.NIL : effectTable(effect);
        }));
        effects.set("has", Lua.fn(args -> LuaValue.valueOf(findEffect(args) != null)));
        effects.set("level", Lua.fn(args -> {
            Game.EffectView effect = findEffect(args);
            return LuaValue.valueOf(effect == null ? 0 : effect.amplifier() + 1);
        }));
        effects.set("duration", Lua.fn(args -> {
            Game.EffectView effect = findEffect(args);
            return LuaValue.valueOf(effect == null ? 0 : effect.duration());
        }));
        return effects;
    }

    private static LuaTable world(Script script) {
        LuaTable world = new LuaTable();
        world.set("isAir", Lua.fn(args -> {
            requireWorld();
            return LuaValue.valueOf(Game.isBlockAir(args.checkint(1), args.checkint(2), args.checkint(3)));
        }));
        world.set("isReplaceable", Lua.fn(args -> {
            requireWorld();
            return LuaValue.valueOf(Game.isBlockReplaceable(args.checkint(1), args.checkint(2), args.checkint(3)));
        }));
        world.set("isSolid", Lua.fn(args -> {
            requireWorld();
            return LuaValue.valueOf(!Game.isBlockReplaceable(args.checkint(1), args.checkint(2), args.checkint(3)));
        }));
        world.set("is", Lua.fn(args -> {
            requireWorld();
            int x = args.checkint(1);
            int y = args.checkint(2);
            int z = args.checkint(3);
            return LuaValue.valueOf(Game.blockIs(x, y, z, lookup(BLOCK_KINDS, args, 4, "block")));
        }));
        world.set("block", Lua.fn(args -> {
            requireWorld();
            return LuaValue.valueOf(Game.blockId(args.checkint(1), args.checkint(2), args.checkint(3)));
        }));

        world.set("anchorCharges", Lua.fn(args -> {
            requireWorld();
            args.checkint(1);
            args.checkint(2);
            args.checkint(3);
            return LuaValue.valueOf(0);
        }));
        world.set("boxEmpty", Lua.fn(args -> {
            requireWorld();
            Lua.Vec3Arg minimum = Lua.readVec3(args, 1);
            Vec3 maximum = Lua.readVec3(args, minimum.next()).value();
            return LuaValue.valueOf(Game.boxEmpty(minimum.value(), maximum));
        }));
        world.set("raycast", Lua.fn(args -> {
            requireWorld();
            Lua.Vec3Arg origin = Lua.readVec3(args, 1);
            Lua.Vec3Arg direction = Lua.readVec3(args, origin.next());
            Raycast.BlockHit hit = Raycast.raycastBlocks(origin.value(), direction.value(),
                    args.optdouble(direction.next(), 5.0));
            if (hit == null) {
                return LuaValue.NIL;
            }
            LuaTable result = new LuaTable();
            result.set("x", hit.x());
            result.set("y", hit.y());
            result.set("z", hit.z());
            result.set("face", hit.face().getName());
            result.set("distance", hit.distance());
            result.set("point", script.vec3(hit.point()));
            return result;
        }));
        world.set("crosshair", Lua.fn(args -> {
            ApiCore.requireContext();
            CrosshairTarget target = Game.crosshair();
            LuaTable result = new LuaTable();
            result.set("kind", target.kind().name().toLowerCase(Locale.ROOT));
            result.set("x", target.x());
            result.set("y", target.y());
            result.set("z", target.z());
            result.set("face", target.face().getName());
            result.set("entityId", target.entityId());
            result.set("crystal", LuaValue.valueOf(target.crystal()));
            result.set("point", script.vec3(target.point()));
            return result;
        }));
        world.set("time", Lua.fn(args -> LuaValue.valueOf((double) ApiCore.requireState().worldTime)));

        world.set("entitiesIn", Lua.fn(args -> {
            FrameState state = ApiCore.requireState();
            Lua.Vec3Arg first = Lua.readVec3(args, 1);
            Vec3 a = first.value();
            Vec3 b = Lua.readVec3(args, first.next()).value();
            Vec3 minimum = new Vec3(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z()));
            Vec3 maximum = new Vec3(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z()));
            LuaTable list = new LuaTable();
            for (EntityView entity : state.entities) {
                Vec3 position = entity.tickPos;
                if (position.x() < minimum.x() || position.x() > maximum.x() || position.y() < minimum.y()
                        || position.y() > maximum.y() || position.z() < minimum.z() || position.z() > maximum.z()) {
                    continue;
                }
                list.insert(0, entity(script, entity.id));
            }
            return list;
        }));
        return world;
    }

    private static LuaTable rotation(Script script) {
        LuaTable rotation = new LuaTable();
        rotation.set("set", Lua.fn(args -> {
            ApiCore.requireContext();
            RotationTarget target = new RotationTarget();
            LuaValue options = args.arg(3);
            if (args.arg1().istable()) {
                options = args.arg1();
                target.yaw = (float) Lua.fieldNumber(options, "yaw", 0.0);
                target.pitch = (float) Lua.fieldNumber(options, "pitch", 0.0);
            } else {
                target.yaw = Lua.checkFloat(args, 1);
                target.pitch = Lua.checkFloat(args, 2);
            }
            if (options.istable()) {
                target.speed = (float) Lua.fieldNumber(options, "speed", target.speed);
                target.smoothing = (float) Lua.fieldNumber(options, "smoothing", target.smoothing);
                target.jitter = Lua.fieldInt(options, "jitter", target.jitter);
                target.silent = Lua.fieldBoolean(options, "silent", target.silent);
                target.priority = Lua.fieldInt(options, "priority", target.priority);
            }
            target.pitch = Maths.clamp(target.pitch, -90.0f, 90.0f);
            RotationEngine.submit(target);
            return LuaValue.NONE;
        }));
        LuaValue aimAt = Lua.fn(args -> {
            LocalPlayerView player = ApiCore.requirePlayer();
            RotationTarget target = RotationEngine.aimAt(player.eyePos, Lua.readVec3(args, 1).value(), player.yaw);
            return LuaValue.varargsOf(LuaValue.valueOf(target.yaw), LuaValue.valueOf(target.pitch));
        });
        rotation.set("aimAt", aimAt);
        rotation.set("toPoint", aimAt);
        rotation.set("sent", Lua.fn(args ->
                LuaValue.varargsOf(LuaValue.valueOf(RotationEngine.sentYaw()), LuaValue.valueOf(RotationEngine.sentPitch()))));
        rotation.set("current", Lua.fn(args ->
                LuaValue.varargsOf(LuaValue.valueOf(RotationEngine.yaw()), LuaValue.valueOf(RotationEngine.pitch()))));

        rotation.set("player", Lua.fn(args -> {
            LocalPlayerView player = ApiCore.requirePlayer();
            return LuaValue.varargsOf(LuaValue.valueOf(player.yaw), LuaValue.valueOf(player.pitch));
        }));
        rotation.set("step", Lua.fn(args -> LuaValue.valueOf(RotationEngine.step())));
        rotation.set("engaged", Lua.fn(args -> LuaValue.valueOf(RotationEngine.engaged())));
        rotation.set("idling", Lua.fn(args -> LuaValue.valueOf(!RotationEngine.engaged())));
        rotation.set("reset", Lua.fn(args -> {
            RotationEngine.reset();
            return LuaValue.NONE;
        }));
        rotation.set("lookVector", Lua.fn(args ->
                script.vec3(Raycast.lookVector(Lua.checkFloat(args, 1), Lua.checkFloat(args, 2)))));
        return rotation;
    }

    private static LuaTable action() {
        LuaTable action = new LuaTable();

        action.set("swing", Lua.fn(args -> {
            ApiCore.requirePlayer();
            Game.swingHand();
            return LuaValue.NONE;
        }));
        action.set("place", Lua.fn(args -> {
            ApiCore.requirePlayer();
            int x = args.checkint(1);
            int y = args.checkint(2);
            int z = args.checkint(3);
            EnumFacing face = checkFace(args, 4);
            Lua.Vec3Arg hit = Lua.readVec3(args, 5);
            return LuaValue.valueOf(Game.placeBlock(x, y, z, face, hit.value(), Lua.optBoolean(args, hit.next(), false)));
        }));
        action.set("breakBlock", Lua.fn(args -> {
            ApiCore.requireContext();
            Game.attackBlock(args.checkint(1), args.checkint(2), args.checkint(3), checkFace(args, 4),
                    Lua.optBoolean(args, 5, false));
            return LuaValue.NONE;
        }));
        action.set("jump", Lua.fn(args -> {
            ApiCore.requirePlayer();
            Game.jump();
            return LuaValue.NONE;
        }));
        action.set("sprint", Lua.fn(args -> {
            ApiCore.requirePlayer();
            Game.setSprinting(Lua.optBoolean(args, 1, true));
            return LuaValue.NONE;
        }));
        action.set("click", Lua.fn(args -> {
            ApiCore.requireContext();

            Game.queueClick(Lua.optBoolean(args, 1, false));
            return LuaValue.NONE;
        }));
        action.set("hold", Lua.fn(args -> {
            ApiCore.requireContext();
            Game.pressKey(Lua.optBoolean(args, 1, false), Lua.optBoolean(args, 2, true));
            return LuaValue.NONE;
        }));
        action.set("key", Lua.fn(args -> {
            ApiCore.requireContext();
            String name = args.checkjstring(1);
            if (!Game.setBinding(name, Lua.optBoolean(args, 2, true))) {
                throw Lua.error("'%s' is not a movement binding: forward, back, left, right, jump, sneak, "
                        + "sprint, attack or use", name);
            }
            return LuaValue.NONE;
        }));
        action.set("setVelocity", Lua.fn(args -> {
            LocalPlayerView player = ApiCore.requirePlayer();
            Game.setVelocity(player.handle, Lua.readVec3(args, 1).value());
            return LuaValue.NONE;
        }));

        action.set("look", Lua.fn(args -> {
            ApiCore.requirePlayer();
            Game.setRotation(Lua.checkFloat(args, 1), Lua.checkFloat(args, 2), Lua.optBoolean(args, 3, false));
            return LuaValue.NONE;
        }));
        action.set("sound", Lua.fn(args -> {
            ApiCore.requirePlayer();
            Game.playSound(args.checkjstring(1), Lua.optFloat(args, 2, 1.0f), Lua.optFloat(args, 3, 1.0f));
            return LuaValue.TRUE;
        }));

        action.set("canInteract", Lua.fn(args -> {
            ApiCore.requireContext();
            return LuaValue.TRUE;
        }));
        action.set("attackCooldown", Lua.fn(args -> {
            ApiCore.requireContext();
            Game.setAttackCooldown(args.optint(1, 0));
            return LuaValue.NONE;
        }));
        action.set("useCooldown", Lua.fn(args -> {
            ApiCore.requireContext();
            Game.setItemUseCooldown(args.optint(1, 0));
            return LuaValue.NONE;
        }));
        action.set("closeScreen", Lua.fn(args -> {
            ApiCore.requireContext();
            Game.closeScreen();
            return LuaValue.NONE;
        }));
        action.set("respawn", Lua.fn(args -> {
            ApiCore.requirePlayer();
            Game.respawn();
            return LuaValue.TRUE;
        }));
        return action;
    }

    private static LuaTable input() {
        LuaTable input = new LuaTable();
        input.set("keyHeld", Lua.fn(args -> LuaValue.valueOf(Input.keyHeld(args.checkint(1)))));
        input.set("bindHeld", Lua.fn(args -> LuaValue.valueOf(Input.bindHeld(args.checkint(1)))));
        input.set("attackHeld", Lua.fn(args -> LuaValue.valueOf(Input.attackHeld())));
        input.set("useHeld", Lua.fn(args -> LuaValue.valueOf(Input.useHeld())));
        input.set("cursor", Lua.fn(args ->
                LuaValue.varargsOf(LuaValue.valueOf(Input.cursorX()), LuaValue.valueOf(Input.cursorY()))));
        return input;
    }

    static void install(Script script, LuaTable cold) {
        script.entityType = entityType(script);
        cold.set("entities", entities(script));
        cold.set("inventory", inventory());
        cold.set("effects", effects());
        cold.set("world", world(script));
        cold.set("rotation", rotation(script));
        cold.set("action", action());
        cold.set("input", input());

        LuaTable player = new LuaTable();
        LuaTable meta = new LuaTable();
        meta.set(LuaValue.INDEX, Lua.fn(args -> playerField(script, args.checkjstring(2))));
        meta.set("__name", "player");
        player.setmetatable(meta);
        cold.set("player", player);
    }
}
