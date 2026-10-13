cold.script {
    name = "Clicker",
    version = "1.0",
    author = "Island Community",
    description = "Clicks while the button is held",
}

local clicker

clicker = cold.module {
    name = "ScriptClicker",
    description = "Extra clicks while the attack button is held",
    category = "combat",
    subcategory = "basic",
    icon = "activity",
    settings = {
        { name = "Min CPS", type = "number", value = 8, min = 1, max = 20, decimals = 0 },
        { name = "Max CPS", type = "number", value = 12, min = 1, max = 20, decimals = 0 },
        { name = "Charged only", type = "boolean", value = true,
          description = "Wait for the attack cooldown before each click" },
    },
}

local timer = cold.timer()
local delay = 0.1

local function nextDelay()
    local settings = clicker.settings
    local low = math.min(settings["Min CPS"], settings["Max CPS"])
    local high = math.max(settings["Min CPS"], settings["Max CPS"])

    return 1.0 / (low + math.random() * (high - low))
end

function clicker.onEnable()
    timer:reset()
    delay = nextDelay()
end

function clicker.suffix()
    return string.format("%.0f-%.0f", clicker.settings["Min CPS"], clicker.settings["Max CPS"])
end

function clicker.onTick()
    if not cold.player.valid or cold.client.screenOpen then return end
    if not cold.input.attackHeld() then return end
    if clicker.settings["Charged only"] and cold.player.cooldown < 0.99 then return end

    if timer:passed(delay) then
        timer:reset()
        delay = nextDelay()
        cold.action.click(false)
    end
end
