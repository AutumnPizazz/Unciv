# Military Strength Calculation

> Author: SpringPizazz
>
> Date: 2026-02-11
>
> Game version: 4.19.12

## 1. Base combat strength of each unit

Every unit has a **base combat strength**, determined mainly by:

### 1.1 Base attack

- **Melee units**: `strength ^ 1.5`
- **Ranged units**: `ranged strength ^ 1.45`

#### Examples

| Unit | Strength | Base combat strength |
| ------ | ---------- | ------------------------ |
| Warrior | 6 | $6^{1.5} \approx 14.7$ |
| Archer | 7 (ranged) | $7^{1.45} \approx 14.9$ |
| Catapult | 12 (ranged) | $12^{1.45} \approx 37.9$ |

### 1.2 Movement bonus

Strength is also multiplied by:
$$
\text{Movement}^{0.3}
$$

#### Examples

- **Warrior** (movement 2): $14.7 \times 2^{0.3} \approx 18$
- **Archer** (movement 2): $14.9 \times 2^{0.3} \approx 18$
- **Horseman** (strength 10, movement 4): $10^{1.5} \times 4^{0.3} \approx 31.6 \times 1.52 \approx 44$

### 1.3 Bonuses and modifiers

| Factor | Effect |
| ------ | ------ |
| Ranged naval units | strength **× 0.5** |
| Self-destructing units (e.g. missiles) | strength **× 0.5** |
| City attack bonus (e.g. +50%) | half counts → **+25%** |
| Bonus vs specific units | only **a quarter** counts |
| "When attacking"/"when defending" bonus | only **half** counts |
| Terrain bonus | only **half** counts |
| Paradrop ability | **+25%** |
| Needs setup to attack | **-20%** |
| Extra attacks | **+20%** per extra attack |
| Nukes | **+4000** |

#### Concrete example: Catapult

- Ranged attack 12, movement 2
- Has "needs setup" and "city attack +50%"

Calculation:

1. **Base**: $12^{1.45} \times 2^{0.3} \approx 37.9 \times 1.23 \approx 46.5$
2. **Setup modifier**: $46.5 \div 1.2 = 38.75$
3. **City attack**: $38.75 \times 1.25 \approx 48.4$

## 2. Actual unit strength (promotions & HP)

Final strength is also affected by:

- **Promotions**: each promotion multiplies strength by $(\text{promotions} + 1)^{0.3}$
- **Current HP**: applied proportionally (e.g. 50HP = ×0.5)

### Examples

- **Full-HP Archer** (no promotions): $18 \times 1.0 \times 100\% = 18$
- **Half-HP Archer** (2 promotions): $18 \times (3^{0.3}) \times 50\% \approx 18 \times 1.39 \times 0.5 \approx 12.5$

## 3. Total civilization military strength

Summing all unit strengths gives your **civilization military strength**!

### Formula

$$
\text{Military strength} = (1 + \sum \text{unit strengths}) \times \text{gold modifier}
$$

### Important details

1. **Base value is 1**: even with no units you have 1 military strength
2. **Naval units × 0.5**: water units count for half strength
3. **Gold modifier**: more gold = higher military strength

### Gold modifier table

| Gold | Modifier |
| ------ | ------ |
| 0 | 0% (×1.0) |
| 100 | 10% (×1.1) |
| 1,000 | 31.6% (×1.316) |
| 2,500 | 50% (×1.5) |
| 6,400 | 80% (×1.8) |
| 10,000 | 100% (×2.0) |
| 20,000 | 141% (but **capped at ×2.0**) |

> 💡 **The gold modifier caps at 2x (reached at 10,000 gold)**

## 4. Full calculation example

Suppose your civilization has these units (all **full-HP, no promotions**):

| Unit | Count | Unit strength | Subtotal |
| ------ | ---- | ------------ | ---- |
| Warrior | 3 | 18 | 54 |
| Archer | 2 | 18 | 36 |
| Catapult | 1 | 48 | 48 |
| Trireme | 1 | 50 (naval ×0.5) | 25 |
| Horseman | 1 | 44 | 44 |

- **Unit total**: $1 + 54 + 36 + 48 + 25 + 44 = 208$

- With **2,500 gold**:
  - Gold modifier = $\sqrt{2500}\% = 50\%$ (×1.5)
  - **Final military strength** = $208 \times 1.5 = 312$

## 5. Practical tips

1. **Ranged units**: despite high attack, the 1.45 exponent (not 1.5) makes their actual strength slightly lower than equal-attack melee units
2. **Naval units**: automatically halved — you need more ships at sea to deter
3. **HP matters**: damaged units lose lots of strength; heal promptly
4. **Gold is invisible strength**: 2,500 gold gives +50% military strength!
5. **Promotions accumulate**: veteran units beat fresh ones

## 6. Code references

| Feature | Code location |
| ------ | ------ |
| Base unit strength | `core/src/com/unciv/models/ruleset/unit/BaseUnit.kt:509` |
| Unit strength | `core/src/com/unciv/logic/map/mapunit/MapUnit.kt:685` |
| Civ military strength | `core/src/com/unciv/logic/civilization/Civilization.kt:800` |
| Detailed docs | `docs/Other/Force-rating-calculation.md` |

> ✦ **Summary**: stronger units, more units, more gold → higher military strength!
