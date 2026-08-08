-- testMOD Lua 示例
function hello(ctx)
    local civ = ctx.civ

    ctx.log("======== Lua testMOD ========")
    ctx.log("Civ: " .. civ.name)
    ctx.log("Gold: " .. tostring(civ.gold))
    ctx.log("Cities: " .. tostring(civ.cityCount))
    ctx.log("Era: " .. civ.era)
    ctx.log("==============================")

    -- 弹出游戏内通知
    civ.addNotification("Lua testMOD says hello! Gold=" .. tostring(civ.gold))

    -- 赠送 100 科技
    civ.addStat("Gold", 100)

    ctx.log("Done! 100 Gold gifted.")
    return true
end
