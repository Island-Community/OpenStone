cold.script {
    name = "Coords",
    version = "1.0",
    author = "Island Community",
    description = "Position, facing and speed",
}

local coords

coords = cold.module {
    name = "ScriptCoords",
    description = "Position, facing and speed in the corner",
    category = "render",
    subcategory = "basic",
    icon = "move-3d",
    settings = {
        { name = "X", type = "number", value = 3, min = 0, max = 100, decimals = 0,
          description = "Percent across the screen" },
        { name = "Y", type = "number", value = 78, min = 0, max = 100, decimals = 0 },
        { name = "Size", type = "number", value = 8, min = 5, max = 16, decimals = 1 },
        { name = "Rotation", type = "boolean", value = false,
          description = "Also show the rotation the server holds" },
        { name = "Colour", type = "color", value = cold.rgb(0xE2D0F9) },
    },
}

local compass = { "south", "south west", "west", "north west",
                  "north", "north east", "east", "south east" }

local function facing(yaw)
    local index = math.floor(((yaw % 360) + 360) % 360 / 45 + 0.5) % 8
    return compass[index + 1]
end

function coords.onRender()
    if not cold.player.valid then return end

    local settings = coords.settings
    local unit = cold.render.scale()
    local width, height = cold.render.size()
    local size = settings.Size * unit
    local x = width * settings.X / 100
    local y = height * settings.Y / 100
    local pos = cold.player.pos

    local lines = {
        string.format("%.1f  %.1f  %.1f", pos.x, pos.y, pos.z),
        string.format("%s   %.1f b/s", facing(cold.player.yaw), cold.player.speed * 20),
    }
    if settings.Rotation then
        local yaw, pitch = cold.rotation.sent()
        lines[#lines + 1] = string.format("sent %.2f / %.2f", yaw, pitch)
    end

    for index, line in ipairs(lines) do
        cold.render.text(line, x, y + (index - 1) * size * 1.35, size, settings.Colour,
            { font = "medium", shadow = true, cap = true })
    end
end
