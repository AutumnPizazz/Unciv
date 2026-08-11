-- LuaStarterMod main entry points.
-- Remember: the engine passes exactly ONE argument - the ctx table.
-- The unique's [parameter] arrives as a string in ctx.parameter.

-- Fired every turn start (GlobalUniques.json, <upon turn start>).
-- Demonstrates: ctx.parameter (Countable-resolved string), ctx.civ, ctx.log, notifications.
function onTurnStart(ctx)
    local amount = tonumber(ctx.parameter) or 0
    local civ = ctx.civ

    civ.addGold(amount)
    civ.addNotification("LuaStarterMod: +" .. tostring(amount) .. " Gold this turn")
    ctx.log("onTurnStart: gave " .. tostring(amount) .. " gold to " .. civ.name)

    -- Example guard: only act when the civ has a capital
    local capital = civ.getCapital()
    if capital ~= nil then
        ctx.log("onTurnStart: capital is " .. capital.name)
    end
    return true
end

-- Fired every turn end (<upon turn end>). Put cleanup / per-turn-end logic here.
function onTurnEnd(ctx)
    -- ctx.log("onTurnEnd for " .. ctx.civ.name)
    return true
end
