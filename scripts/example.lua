cold.script {
    name = "Example",
    version = "1.0",
    author = "Island Community",
    description = "A tour of the script API",
}

local demo

demo = cold.module {
    name = "ScriptDemo",
    description = "Reads the API and draws what it finds",
    category = "client",
    icon = "braces",
    bind = 0,
    settings = {
        { name = "Mode", type = "choice", choices = { "Text", "Bars" }, value = "Text",
          description = "What to draw in the corner" },
        { name = "Size", type = "number", value = 9, min = 6, max = 20, decimals = 1 },
        { name = "Shadow", type = "boolean", value = true },
        { name = "Colour", type = "color", value = cold.rgb(0x8FD3FF) },
        { name = "Bar count", type = "number", value = 4, min = 1, max = 12, decimals = 0,
          visible = function() return demo.settings.Mode == "Bars" end },
    },
}

local function opts()
    return demo.settings
end

function demo.onEnable()
    cold.notify("ScriptDemo on", 2, "good")
end

function demo.onDisable()
    cold.notify("ScriptDemo off", 2)
end

function demo.suffix()
    return opts().Mode
end

function demo.onTick()
    if not cold.player.valid then return end
    demo.speed = cold.player.speed * 20
end

function demo.onRender()
    local settings = opts()
    local unit = cold.render.scale()
    local x, y = 4 * unit, 90 * unit
    local size = settings.Size * unit
    local colour = settings.Colour

    if settings.Mode == "Text" then
        local lines = {
            string.format("%d fps, %d ms", cold.client.fps, cold.client.ping),
            string.format("%d entities", cold.entities.count()),
            string.format("%.1f blocks/s", demo.speed or 0),
        }
        for index, line in ipairs(lines) do
            cold.render.text(line, x, y + (index - 1) * size * 1.4, size, colour,
                { font = "medium", shadow = settings.Shadow, cap = true })
        end
        return
    end

    local bars = math.floor(settings["Bar count"])
    local width = 3 * unit
    for index = 1, bars do
        local fraction = 0.35 + 0.65 * math.abs(math.sin(cold.client.time * 2 + index))
        local height = 18 * unit * fraction
        cold.render.rect(x + (index - 1) * (width + unit), y + 18 * unit - height, width, height,
            colour:fade(0.35 + 0.65 * fraction), unit)
    end
end

cold.on("worldload", function()
    cold.notify("world loaded", 3, "good")
end)

cold.on("key", function(key, down)
    if down and key == cold.keys.F9 then
        demo.enabled = not demo.enabled
    end
end)

local runs = math.floor(cold.config.get("runs", 0)) + 1
cold.config.set("runs", runs)
cold.config.save()
cold.log(string.format("loaded, run number %d", runs))
