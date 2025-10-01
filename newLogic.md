# 🌨 Unified SS+ Snow Logic

## 1. Global System
- **`globalSnowHistory : Map<Integer, SnowRecord>`**  
  Each storm inserts a `SnowRecord` with:
  - `minLayers` → minimum layers this storm can generate  
  - `avgLayers` → target average layers  
  - `maxLayers` → maximum layers possible  
  - `pattern` → optional per-position distribution (random guide)  

- **`currentStormId : int`**  
  Tracks the ongoing storm.  
  This is **not counted** when chunks check for piling.  

- **History updates** via `RainHandler`:  
  - When rain starts → mark `currentStormId`.  
  - When rain ends → push a new `SnowRecord` into `globalSnowHistory`.  

---

## 2. Chunk System
Each chunk keeps only:
- **`snowColumns : Map<BlockPos, Integer>`** → number of snow layers at each tracked surface.  

Chunks do **not track storm history**.  
Instead, they compare their current state (`snowColumns`) with `globalSnowHistory`.  

---

## 3. Snow Piling Logic
### Conditions
- Must be **`coldEnoughForSnow`**.  
- Chunk must be **behind global history** (ignores `currentStormId`).  

### Algorithm
1. **Compute chunk’s current average:**
```

totalLayers = sum(snowColumns.values())
totalPositions = snowColumns.size()
currentAvg = (totalPositions > 0) ? totalLayers / totalPositions : 0

```

2. **Compute target average from SnowRecord:**
```

normalization = (minLayers + (maxLayers - avgLayers) / 2) / 200
targetAvg = avgLayers / max(normalization, epsilon)

```

3. **Compare:**
- If `abs(currentAvg - targetAvg) > 1`  
  → `delta = round(targetAvg - currentAvg)`  
  → Increase all `snowColumns` entries by `delta`.  
  → Example: current=4, target=6 → every pos +2 layers.  

4. **If chunk has no snow:**
- Generate staged random distribution:
  - For each pos: random `r ∈ [1, max-min]`.  
  - Add `minLayers` baseline to all.  
  - Store results in `snowColumns`.  
- After staging → place all snow blocks at once.  

5. **Placement rule:**  
- If the block can’t hold snow → move upward until valid and place there.  

---

## 4. Snow Melting Logic
### Conditions
- **Not cold enough for snow (`!coldEnoughForSnow`)**  
- Season ∈ `[Late Spring → Early Winter]`  
- Current winter has **no storms yet** (`globalSnowHistory` empty for this winter).  

### Algorithm
1. Iterate `snowColumns`.  
2. For each tracked `BlockPos`:  
- Remove the topmost snow layer.  
- Check below: if another snow block, continue removing until solid ground.  
- Update `snowColumns`.  
3. If `snowColumns` is empty → no melt occurs.  

---

## 5. Realtime Vanilla Snow Tracking
- Vanilla snow placement inside `ServerLevel.tickChunk` is intercepted with a **Redirect on `setBlockAndUpdate`**.  
- When `Blocks.SNOW` is placed or updated:  
- Insert/Update in `snowColumns`.  
- When snow melts naturally (block update → snow removed):  
- Remove from `snowColumns`.  

This way all snow blocks in a chunk are always known without rescanning the heightmap.  

---

## 6. Loaded vs Unloaded Chunks
- **Unloaded:**  
- On load (chunk generation or reload), instantly bring `snowColumns` in sync with `globalSnowHistory` (apply piling/melting if needed).  

- **Loaded:**  
- Use the existing enqueue system to periodically check if `snowColumns` is consistent with `globalSnowHistory`.  
- If behind → apply piling.  
- If invalid season and snow present → apply melting.  

---

## 7. Special Rules
- **Ignore current storm:**  
When piling, never count the `currentStormId`. Only finished storms are considered.  

- **Consistency Guarantee:**  
- Every chunk eventually converges to the same snow distribution.  
- Piling ensures averages are correct.  
- Melting ensures snow disappears only in valid seasonal windows.  

---
