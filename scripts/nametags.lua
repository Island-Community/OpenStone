cold.script {
    name = "Nametags",
    version = "1.0",
    author = "Island Community",
    description = "Names, health and boxes over players",
}

local tags

tags = cold.module {
    name = "ScriptNametags",
    description = "Name, health and a box over every player",
    category = "render",
    subcategory = "advanced",
    icon = "user-round",
    settings = {
        { name = "Range", type = "number", value = 64, min = 8, max = 128, decimals = 0 },
        { name = "Box", type = "boolean", value = true },
        { name = "Health bar", type = "boolean", value = true },
        { name = "Distance", type = "boolean", value = true },
        { name = "Colour", type = "color", value = cold.rgb(0xE2D0F9) },
    },
}

local function healthColour(fraction)
    return cold.hsv(0.33 * math.max(fraction, 0), 0.75, 1.0)
end

function tags.onRender()
    if not cold.player.valid then return end
    local settings = tags.settings
    local unit = cold.render.scale()
    local size = 7.5 * unit

    for _, entity in ipairs(cold.entities.all({ range = settings.Range, mobs = false })) do
        if entity.player and entity.health > 0 then
            local x, y, width, height = cold.render.entityBox(entity)
            if x then
                if settings.Box then
                    cold.render.outline(x, y, width, height,
                        settings.Colour:fade(0.75), math.max(1, unit * 0.5), unit)
                end

                local label = entity:name()
                if settings.Distance then
                    label = string.format("%s  %.0fm", label, entity.distance)
                end
                local text = cold.render.measure(label, size)
                local top = y - size * 1.6

                cold.render.rect(x + width * 0.5 - text * 0.5 - unit * 2, top - unit,
                    text + unit * 4, size * 1.5, cold.rgb(0x000000, 0.47), unit)
                cold.render.text(label, x + width * 0.5, top, size, settings.Colour,
                    { align = "center", font = "medium", cap = true })

                if settings["Health bar"] and entity.maxHealth > 0 then
                    local fraction = math.min(1, (entity.health + entity.absorption) / entity.maxHealth)
                    local bar = { x = x - unit * 2.5, y = y, w = unit * 1.5, h = height }
                    cold.render.rect(bar.x, bar.y, bar.w, bar.h, cold.rgb(0x000000, 0.5), unit * 0.75)
                    cold.render.rect(bar.x, bar.y + bar.h * (1 - fraction), bar.w, bar.h * fraction,
                        healthColour(fraction), unit * 0.75)
                end
            end
        end
    end
end
