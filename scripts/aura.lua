cold.script {
    name = "Aura",
    version = "1.0",
    author = "Island Community",
    description = "Kill aura, rotation-gated",
}

local aura

aura = cold.module {
    name = "ScriptAura",
    description = "Aims at the best target and hits it once the server agrees",
    category = "combat",
    subcategory = "extreme",
    icon = "swords",
    settings = {
        { name = "Range", type = "number", value = 3.0, min = 1.0, max = 6.0, decimals = 2 },
        { name = "Targets", type = "choice", choices = { "Players", "Mobs", "All" }, value = "Players" },
        { name = "Sort", type = "choice", choices = { "Distance", "Health", "Angle" }, value = "Distance" },
        { name = "Silent", type = "boolean", value = true,
          description = "Send the rotation without moving the camera" },
        { name = "Turn speed", type = "number", value = 180, min = 20, max = 360, decimals = 0 },
        { name = "FOV", type = "number", value = 180, min = 30, max = 360, decimals = 0 },
        { name = "Tolerance", type = "number", value = 4, min = 0.5, max = 20, decimals = 1,
          description = "Degrees the sent rotation may be off by and still swing" },
    },
}

local targetId = nil

function aura.onDisable()
    targetId = nil
end

function aura.suffix()
    return aura.settings.Silent and "silent" or nil
end

function aura.onTick()
    if not cold.player.valid or cold.client.screenOpen then
        targetId = nil
        return
    end

    local settings = aura.settings
    local target = cold.entities.target({
        range = settings.Range,
        players = settings.Targets ~= "Mobs",
        mobs = settings.Targets ~= "Players",
    }, settings.Sort:lower())

    if not target then
        targetId = nil
        return
    end
    targetId = target.id

    local point = target:aimPoint(0.12)
    local yaw, pitch = cold.rotation.aimAt(point)

    if math.abs(cold.util.angleDiff(cold.player.yaw, yaw)) > settings.FOV * 0.5 then
        return
    end

    cold.rotation.set(yaw, pitch, {
        speed = settings["Turn speed"],
        smoothing = 0.25,
        silent = settings.Silent,
        priority = 5,
    })

    local sentYaw, sentPitch = cold.rotation.sent()
    local tolerance = settings.Tolerance
    if math.abs(cold.util.angleDiff(sentYaw, yaw)) > tolerance then return end
    if math.abs(sentPitch - pitch) > tolerance then return end

    if cold.player.cooldown < 0.99 then return end

    target:attack()
end

function aura.onRender()
    if not targetId then return end
    local target = cold.entities.byId(targetId)
    if not target or not target.valid then return end

    local x, y, width, height = cold.render.entityBox(target)
    if not x then return end

    local unit = cold.render.scale()
    local accent = cold.accent()
    cold.render.outline(x, y, width, height, accent:fade(0.85), math.max(1, unit * 0.5), unit)
    cold.render.glow(x, y, width, height, accent:fade(0.25), unit * 6, unit)
end
