# Roadmap

- Rework the trading post system
- New fifth society: the Ancient Order
  - [ ] Tier 1: Farms can be built on hills; mines can be built on flatland; Builders +1 movement
  - [ ] Tier 2: Farms +0.5 housing; flatland mines +1 hammer
  - [ ] Tier 3: All cities +2 population and +2 happiness
  - [ ] Tier 4: Buy a nerfed GDR in the capital with 5 population and 1000 gold

| Attribute | Nerfed GDR | Normal GDR |
|:-:|:-:|:-:|
| Strength | 6190 | 6190 |
| Movement | 2 | 5 |
| Maintenance | 50 | 15 |
| Strategic resource | none | Uranium |

- New function menu in the policy panel
  - [ ] Show a beginner's guide menu
  - [ ] Show the municipality tree menu
  - [ ] Remind about unused governor points
  - [ ] Spend gold/faith to quickly overturn policies
  - [ ] Toggle menu: whether the above menus pop up automatically each turn

- New war-weariness system

  Let $x = \text{floor}((\text{happiness} \times 100 - \text{war-weariness}) / \text{cities} / 20)$

  Let the current era number be $y$ (Ancient = 1, Classical = 2)

  Let $z = \text{floor}(\text{your army size} / (\text{global average major-civ army size} + 1) \times 100)$

  War-weariness sources:
  - Killing enemy units: $+(20 + 10y)$
  - Losing your units: $+\text{floor}((60 + 20y) / (1 + z/100))$
  - Conquering cities: $+(100 + 30y)$
  - Losing cities: $+(150 + 40y)$
  - Decay per turn: $-30$
  - Authoritarian government extra decay: $-20$ (total $-50$/turn)
  - Some policy cards and civ abilities reduce war-weariness gain by a percentage

  Nationwide $+x\%$ science/culture/hammers/gold/faith, uncapped.
