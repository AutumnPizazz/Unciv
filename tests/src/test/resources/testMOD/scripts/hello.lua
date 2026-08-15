-- testMOD Lua 示例
function hello(ctx)
    local civ = ctx.civ

    ctx.log("======== Lua testMOD ========")
    ctx.log("Civ: " .. civ.name)
    ctx.log("Gold: " .. tostring(civ.getGold()))
    ctx.log("Cities: " .. tostring(civ.getCityCount()))
    ctx.log("Era: " .. civ.getEra())
    ctx.log("==============================")

    -- 弹出游戏内通知
    civ.addNotification("Lua testMOD says hello! Gold=" .. tostring(civ.getGold()))

    -- 赠送 100 金币
    civ.addGold(100)

    ctx.log("Done! 100 Gold gifted.")
    return true
end
