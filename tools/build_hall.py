# Builds the Cinema4D auditorium as hall.glb + seat_map.json.
# Run:  /Applications/Blender.app/Contents/MacOS/Blender --background --python build_hall.py -- <out_glb> <out_json>
import bpy
import json
import math
import os
import sys

argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
OUT_GLB = argv[0] if len(argv) > 0 else "/tmp/cinema_build/hall.glb"
OUT_JSON = argv[1] if len(argv) > 1 else "/tmp/cinema_build/seat_map.json"

# ---------------------------------------------------------------- clean scene
bpy.ops.wm.read_factory_settings(use_empty=True)
scene = bpy.context.scene
scene.unit_settings.system = "METRIC"
scene.unit_settings.scale_length = 1.0

# ---------------------------------------------------------------- materials
def make_mat(name, base, rough=0.9, metal=0.0, emit=None, estr=0.0, spec=0.4, sheen=0.0):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    bsdf = m.node_tree.nodes.get("Principled BSDF")

    def s(key, val):
        if key in bsdf.inputs:
            try:
                bsdf.inputs[key].default_value = val
            except Exception:
                pass

    s("Base Color", (*base, 1.0))
    s("Roughness", rough)
    s("Metallic", metal)
    if "Specular IOR Level" in bsdf.inputs:
        s("Specular IOR Level", spec)
    if sheen > 0.0:
        s("Sheen Weight", sheen)
        s("Sheen Roughness", 0.35)
        if "Sheen Tint" in bsdf.inputs:
            s("Sheen Tint", (1.0, 0.55, 0.55, 1.0))
    if emit is not None:
        s("Emission Color", (*emit, 1.0))
        s("Emission Strength", estr)
    try:
        m.use_backface_culling = False  # -> doubleSided in glTF
    except Exception:
        pass
    return m

MAT = {
    "wall":   make_mat("WallPaint",  (0.022, 0.016, 0.014), 0.95),
    "trim":   make_mat("WallTrim",   (0.040, 0.030, 0.026), 0.9),
    "carpet": make_mat("Carpet",     (0.020, 0.020, 0.026), 1.0),
    "aisle":  make_mat("CarpetAisle",(0.048, 0.046, 0.052), 1.0),
    "fabric": make_mat("SeatFabric", (0.220, 0.020, 0.032), 0.58,
                       emit=(0.018, 0.001, 0.002), estr=1.0, spec=0.05, sheen=0.0),
    "plastic":make_mat("SeatPlastic",(0.020, 0.020, 0.026), 0.35, 0.15, spec=0.35),
    "shell":  make_mat("SeatShell",  (0.008, 0.008, 0.010), 0.22, 0.0, spec=0.5),
    "frame":  make_mat("ScreenFrame",(0.008, 0.008, 0.010), 0.7),
    "screen": make_mat("ScreenMat",  (0.010, 0.010, 0.012), 0.6),
    "warm":   make_mat("GlowWarm",   (0.10, 0.08, 0.05), 0.6, emit=(1.0, 0.58, 0.22), estr=1.0),
    "red":    make_mat("GlowRed",    (0.10, 0.02, 0.02), 0.6, emit=(1.0, 0.08, 0.05), estr=4.0),
}

# ---------------------------------------------------------------- mesh builder
class MB:
    """Accumulates transformed boxes -> one mesh with material slots."""
    def __init__(self):
        self.verts = []
        self.faces = []
        self.mats = []

    def add_box(self, cx, cy, cz, sx, sy, sz, mat, rot_x=0.0):
        x, y, z = sx / 2, sy / 2, sz / 2
        local = [(-x, -y, -z), (x, -y, -z), (x, y, -z), (-x, y, -z),
                 (-x, -y, z),  (x, -y, z),  (x, y, z),  (-x, y, z)]
        c, sn = math.cos(rot_x), math.sin(rot_x)
        base = len(self.verts)
        for (lx, ly, lz) in local:
            # rotate about X (tilt), then translate
            ry = ly * c - lz * sn
            rz = ly * sn + lz * c
            self.verts.append((cx + lx, cy + ry, cz + rz))
        quads = [(0, 3, 2, 1), (4, 5, 6, 7), (0, 1, 5, 4),
                 (2, 3, 7, 6), (0, 4, 7, 3), (1, 2, 6, 5)]
        for q in quads:
            self.faces.append(tuple(base + i for i in q))
            self.mats.append(mat)

    def build(self, name, materials):
        me = bpy.data.meshes.new(name)
        me.from_pydata(self.verts, [], self.faces)
        me.update()
        for m in materials:
            me.materials.append(m)
        for i, poly in enumerate(me.polygons):
            poly.material_index = self.mats[i]
        obj = bpy.data.objects.new(name, me)
        scene.collection.objects.link(obj)
        return obj

# material name -> index within each MB (each MB lists its own materials)
def add_seat(mb_seats, mb_hw, x, y, z_floor, mat_fabric_idx, recliner=False):
    """One upholstered seat, local origin at floor under seat centre, facing -Y.
    Fabric pads go into mb_seats (beveled later for a plush look); frame,
    shell and armrests go into mb_hw and stay crisp."""
    # pedestal — slim glossy column
    mb_hw.add_box(x, y + 0.04, z_floor + 0.15, 0.34, 0.32, 0.30, 0)
    # glossy outer tub behind the cushions
    mb_hw.add_box(x, y + 0.17, z_floor + 0.54, 0.60, 0.18, 0.54, 1)
    # cushion — plump pad
    mb_seats.add_box(x, y - 0.05, z_floor + 0.44, 0.56, 0.52, 0.17, mat_fabric_idx)
    # backrest pad — leans back
    mb_seats.add_box(x, y + 0.21, z_floor + 0.83, 0.56, 0.17, 0.58, mat_fabric_idx, rot_x=-0.17)
    # headrest pad
    mb_seats.add_box(x, y + 0.27, z_floor + 1.18, 0.44, 0.15, 0.24, mat_fabric_idx, rot_x=-0.17)
    # armrests — rounded bars
    mb_hw.add_box(x - 0.325, y - 0.01, z_floor + 0.62, 0.09, 0.44, 0.08, 0)
    mb_hw.add_box(x + 0.325, y - 0.01, z_floor + 0.62, 0.09, 0.44, 0.08, 0)
    if recliner:
        # extended footrest
        mb_hw.add_box(x, y - 0.34, z_floor + 0.17, 0.48, 0.36, 0.09, 0)

# ---------------------------------------------------------------- hall layout
# Sloped auditorium slab: top surface z = SLOPE_Z0 + (y - SLOPE_Y0) * SLOPE_M
SLOPE_Y0, SLOPE_Z0, SLOPE_M = -4.8, 0.42, 0.298
SLAB_Y0, SLAB_Y1 = -4.8, 8.4
SCREEN_Y, SCREEN_Z, SCREEN_W, SCREEN_H = -9.92, 3.45, 10.6, 5.0
FRAME_Y = -9.965
WALL_X, WALL_YF, WALL_YB = 7.0, -10.0, 10.0
CEIL_Z = 7.55

def floor_z(y):
    return SLOPE_Z0 + (y - SLOPE_Y0) * SLOPE_M

# rows: (letter, y, per_side, seat_w, gap, pitch_to_next, recliner)
rows_cfg = []
y = -3.2
for i in range(10):  # A..J standard
    rows_cfg.append((chr(65 + i), y, 7, 0.56, 0.07, 0.98, False))
    y += 0.98
for i in range(2):    # K,L recliners
    rows_cfg.append((chr(75 + i), y, 6, 0.66, 0.11, 1.14, True))
    y += 1.14

AISLE_HALF = 0.55

# ---------------------------------------------------------------- build meshes
mb_floor = MB()    # mats: carpet
mb_slab  = MB()    # mats: carpet
mb_walls = MB()    # mats: wall, trim
mb_glow  = MB()    # mats: warm, red
mb_frame = MB()    # mats: frame, screen
mb_seats = MB()    # mats: fabric, plastic
mb_carpet= MB()    # mats: aisle

mi = {k: i for i, k in enumerate(["carpet"])}
# --- flat floor (whole hall, top z=0) + ceiling
mb_floor.add_box(0, 0, -0.06, 14.4, 20.6, 0.12, 0)
mb_walls.add_box(0, 0, CEIL_Z + 0.06, 14.4, 20.6, 0.12, 0)  # ceiling = wall paint
# --- sloped slab (carpet top)
slab_len = math.hypot(SLAB_Y1 - SLAB_Y0, (floor_z(SLAB_Y1) - floor_z(SLAB_Y0))) + 0.3
slab_cy = (SLAB_Y0 + SLAB_Y1) / 2
slab_cz = (floor_z(SLAB_Y0) + floor_z(SLAB_Y1)) / 2 - 0.075
slab_rot = math.atan(SLOPE_M)
mb_slab.add_box(0, slab_cy, slab_cz, 14.4, slab_len, 0.15, 0, rot_x=slab_rot)
# front lip where slab meets flat floor (small ramp look) - skip, overlap ok

# --- walls (wall paint) ---
mb_walls.add_box(0, WALL_YF - 0.1, CEIL_Z / 2, 14.4, 0.2, CEIL_Z, 0)          # front
mb_walls.add_box(0, WALL_YB + 0.1, CEIL_Z / 2, 14.4, 0.2, CEIL_Z, 0)          # back
mb_walls.add_box(-WALL_X - 0.1, 0, CEIL_Z / 2, 0.2, 20.6, CEIL_Z, 0)          # left
mb_walls.add_box(WALL_X + 0.1, 0, CEIL_Z / 2, 0.2, 20.6, CEIL_Z, 0)           # right

# --- wall trim ribs (trim) ---
for sy in (-1, 1):
    wx = sy * (WALL_X - 0.02)
    for yy in [(-8 + 2.1 * i) for i in range(9)]:
        mb_walls.add_box(wx, yy, 3.6, 0.05, 0.16, 6.4, 1)
    mb_walls.add_box(wx, 0, 6.9, 0.05, 19.5, 0.14, 1)   # top rail
for xx in range(-6, 7, 2):
    mb_walls.add_box(xx, WALL_YB - 0.02, 3.6, 0.16, 0.05, 6.4, 1)
mb_walls.add_box(0, WALL_YB - 0.02, 6.9, 13.8, 0.05, 0.14, 1)

# --- screen + frame ---
mb_frame.add_box(0, FRAME_Y, SCREEN_Z, SCREEN_W + 0.6, 0.07, SCREEN_H + 0.4, 0)  # frame
mb_frame.add_box(0, SCREEN_Y, SCREEN_Z, SCREEN_W, 0.10, SCREEN_H, 1)             # screen face

# --- aisle carpet strips on slab (aisle mat) ---
for ax in (-0.62, 0.62):
    mb_carpet.add_box(ax, slab_cy, slab_cz + 0.005, 0.09, slab_len - 0.2, 0.02, 0, rot_x=slab_rot)

# --- glow bits: step-light strips along centre aisle + exit signs + wall frames ---
for ax in (-0.60, 0.60):
    mb_glow.add_box(ax, slab_cy, slab_cz + 0.012, 0.035, slab_len - 0.4, 0.025, 0, rot_x=slab_rot)
# exit signs above side doors
for sy in (-1, 1):
    mb_glow.add_box(sy * (WALL_X - 0.06), -2.0, 2.6, 0.08, 0.55, 0.28, 1)
    mb_glow.add_box(sy * (WALL_X - 0.06),  4.0, 2.6, 0.08, 0.55, 0.28, 1)
# glowing "poster" panels on side walls (soft warm rectangles)
for sy in (-1, 1):
    for py in (-3.0, 0.6, 4.2):
        mb_glow.add_box(sy * (WALL_X - 0.05), py, 3.1, 0.04, 1.15, 1.7, 0)
# vertical wall sconces between the panels
for sy in (-1, 1):
    for py in (-6.2, -1.2, 2.4, 6.4):
        mb_glow.add_box(sy * (WALL_X - 0.04), py, 3.6, 0.03, 0.09, 1.5, 0)
# ceiling downlight panels — visible fixtures above the two light rows
for cx in (-3.6, 3.6):
    for cy in (-6.5, -2.2, 2.2, 6.5):
        mb_glow.add_box(cx, cy, CEIL_Z - 0.04, 0.45, 0.45, 0.03, 0)
# ceiling cove strips running the hall depth on both sides
for sy in (-1, 1):
    mb_glow.add_box(sy * 5.9, 0, CEIL_Z - 0.05, 0.10, 17.5, 0.03, 0)

# --- seats + seat map data ---
seats_json = []
mat_fabric_i = 0
mb_hw = MB()      # mats: plastic, shell
mat_plastic_i = 0
mat_shell_i = 1
for (letter, ry, per_side, sw, gap, _pitch, recliner) in rows_cfg:
    zf = floor_z(ry)
    pitch = sw + gap
    for side in (-1, 1):
        for j in range(per_side):
            # j=0 nearest centre aisle
            x = side * (AISLE_HALF + sw / 2 + j * pitch)
            # numbering: left block far->near = 1..per_side, right near->far = per_side+1..
            if side == -1:
                num = per_side - j
            else:
                num = per_side + 1 + j
            sid = f"{letter}{num}"
            add_seat(mb_seats, mb_hw, x, ry, zf, mat_fabric_i, recliner)
            ax = abs(x)
            tier = "recliner" if recliner else ("prime" if ax <= 2.45 else "classic")
            # glTF coords: blender (x,y,z) -> (x, z, -y)
            eye = [round(x, 4), round(zf + 1.20, 4), round(-(ry - 0.05), 4)]
            pos = [round(x, 4), round(zf + 0.45, 4), round(-ry, 4)]
            seats_json.append({
                "id": sid, "row": letter, "num": num,
                "x": round(x, 4), "mapY": round(ry, 4),
                "eye": eye, "pos": pos, "tier": tier,
            })

# sort seats row-major then by x for stable map ordering
row_order = {r[0]: i for i, r in enumerate(rows_cfg)}
seats_json.sort(key=lambda s: (row_order[s["row"]], s["x"]))

# ---------------------------------------------------------------- build objects
mb_floor.build("Floor", [MAT["carpet"]])
mb_slab.build("Slab",   [MAT["carpet"]])
mb_walls.build("Walls", [MAT["wall"], MAT["trim"]])
mb_glow.build("Glow",   [MAT["warm"], MAT["red"]])
mb_frame.build("Screen",[MAT["frame"], MAT["screen"]])
mb_carpet.build("AisleCarpet", [MAT["aisle"]])
mb_hw.build("SeatHardware", [MAT["plastic"], MAT["shell"]])
seats_obj = mb_seats.build("Seats", [MAT["fabric"]])
# bevel the fabric pads so cushions and backs read as soft upholstery
bev = seats_obj.modifiers.new("Bevel", 'BEVEL')
bev.width = 0.04
bev.segments = 3
bev.limit_method = 'ANGLE'
bev.angle_limit = math.radians(35)
bev.use_clamp_overlap = True
bpy.context.view_layer.objects.active = seats_obj
bpy.ops.object.modifier_apply(modifier=bev.name)

# dim sun for glTF punctual light
sun = bpy.data.lights.new("SunKey", "SUN")
sun.energy = 0.35
sun.color = (1.0, 0.9, 0.8)
sun.angle = math.radians(30)
sun_obj = bpy.data.objects.new("SunKey", sun)
scene.collection.objects.link(sun_obj)
sun_obj.rotation_euler = (math.radians(55), 0, math.radians(15))

world = bpy.data.worlds.new("World")
world.use_nodes = True
world.node_tree.nodes["Background"].inputs[0].default_value = (0.004, 0.005, 0.008, 1.0)
world.node_tree.nodes["Background"].inputs[1].default_value = 0.6
scene.world = world

# ---------------------------------------------------------------- export
os.makedirs(os.path.dirname(OUT_GLB), exist_ok=True)
bpy.ops.export_scene.gltf(
    filepath=OUT_GLB,
    export_format="GLB",
    export_apply=True,
    export_yup=True,
)

seat_map = {
    "version": 1,
    "units": "meters",
    "hall": {
        "width": 14.4, "depth": 20.6,
        "wallX": WALL_X, "frontY": WALL_YF, "backY": WALL_YB,
        "ceilZ": CEIL_Z,
        "screen": {"y": SCREEN_Y, "z": SCREEN_Z, "w": SCREEN_W, "h": SCREEN_H,
                   "center": [0.0, SCREEN_Z, -SCREEN_Y]},
        "slope": {"y0": SLOPE_Y0, "z0": SLOPE_Z0, "m": SLOPE_M},
    },
    "rows": [{"letter": r[0], "y": round(r[1], 4), "floorZ": round(floor_z(r[1]), 4),
              "perSide": r[2], "recliner": r[6]} for r in rows_cfg],
    "aisleHalf": AISLE_HALF,
    "seats": seats_json,
}
with open(OUT_JSON, "w") as f:
    json.dump(seat_map, f, separators=(",", ":"))

print(f"[build_hall] seats={len(seats_json)} rows={len(rows_cfg)}")
print(f"[build_hall] glb={OUT_GLB} json={OUT_JSON}")
for r in rows_cfg:
    print(f"  row {r[0]}: y={r[1]:.2f} floorZ={floor_z(r[1]):.2f} seats={2*r[2]}")
