cold.script {
    name = "Self test",
    version = "1.0",
    author = "Island Community",
    description = "Exercises the script API and reports failures",
}

local results = {}
local failures = {}

local test

local function check(name, ok, detail)
    results[#results + 1] = name
    if not ok then
        failures[#failures + 1] = name .. (detail and ("  " .. tostring(detail)) or "")
    end
end

local function attempt(name, fn)
    local ok, err = pcall(fn)
    check(name, ok, ok and nil or err)
    return ok
end

local counters = { tick = 0, frame = 0, render = 0, render3d = 0, key = 0, char = 0,
                   mouse = 0, move = 0, scroll = 0, screen = 0, slot = 0, toggled = 0,
                   effect = 0, spawn = 0, remove = 0, death = 0, health = 0 }

for _, name in ipairs({ "tick", "frame", "render", "render3d", "key", "chartyped", "mouse",
                        "mousemove", "scroll", "screenopen", "screenclose", "entityspawn",
                        "entityremove", "entitydeath", "healthchange", "death", "respawn",
                        "slotchange", "moduletoggled", "effectadded", "effectremoved",
                        "worldload", "worldleave" }) do
    check("event " .. name, pcall(function()
        local handle = cold.on(name, function() end)
        cold.off(name, handle)
    end))
end

check("event aliases", pcall(function()
    local a = cold.on("entity_death", function() end)
    local b = cold.on("update", function() end)
    cold.off("entitydeath", a)
    cold.off("tick", b)
end))

cold.on("tick", function() counters.tick = counters.tick + 1 end)
cold.on("frame", function() counters.frame = counters.frame + 1 end)
cold.on("key", function() counters.key = counters.key + 1 end)
cold.on("chartyped", function() counters.char = counters.char + 1 end)
cold.on("mouse", function() counters.mouse = counters.mouse + 1 end)
cold.on("mousemove", function() counters.move = counters.move + 1 end)
cold.on("scroll", function() counters.scroll = counters.scroll + 1 end)
cold.on("screenopen", function() counters.screen = counters.screen + 1 end)
cold.on("screenclose", function() counters.screen = counters.screen + 1 end)
cold.on("slotchange", function() counters.slot = counters.slot + 1 end)
cold.on("moduletoggled", function() counters.toggled = counters.toggled + 1 end)
cold.on("effectadded", function() counters.effect = counters.effect + 1 end)
cold.on("effectremoved", function() counters.effect = counters.effect + 1 end)
cold.on("entityspawn", function() counters.spawn = counters.spawn + 1 end)
cold.on("entityremove", function() counters.remove = counters.remove + 1 end)
cold.on("entitydeath", function() counters.death = counters.death + 1 end)
cold.on("healthchange", function() counters.health = counters.health + 1 end)

check("vec3 arithmetic", (cold.vec3(1, 2, 3) + cold.vec3(1, 1, 1)).y == 3)
check("vec3 length", math.abs(cold.vec3(3, 4, 0):length() - 5) < 1e-9)
check("vec functions", math.abs(cold.vec.distance(cold.vec3(0, 0, 0), cold.vec3(0, 3, 4)) - 5) < 1e-9)
check("colour hex", cold.rgb(0x58C6FF):hex() == "#58C6FF")
check("colour mix", math.abs(cold.rgb(0x000000):mix(cold.rgb(0xFFFFFF), 0.5).r - 0.5) < 0.01)
check("util angleDiff", cold.util.angleDiff(170, -170) == 20)
check("util wrapDegrees", cold.util.wrapDegrees(-270) == 90)
check("ease", math.abs(cold.ease.outQuint(1) - 1) < 1e-9)
check("string.split", #string.split("a,b,c", ",") == 3)
check("table.filter", #table.filter({ 1, 2, 3, 4 }, function(v) return v % 2 == 0 end) == 2)
check("timer", cold.timer():finished(0))

local encoded = cold.json.encode({ a = 1, b = "two", c = { true, false }, d = { x = 1.5 } })
local decoded = cold.json.decode(encoded)
check("json round trip", decoded and decoded.a == 1 and decoded.b == "two" and
    decoded.c[1] == true and decoded.d.x == 1.5, encoded)
check("json array", cold.json.encode({ 1, 2, 3 }) == "[1,2,3]", cold.json.encode({ 1, 2, 3 }))
check("json bad input", cold.json.decode("{not json") == nil)

local db = cold.storage("selftest")
db:set("count", (db:get("count", 0)) + 1):set("word", "hello"):save()
check("storage", cold.storage("selftest"):get("word") == "hello")

check("config", (function()
    cold.config.set("selftest", 42)
    cold.config.save()
    return cold.config.get("selftest", 0) == 42
end)())

cold.lang.load({
    en_us = { greeting = "hello %s", bye = "bye" },
    ru_ru = { greeting = "привет %s" },
})
check("lang default", cold.lang("greeting", "world") == "hello world", cold.lang("greeting", "x"))
cold.lang.select("ru_ru")
check("lang select", cold.lang("greeting", "мир") == "привет мир")
check("lang fallback key", cold.lang("missing") == "missing")
cold.lang.select("en_us")
check("lang current", cold.lang.current() == "en_us")
check("lang has", cold.lang.has("bye") and not cold.lang.has("nope"))

check("esp namespace", type(cold.esp.box) == "function" and type(cold.esp.all) == "function")

check("keys.code", cold.keys.code("RSHIFT") == 344 and cold.keys.code("a") == 65)
check("keys.name", cold.keys.name(344) == "rshift", cold.keys.name(344))
check("keyName", cold.keyName(65) == "A")

cold.friends.add("SelfTestFriend")
check("friends add", cold.friends.is("selftestfriend"))
check("friends list", table.contains(cold.friends.list(), "SelfTestFriend"))
cold.friends.remove("SelfTestFriend")
check("friends remove", not cold.friends.is("SelfTestFriend"))

check("theme colour", cold.theme.color("accent") ~= nil)
check("theme readable", cold.theme.readable(cold.rgb(0xFFFFFF)):hex() ~= "#FFFFFF")
check("theme health", cold.theme.health(20, 20) ~= nil)
check("accent", cold.accent() ~= nil)

check("modules.list", #cold.modules.list() > 3)
local esp = cold.modules.get("ESP")
check("modules.get", esp ~= nil and esp.name == "ESP")
if esp then
    check("module category", esp.category == "Visuals", esp.category)
    check("module settings names", #esp:settingNames() > 0)
    local info = esp:setting(esp:settingNames()[1])
    check("module setting info", info ~= nil and info.name ~= nil)
end

local probe
probe = cold.module {
    name = "ScriptSelfTestProbe",
    category = "client",
    description = "settings round trip",
    settings = {
        { name = "Number", type = "number", value = 5, min = 0, max = 10, decimals = 1 },
        { name = "Choice", type = "choice", choices = { "One", "Two" }, value = "One" },
        { name = "Flag", type = "boolean", value = false },
        { name = "Tint", type = "color", value = cold.rgb(0x5BE59A) },
        { name = "Bind", type = "key", value = 0 },
        { name = "Hidden", type = "boolean", value = true,
          visible = function() return probe.settings.Flag end },
    },
}
probe.settings.Number = 7.5
probe.settings.Choice = "Two"
probe.settings.Flag = true
probe.settings.Tint = cold.rgb(0xFF0000)
probe.settings.Bind = cold.keys.F8
check("setting number", probe.settings.Number == 7.5)
check("setting choice", probe.settings.Choice == "Two")
check("setting boolean", probe.settings.Flag == true)
check("setting colour", probe.settings.Tint:hex() == "#FF0000")
check("setting key", probe.settings.Bind == cold.keys.F8)
check("module enabled write", (function()
    probe.enabled = true
    local on = probe.enabled
    probe.enabled = false
    return on and not probe.enabled
end)())

local chained = 0
probe:on("tick", function() chained = chained + 1 end)
probe:on("tick", function() chained = chained + 10 end)

local element = cold.hud.element {
    name = "SelfTestHud",
    width = 90,
    height = 22,
    x = 3,
    y = 55,
    visible = function() return false end,
    render = function(self, d) end,
}
check("hud element", element ~= nil and element.name == "SelfTestHud")
check("hud element pos", (function()
    element:pos(3, 55)
    local x, y = element:pos()
    return x == 3 and y == 55
end)())
check("hud find", cold.hud.find("SelfTestHud") ~= nil)
check("hud all", #cold.hud.all() >= 1)

local probed = false

local function probeGame()
    probed = true

    attempt("client.msg", function() cold.client.msg("selftest: checking the API") end)
    attempt("client.warn", function() cold.client.warn("selftest: warning line") end)
    attempt("client.overlay", function() cold.client.overlay("selftest") end)
    attempt("client.server", function() local _ = cold.client.server() end)
    attempt("client.gameDir", function()
        local dir = cold.client.gameDir()
        assert(type(dir) == "string" and #dir > 0, "no game directory")
    end)
    attempt("client.find", function() local _ = cold.client.find("ESP", "Mode") end)
    attempt("client.fontWidth", function()
        assert(cold.client.fontWidth("test", 10) > 0, "zero width")
    end)
    attempt("client fields", function()
        assert(type(cold.client.fps) == "number", "fps")
        assert(type(cold.client.scale) == "number", "scale")
        assert(type(cold.client.tick) == "number", "tick")
    end)

    attempt("player fields", function()
        assert(cold.player.valid, "no player")
        assert(type(cold.player.health) == "number", "health")
        assert(cold.player.pos.x ~= nil, "pos")
        assert(type(cold.player.speed) == "number", "speed")
    end)

    attempt("entities", function()
        local all = cold.entities.all()
        assert(type(all) == "table", "all")
        assert(cold.entities.count() >= #all, "count")
        local _ = cold.entities.players(64)
        local _ = cold.entities.mobs(64)
    end)

    attempt("world", function()
        local pos = cold.player.pos
        local x, y, z = math.floor(pos.x), math.floor(pos.y) - 1, math.floor(pos.z)
        local _ = cold.world.isAir(x, y, z)
        local _ = cold.world.isSolid(x, y, z)
        local _ = cold.world.block(x, y, z)
        local _ = cold.world.is(x, y, z, "obsidian")
        local _ = cold.world.anchorCharges(x, y, z)
        local _ = cold.world.boxEmpty(pos, pos:offset(1, 1, 1))
        local _ = cold.world.crosshair().kind
        local _ = cold.world.time()
        local _ = cold.world.entitiesIn(pos:offset(-8, -8, -8), pos:offset(8, 8, 8))
        local hit = cold.world.raycast(cold.player.eye, cold.rotation.lookVector(0, 90), 6)
        assert(hit == nil or hit.face ~= nil, "raycast face")
    end)

    attempt("inventory", function()
        local _ = cold.inventory.slot()
        local _ = cold.inventory.items("hotbar")
        local _ = cold.inventory.stack(cold.inventory.slot())
        local _ = cold.inventory.count(0)
        local _ = cold.inventory.durability(0)
        local _ = cold.inventory.findItem("diamond_sword")
        local _ = cold.inventory.total("obsidian")
        local _ = cold.inventory.holding("sword")
        local _ = cold.inventory.handEmpty()
        local _ = cold.inventory.holdingBlock()
        local _ = cold.inventory.isOpen()
    end)

    attempt("effects", function()
        local list = cold.effects.list()
        assert(type(list) == "table", "list")
        local _ = cold.effects.has("speed")
        local _ = cold.effects.level("speed")
        local _ = cold.effects.duration("speed")
        local _ = cold.effects.get("speed")
    end)

    attempt("rotation reads", function()
        local yaw, pitch = cold.rotation.sent()
        assert(type(yaw) == "number" and type(pitch) == "number", "sent")
        local _, _ = cold.rotation.current()
        local _, _ = cold.rotation.player()
        assert(cold.rotation.step() > 0, "step")
        local _ = cold.rotation.idling()
        local _ = cold.rotation.lookVector(0, 0)
        local ay, ap = cold.rotation.aimAt(cold.player.eye:offset(0, 0, 4))
        assert(type(ay) == "number" and type(ap) == "number", "aimAt")
        local grid = cold.rotation.gcd(yaw, yaw + 7.3)
        assert(math.abs((grid - yaw) % cold.rotation.step()) < 1e-3, "gcd off grid")
    end)

    attempt("input", function()
        local _ = cold.input.keyHeld(cold.keys.A)
        local _ = cold.input.bindHeld(0)
        local _ = cold.input.attackHeld()
        local _ = cold.input.useHeld()
        local x, y = cold.input.cursor()
        assert(type(x) == "number" and type(y) == "number", "cursor")
        local _ = cold.keys.down("LSHIFT")
    end)

    attempt("actions", function()
        cold.action.key("forward", false)
        cold.action.key("sneak", false)
        cold.action.attackCooldown(0)
        cold.action.useCooldown(0)
        cold.action.sound("entity.experience_orb.pickup", 0.35, 1.4)
        cold.action.hold(false, false)
    end)

    attempt("target", function()
        local _ = cold.target.best({ range = 6 })
        local _ = cold.target.all({ range = 6 })
        local _ = cold.target.valid(nil, {})
    end)

    attempt("notify", function()
        cold.notify("selftest running", 3)
        cold.notify.success("selftest: notify.success")
        cold.notify.info("selftest: notify.info")
    end)

    attempt("scripts", function()
        assert(#cold.scripts.list() >= 1, "list")
    end)
end

test = cold.module {
    name = "ScriptSelfTest",
    description = "Runs the script API self test and reports failures",
    category = "client",
    icon = "check",
    settings = {
        { name = "Draw", type = "boolean", value = true,
          description = "Also exercise every drawing call each frame" },
    },
}

function test.suffix()
    return #failures == 0 and "ok" or (#failures .. " failed")
end

function test.onTick()
    if not probed and cold.player.valid then
        probeGame()
        cold.log(string.format("self test: %d checks, %d failed", #results, #failures))
        for _, failure in ipairs(failures) do
            cold.log("  FAIL " .. failure)
        end
        cold.notify(string.format("self test: %d checks, %d failed", #results, #failures), 8,
            #failures == 0 and "good" or "bad")
    end
end

function test.onRender()
    counters.render = counters.render + 1

    if not cold.client.inWorld then return end
    local unit = cold.render.scale()
    local width, height = cold.render.size()
    local size = 7.5 * unit

    local colour = #failures == 0 and cold.rgb(0x5BE59A) or cold.rgb(0xFF5C6E)
    cold.render.text(string.format("self test  %d checks  %d failed", #results, #failures),
        4 * unit, 150 * unit, size * 1.15, colour, { cap = true, shadow = true })
    cold.render.text(string.format("tick %d  frame %d  render %d  key %d  char %d  mouse %d  " ..
            "move %d  scroll %d  screen %d  slot %d  toggle %d  effect %d  chain %d",
            counters.tick, counters.frame, counters.render, counters.key, counters.char,
            counters.mouse, counters.move, counters.scroll, counters.screen, counters.slot,
            counters.toggled, counters.effect, chained),
        4 * unit, 158 * unit, size, cold.rgb(0x8FD3FF), { cap = true, shadow = true })
    for index, failure in ipairs(failures) do
        cold.render.text(failure, 4 * unit, (166 + index * 8) * unit, size, cold.rgb(0xFF5C6E),
            { cap = true, shadow = true })
    end

    if not test.settings.Draw then return end

    local x = width - 130 * unit
    local y = 150 * unit
    cold.render.rect(x, y, 40 * unit, 10 * unit, cold.rgb(0x171A21, 0.8), 2 * unit)
    cold.render.gradient(x + 42 * unit, y, 40 * unit, 10 * unit, cold.rgb(0x58C6FF),
        cold.rgb(0xE2D0F9), true, 2 * unit)
    cold.render.outline(x, y + 12 * unit, 40 * unit, 10 * unit, cold.accent(), unit, 2 * unit)
    cold.render.sweep(x + 42 * unit, y + 12 * unit, 40 * unit, 10 * unit, cold.accent(), unit,
        2 * unit)
    cold.render.shadow(x, y + 24 * unit, 20 * unit, 8 * unit, cold.rgb(0x000000, 0.6), 4 * unit,
        2 * unit)
    cold.render.glow(x + 24 * unit, y + 24 * unit, 20 * unit, 8 * unit, cold.accent():fade(0.5),
        4 * unit, 2 * unit)
    cold.render.line(x, y + 36 * unit, x + 80 * unit, y + 36 * unit, unit, cold.accent())
    cold.render.ring(x + 8 * unit, y + 46 * unit, 6 * unit, unit, 0.7, cold.accent())
    cold.render.circle(x + 26 * unit, y + 46 * unit, 5 * unit, cold.rgb(0x5BE59A))
    cold.render.quad(x + 38 * unit, y + 40 * unit, x + 52 * unit, y + 42 * unit,
        x + 50 * unit, y + 52 * unit, x + 36 * unit, y + 50 * unit, cold.rgb(0xFFC55C, 0.7))
    cold.render.icon("zap", x + 56 * unit, y + 40 * unit, 10 * unit, 10 * unit, cold.accent())
    cold.render.text("outline", x, y + 56 * unit, size, cold.rgb(0xFFFFFF),
        { cap = true, outline = cold.rgb(0x000000), font = "mono" })
    cold.render.clip(x, y + 64 * unit, 40 * unit, 8 * unit)
    cold.render.text("clipped text that runs past the edge", x, y + 64 * unit, size,
        cold.rgb(0xFFFFFF), { cap = true })
    cold.render.unclip()
    local _ = cold.render.measure("test", size)
    local _ = cold.render.lineHeight(size)

    if not cold.player.valid then return end
    counters.render3d = counters.render3d + 1
    local base = cold.player.pos
    local a = base:offset(2, 0, 2)
    local b = base:offset(3, 1, 3)
    cold.render3d.line(a, b, cold.rgb(0x58C6FF))
    cold.render3d.box(a, b, cold.accent())
    cold.render3d.filledBox(base:offset(-3, 0, 2), base:offset(-2, 1, 3),
        cold.rgb(0xE2D0F9, 0.25))
    cold.render3d.marker(base:offset(0, 2, 0), 5 * unit, cold.rgb(0xFFC55C))
    cold.render3d.circle(base, 3, cold.accent():fade(0.6), 32, unit)
    cold.render3d.text(base:offset(0, 2.4, 0), "self test", 8 * unit, cold.rgb(0xFFFFFF))
    local _ = cold.render3d.visible(base)
    local nearest = cold.entities.nearest(48)
    if nearest then
        cold.render3d.entityBox(nearest, cold.rgb(0x5BE59A))
        cold.render3d.entityTracer(nearest, cold.rgb(0x5BE59A, 0.5))
        cold.esp.box(nearest, cold.rgb(0x8FD3FF))
        cold.esp.tracer(nearest, cold.rgb(0x8FD3FF, 0.4))
        cold.esp.nametag(nearest)
        cold.esp.health(nearest)
    end
    cold.esp.all({ range = 24 }, function(entity)
        cold.render3d.marker(entity.pos, 2 * unit, cold.rgb(0xE2D0F9, 0.6))
    end)
end
