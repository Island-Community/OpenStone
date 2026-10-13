local vec3 = ...
local methods = {}
vec3.__index = methods
vec3.__name = "vec3"

local function newVec(x, y, z)
    return setmetatable({ x = x or 0, y = y or 0, z = z or 0 }, vec3)
end

function cold.vec3(x, y, z)
    if type(x) == "table" then return newVec(x.x, x.y, x.z) end
    return newVec(x, y, z)
end

function methods:copy() return newVec(self.x, self.y, self.z) end
function methods:unpack() return self.x, self.y, self.z end
function methods:lengthSquared() return self.x * self.x + self.y * self.y + self.z * self.z end
function methods:length() return math.sqrt(self:lengthSquared()) end
function methods:horizontal() return math.sqrt(self.x * self.x + self.z * self.z) end
function methods:dot(o) return self.x * o.x + self.y * o.y + self.z * o.z end
function methods:offset(dx, dy, dz) return newVec(self.x + (dx or 0), self.y + (dy or 0), self.z + (dz or 0)) end
function methods:floor() return newVec(math.floor(self.x), math.floor(self.y), math.floor(self.z)) end
function methods:distance(o)
    local dx, dy, dz = self.x - o.x, self.y - o.y, self.z - o.z
    return math.sqrt(dx * dx + dy * dy + dz * dz)
end
function methods:normalised()
    local length = self:length()
    if length <= 0 then return newVec(0, 0, 0) end
    return newVec(self.x / length, self.y / length, self.z / length)
end
methods.normalized = methods.normalised

vec3.__add = function(a, b) return newVec(a.x + b.x, a.y + b.y, a.z + b.z) end
vec3.__sub = function(a, b) return newVec(a.x - b.x, a.y - b.y, a.z - b.z) end
vec3.__unm = function(a) return newVec(-a.x, -a.y, -a.z) end
vec3.__eq = function(a, b) return a.x == b.x and a.y == b.y and a.z == b.z end
vec3.__mul = function(a, b)
    if type(a) == "number" then return newVec(b.x * a, b.y * a, b.z * a) end
    if type(b) == "number" then return newVec(a.x * b, a.y * b, a.z * b) end
    return newVec(a.x * b.x, a.y * b.y, a.z * b.z)
end
vec3.__tostring = function(v)
    return string.format("vec3(%.2f, %.2f, %.2f)", v.x, v.y, v.z)
end

local Timer = {}
Timer.__index = Timer
Timer.__name = "timer"

function cold.timer()
    return setmetatable({ mark = cold.client.time }, Timer)
end

function Timer:reset() self.mark = cold.client.time end
function Timer:elapsed() return cold.client.time - self.mark end
function Timer:millis() return (cold.client.time - self.mark) * 1000 end
function Timer:passed(seconds) return (cold.client.time - self.mark) >= seconds end

function Timer:finished(millis) return (cold.client.time - self.mark) * 1000 >= millis end
function Timer:tryReset(seconds)
    if self:passed(seconds) then
        self:reset()
        return true
    end
    return false
end

local util = {}
cold.util = util

function util.clamp(value, low, high)
    if value < low then return low end
    if value > high then return high end
    return value
end

function util.lerp(a, b, t) return a + (b - a) * t end

function util.round(value, places)
    local factor = 10 ^ (places or 0)
    return math.floor(value * factor + 0.5) / factor
end

function util.sign(value)
    if value > 0 then return 1 elseif value < 0 then return -1 end
    return 0
end

function util.map(value, fromLow, fromHigh, toLow, toHigh)
    if fromHigh == fromLow then return toLow end
    return toLow + (value - fromLow) / (fromHigh - fromLow) * (toHigh - toLow)
end

function util.wrapDegrees(degrees)
    degrees = degrees % 360
    if degrees >= 180 then degrees = degrees - 360 end
    if degrees < -180 then degrees = degrees + 360 end
    return degrees
end

function util.angleDiff(from, to) return util.wrapDegrees(to - from) end

function util.approach(current, target, rate, delta)
    return current + (target - current) * (1 - math.exp(-rate * delta))
end

math.clamp = math.clamp or util.clamp
math.round = math.round or util.round
math.sign = math.sign or util.sign

cold.ease = {
    linear = function(t) return t end,
    inQuad = function(t) return t * t end,
    outQuad = function(t) return 1 - (1 - t) * (1 - t) end,
    outCubic = function(t) local i = 1 - t return 1 - i * i * i end,
    outQuint = function(t) local i = 1 - t return 1 - i * i * i * i * i end,
    inOutQuint = function(t)
        if t < 0.5 then return 16 * t * t * t * t * t end
        local i = -2 * t + 2
        return 1 - i * i * i * i * i / 2
    end,
    outExpo = function(t) if t >= 1 then return 1 end return 1 - 2 ^ (-10 * t) end,
    outBack = function(t, overshoot)
        overshoot = overshoot or 1.70158
        local i = t - 1
        return 1 + (overshoot + 1) * i * i * i + overshoot * i * i
    end,
}

function string.split(text, separator)
    local parts = {}
    for piece in string.gmatch(text, "([^" .. (separator or " ") .. "]+)") do
        parts[#parts + 1] = piece
    end
    return parts
end

function string.trim(text) return (text:gsub("^%s+", ""):gsub("%s+$", "")) end
function string.startsWith(text, prefix) return text:sub(1, #prefix) == prefix end
function string.endsWith(text, suffix) return suffix == "" or text:sub(-#suffix) == suffix end

function table.copy(source)
    local out = {}
    for key, value in pairs(source) do out[key] = value end
    return out
end

function table.count(source)
    local count = 0
    for _ in pairs(source) do count = count + 1 end
    return count
end

function table.contains(source, wanted)
    for _, value in pairs(source) do
        if value == wanted then return true end
    end
    return false
end

function table.map(source, fn)
    local out = {}
    for index, value in ipairs(source) do out[index] = fn(value, index) end
    return out
end

function table.filter(source, fn)
    local out = {}
    for _, value in ipairs(source) do
        if fn(value) then out[#out + 1] = value end
    end
    return out
end

function table.keys(source)
    local out = {}
    for key in pairs(source) do out[#out + 1] = key end
    return out
end

function cold.render.entityBox(entity)
    local low, high = entity:box()
    return cold.render.projectBox(low, high)
end

function cold.render.shadowedText(text, x, y, size, colour, options)
    options = options and table.copy(options) or {}
    options.shadow = true
    return cold.render.text(text, x, y, size, colour, options)
end

function cold.render3d.entityBox(entity, colour, thickness)
    local low, high = entity:box()
    cold.render3d.box(low, high, colour, thickness)
end

function cold.render3d.entityTracer(entity, colour, thickness)
    local low, high = entity:box()
    cold.render3d.tracer({ x = (low.x + high.x) * 0.5, y = high.y, z = (low.z + high.z) * 0.5 },
        colour, thickness)
end

local json = {}
cold.json = json

local escapes = { ['"'] = '\\"', ['\\'] = '\\\\', ['\b'] = '\\b', ['\f'] = '\\f',
                  ['\n'] = '\\n', ['\r'] = '\\r', ['\t'] = '\\t' }

local function encodeString(value)
    return '"' .. value:gsub('[%c"\\]', function(character)
        return escapes[character] or string.format('\\u%04x', character:byte())
    end) .. '"'
end

local function isArray(value)
    local count = 0
    for key in pairs(value) do
        if type(key) ~= "number" then return false end
        count = count + 1
    end
    return count == #value
end

function json.encode(value)
    local kind = type(value)
    if value == nil then return "null" end
    if kind == "boolean" then return tostring(value) end
    if kind == "number" then
        if value ~= value or value == math.huge or value == -math.huge then return "null" end
        return string.format("%.14g", value)
    end
    if kind == "string" then return encodeString(value) end
    if kind ~= "table" then return "null" end

    local parts = {}
    if isArray(value) then
        for _, entry in ipairs(value) do parts[#parts + 1] = json.encode(entry) end
        return "[" .. table.concat(parts, ",") .. "]"
    end
    for key, entry in pairs(value) do
        if type(key) == "string" or type(key) == "number" then
            parts[#parts + 1] = encodeString(tostring(key)) .. ":" .. json.encode(entry)
        end
    end
    return "{" .. table.concat(parts, ",") .. "}"
end

local decodeValue

local function skipSpace(text, position)
    local _, last = text:find("^[ \n\r\t]*", position)
    return last + 1
end

local function decodeString(text, position)
    local out = {}
    position = position + 1
    while position <= #text do
        local character = text:sub(position, position)
        if character == '"' then return table.concat(out), position + 1 end
        if character == "\\" then
            local escape = text:sub(position + 1, position + 1)
            if escape == "u" then
                out[#out + 1] = utf8.char(tonumber(text:sub(position + 2, position + 5), 16) or 63)
                position = position + 6
            else
                local simple = { b = "\b", f = "\f", n = "\n", r = "\r", t = "\t" }
                out[#out + 1] = simple[escape] or escape
                position = position + 2
            end
        else
            out[#out + 1] = character
            position = position + 1
        end
    end
    error("unterminated string in JSON")
end

decodeValue = function(text, position)
    position = skipSpace(text, position)
    local character = text:sub(position, position)
    if character == "" then error("unexpected end of JSON") end
    if character == '"' then return decodeString(text, position) end
    if character == "{" then
        local out = {}
        position = skipSpace(text, position + 1)
        if text:sub(position, position) == "}" then return out, position + 1 end
        while true do
            local key, value
            key, position = decodeString(text, skipSpace(text, position))
            position = skipSpace(text, position) + 1
            value, position = decodeValue(text, position)
            out[key] = value
            position = skipSpace(text, position)
            local separator = text:sub(position, position)
            position = position + 1
            if separator == "}" then return out, position end
            if separator ~= "," then error("expected , or } in JSON") end
        end
    end
    if character == "[" then
        local out = {}
        position = skipSpace(text, position + 1)
        if text:sub(position, position) == "]" then return out, position + 1 end
        while true do
            local value
            value, position = decodeValue(text, position)
            out[#out + 1] = value
            position = skipSpace(text, position)
            local separator = text:sub(position, position)
            position = position + 1
            if separator == "]" then return out, position end
            if separator ~= "," then error("expected , or ] in JSON") end
        end
    end
    if text:sub(position, position + 3) == "true" then return true, position + 4 end
    if text:sub(position, position + 4) == "false" then return false, position + 5 end
    if text:sub(position, position + 3) == "null" then return nil, position + 4 end

    local number = text:match("^-?%d+%.?%d*[eE]?[-+]?%d*", position)
    if number and #number > 0 then return tonumber(number), position + #number end
    error("unexpected character in JSON at " .. position)
end

function json.decode(text)
    if type(text) ~= "string" or text == "" then return nil end
    local ok, value = pcall(function()
        local result = decodeValue(text, 1)
        return result
    end)
    if not ok then return nil end
    return value
end

local Storage = {}
Storage.__index = Storage
Storage.__name = "storage"

function cold.storage(name)
    local key = "storage." .. (name or "default")
    return setmetatable({ key = key, data = json.decode(cold.config.get(key, "")) or {} }, Storage)
end

function Storage:get(key, default)
    local value = self.data[key]
    if value == nil then return default end
    return value
end

function Storage:set(key, value)
    self.data[key] = value
    return self
end

function Storage:remove(key) self.data[key] = nil end
function Storage:all() return self.data end
function Storage:clear() self.data = {} end

function Storage:save()
    cold.config.set(self.key, json.encode(self.data))
    cold.config.save()
    return self
end

cold.vec = {
    new = cold.vec3,
    add = function(a, b) return cold.vec3(a.x + b.x, a.y + b.y, a.z + b.z) end,
    sub = function(a, b) return cold.vec3(a.x - b.x, a.y - b.y, a.z - b.z) end,
    scale = function(a, s) return cold.vec3(a.x * s, a.y * s, a.z * s) end,
    dot = function(a, b) return a.x * b.x + a.y * b.y + a.z * b.z end,
    length = function(a) return math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z) end,
    distance = function(a, b)
        local dx, dy, dz = a.x - b.x, a.y - b.y, a.z - b.z
        return math.sqrt(dx * dx + dy * dy + dz * dz)
    end,
    normalize = function(a) return cold.vec3(a):normalised() end,
    lerp = function(a, b, t)
        return cold.vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t)
    end,
    clamp = function(value, low, high) return util.clamp(value, low, high) end,
}
cold.vec.normalise = cold.vec.normalize

cold.keys.code = function(name)
    if type(name) == "number" then return name end
    return cold.keys[string.upper(name)] or -1
end
cold.keys.name = function(code) return cold.keyName(code) end
cold.keys.down = function(key) return cold.input.keyHeld(cold.keys.code(key)) end
cold.keys.mouse = function(button) return cold.input.bindHeld(button or 0) end
cold.keys.cursor = function() return cold.input.cursor() end
cold.keys.x = function() local x = cold.input.cursor() return x end
cold.keys.y = function() local _, y = cold.input.cursor() return y end

local target = {}
cold.target = target

local function withoutFriends(list)
    local out = {}
    for _, entity in ipairs(list) do
        if not (entity.player and cold.friends.is(entity:name())) then
            out[#out + 1] = entity
        end
    end
    return out
end

function target.all(options)
    options = options or {}
    local list = cold.entities.all(options)
    if options.friends == false or options.friends == nil then
        return withoutFriends(list)
    end
    return list
end

function target.best(options)
    options = options or {}
    local best = cold.entities.target(options, options.sort)
    if best and options.friends ~= true and best.player and cold.friends.is(best:name()) then

        local closest, distance = nil, math.huge
        for _, entity in ipairs(target.all(options)) do
            if entity.distance < distance then closest, distance = entity, entity.distance end
        end
        return closest
    end
    return best
end

function target.valid(entity, options)
    options = options or {}
    if not entity or not entity.valid then return false end
    if entity.health and entity.health <= 0 and not options.dead then return false end
    if options.range and entity.distance > options.range then return false end
    if entity.player and options.players == false then return false end
    if not entity.player and options.mobs == false then return false end
    if entity.player and options.friends ~= true and cold.friends.is(entity:name()) then
        return false
    end
    return true
end

local esp = {}
cold.esp = esp

function esp.box(entity, colour, thickness)
    cold.render3d.entityBox(entity, colour or cold.accent(), thickness)
end

function esp.tracer(entity, colour, thickness)
    cold.render3d.entityTracer(entity, colour or cold.accent(), thickness)
end

function esp.nametag(entity, text, colour, size)
    local low, high = entity:box()
    cold.render3d.text({ x = (low.x + high.x) * 0.5, y = high.y + 0.35, z = (low.z + high.z) * 0.5 },
        text or entity:name(), size or (7.5 * cold.render.scale()), colour or cold.rgb(0xFFFFFF))
end

function esp.health(entity, colour)
    local x, y, width, height = cold.render.entityBox(entity)
    if not x then return end
    local unit = cold.render.scale()
    local fraction = entity.maxHealth > 0
        and math.min(1, (entity.health + entity.absorption) / entity.maxHealth) or 0
    cold.render.rect(x - unit * 2.5, y, unit * 1.5, height, cold.rgb(0x000000, 0.5), unit * 0.75)
    cold.render.rect(x - unit * 2.5, y + height * (1 - fraction), unit * 1.5, height * fraction,
        colour or cold.theme.health(entity.health, entity.maxHealth), unit * 0.75)
end

function esp.all(options, draw)
    for _, entity in ipairs(cold.entities.all(options or { range = 64 })) do
        draw(entity)
    end
end

local lang = setmetatable({ tables = {}, code = "en_us" }, {
    __call = function(self, key, ...)
        local table_ = self.tables[self.code] or self.tables["en_us"] or {}
        local value = table_[key] or key
        if select("#", ...) > 0 then return string.format(value, ...) end
        return value
    end,
})
cold.lang = lang

function lang.load(tables)
    for code, entries in pairs(tables) do
        lang.tables[code] = lang.tables[code] or {}
        for key, value in pairs(entries) do lang.tables[code][key] = value end
    end
end

function lang.add(code, entries) lang.load({ [code] = entries }) end
function lang.select(code) lang.code = code end
function lang.current() return lang.code end
function lang.has(key)
    local table_ = lang.tables[lang.code] or {}
    return table_[key] ~= nil
end
function lang.clear() lang.tables = {} end

function cold.round(value, places) return util.round(value, places) end

function cold.rotation.gcd(from, to)
    local step = cold.rotation.step()
    if step <= 0 then return to end
    return from + math.floor((to - from) / step + 0.5) * step
end

function cold.rotation.vanillaStep() return cold.rotation.step() end

function cold.mod(name) return cold.modules.get(name) end

function cold.entities.players(range)
    return cold.entities.all({ range = range or 64, mobs = false })
end

function cold.entities.mobs(range)
    return cold.entities.all({ range = range or 64, players = false })
end

function cold.entities.nearest(range)
    return cold.entities.target({ range = range or 64 }, "distance")
end
