"""Generate an Indian 3-tier sleeper coach (coach.glb) and its berth map.

Run:  Blender --background --python build_coach.py -- <out_glb> <out_json>

Layout: 6 bays, each with two facing banks of 6 berths (lower / middle / upper
across the aisle from each other) = 72 berths, matching a real SL coach.
Authored in Blender (Z up); glTF space is (x, z_blender, -y_blender).
"""
import bpy
import json
import math
import os
import sys

argv = sys.argv[sys.argv.index("--") + 1:]
OUT_GLB, OUT_JSON = argv[0], argv[1]

bpy.ops.wm.read_factory_settings(use_empty=True)
scene = bpy.context.scene


def make_mat(name, base, rough=0.9, metal=0.0, emit=None, estr=0.0, spec=0.4):
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
    if emit is not None:
        s("Emission Color", (*emit, 1.0))
        s("Emission Strength", estr)
    try:
        m.use_backface_culling = False
    except Exception:
        pass
    return m


MAT = {
    "floor":  make_mat("CoachFloor", (0.045, 0.05, 0.062), 1.0),
    "aisle":  make_mat("CoachAisle", (0.085, 0.09, 0.105), 1.0),
    "wall":   make_mat("CoachWall", (0.40, 0.385, 0.36), 0.88, emit=(0.20, 0.19, 0.18), estr=1.2),
    "trim":   make_mat("CoachTrim", (0.13, 0.135, 0.15), 0.6),
    "ceil":   make_mat("CoachCeiling", (0.55, 0.545, 0.53), 0.95, emit=(0.55, 0.54, 0.52), estr=0.55),
    "berth":  make_mat("BerthCushion", (0.055, 0.095, 0.20), 0.62,
                       emit=(0.008, 0.014, 0.030), estr=1.0, spec=0.18),
    "frame":  make_mat("BerthFrame", (0.10, 0.105, 0.115), 0.45, 0.25, spec=0.45),
    "sky":    make_mat("CoachSky", (0.62, 0.78, 0.95), 0.5, emit=(0.74, 0.87, 1.0), estr=2.0),
    "led":    make_mat("CoachLed", (0.9, 0.9, 0.85), 0.4, emit=(1.0, 0.95, 0.86), estr=2.6),
    "fan":    make_mat("CoachFan", (0.07, 0.07, 0.08), 0.4, 0.5, spec=0.5),
    "ceilpanel": make_mat("CeilingPanel", (0.62, 0.61, 0.59), 0.9, emit=(0.72, 0.70, 0.66), estr=3.0),
}


class MB:
    def __init__(self):
        self.verts, self.faces, self.mats = [], [], []

    def add_box(self, cx, cy, cz, sx, sy, sz, mat, rot_x=0.0, rot_z=0.0):
        x, y, z = sx / 2, sy / 2, sz / 2
        local = [(-x, -y, -z), (x, -y, -z), (x, y, -z), (-x, y, -z),
                 (-x, -y, z), (x, -y, z), (x, y, z), (-x, y, z)]
        cxr, snr = math.cos(rot_x), math.sin(rot_x)
        czr, szr = math.cos(rot_z), math.sin(rot_z)
        base = len(self.verts)
        for (lx, ly, lz) in local:
            ry = ly * cxr - lz * snr
            rz = ly * snr + lz * cxr
            rx = lx * czr - ry * szr
            ry2 = lx * szr + ry * czr
            self.verts.append((cx + rx, cy + ry2, cz + rz))
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


# ---------------------------------------------------------------- dimensions
COACH_W, COACH_L, CEIL = 3.40, 20.0, 2.95
WALL_T = 0.08
AISLE_HALF = 0.42
BERTH_X0, BERTH_X1 = 0.44, 1.62          # berth runs from the aisle to the wall
BERTH_W = 0.70                            # along the coach
TIER_Y = {"lower": 0.46, "middle": 1.16, "upper": 1.86}

BAYS = 6
BAY_PITCH = 3.0
BANK_OFFSET = 1.45                        # facing banks inside a bay
WINDOW_Z0, WINDOW_Z1 = 1.02, 1.80
WINDOW_W = 1.15

mb_floor = MB()
mb_wall = MB()
mb_trim = MB()
mb_ceil = MB()
mb_sky = MB()
mb_led = MB()
mb_berth = MB()
mb_frame = MB()
mb_fan = MB()

# ---------------------------------------------------------------- shell
mb_floor.add_box(0, 0, -0.05, COACH_W, COACH_L, 0.10, 0)
mb_floor.add_box(0, 0, 0.005, AISLE_HALF * 2, COACH_L - 0.8, 0.02, 0)
mb_wall.add_box(0, 0, CEIL + 0.05, COACH_W, COACH_L, 0.10, 0)
mb_wall.add_box(0, 0, CEIL - 0.03, COACH_W - 0.12, COACH_L - 0.24, 0.02, 0)

for sy in (-1, 1):
    mb_wall.add_box(0, sy * (COACH_L / 2 - 0.05), CEIL / 2, COACH_W, 0.10, CEIL, 0)

bank_zs = []
for b in range(BAYS):
    bay_c = -COACH_L / 2 + 2.0 + b * BAY_PITCH
    bank_zs.append(bay_c - BANK_OFFSET)
    bank_zs.append(bay_c + BANK_OFFSET)

# side walls with window openings aligned to the banks
for sy in (-1, 1):
    wx = sy * (COACH_W / 2 - WALL_T / 2)
    mb_wall.add_box(wx, 0, WINDOW_Z0 / 2, WALL_T, COACH_L, WINDOW_Z0, 0)
    mb_wall.add_box(wx, 0, (WINDOW_Z1 + CEIL) / 2, WALL_T, COACH_L, CEIL - WINDOW_Z1, 0)
    for bz in bank_zs:
        mb_sky.add_box(sy * (COACH_W / 2 - WALL_T - 0.03), bz, (WINDOW_Z0 + WINDOW_Z1) / 2,
                       0.02, WINDOW_W, WINDOW_Z1 - WINDOW_Z0, 0)
        mb_trim.add_box(wx, bz, WINDOW_Z1 + 0.045, WALL_T + 0.02, WINDOW_W + 0.10, 0.09, 0)
        mb_trim.add_box(wx, bz, WINDOW_Z0 - 0.045, WALL_T + 0.02, WINDOW_W + 0.10, 0.09, 0)
        # window bars
        for k in (-1, 0, 1):
            mb_trim.add_box(wx + sy * -0.02, bz + k * 0.33, (WINDOW_Z0 + WINDOW_Z1) / 2,
                            0.02, 0.03, WINDOW_Z1 - WINDOW_Z0, 0)
    # pillars between the openings
    for i in range(len(bank_zs) + 1):
        pz = -COACH_L / 2 + 1.0 + i * (COACH_L - 2.0) / len(bank_zs)
        mb_wall.add_box(wx, pz, (WINDOW_Z0 + WINDOW_Z1) / 2, WALL_T, 0.55, WINDOW_Z1 - WINDOW_Z0, 0)
    # ceiling led strips
    mb_led.add_box(sy * 0.78, 0, CEIL - 0.02, 0.09, COACH_L - 1.6, 0.03, 0)
    # luggage rack above the upper berth
    mb_trim.add_box(sy * 1.42, 0, 2.22, 0.34, COACH_L - 1.4, 0.04, 0)

# aisle grab rails
for sy in (-1, 1):
    mb_trim.add_box(sy * 0.46, 0, 2.30, 0.03, COACH_L - 2.0, 0.03, 0)

# ceiling fans — the giveaway detail of an Indian coach
for b in range(BAYS):
    bay_c = -COACH_L / 2 + 2.0 + b * BAY_PITCH
    mb_fan.add_box(0, bay_c, CEIL - 0.10, 0.14, 0.14, 0.10, 0)
    for k in range(3):
        ang = k * (2 * math.pi / 3)
        mb_fan.add_box(
            math.sin(ang) * 0.30, bay_c + math.cos(ang) * 0.30, CEIL - 0.13,
            0.10, 0.62, 0.03, 0, rot_z=-ang,
        )

# ---------------------------------------------------------------- berths
seats_json = []
row_letters = []
row_y = []
for idx, bz in enumerate(sorted(bank_zs)):
    letter = chr(65 + idx)
    row_letters.append({"letter": letter, "y": round(bz, 3), "floorZ": 0.0})
    row_y.append(bz)
    for side in (-1, 1):
        x_berth = side * (BERTH_X0 + BERTH_X1) / 2
        for tier, ty in TIER_Y.items():
            # cushion
            mb_berth.add_box(x_berth, bz, ty, BERTH_X1 - BERTH_X0, BERTH_W, 0.11, 0)
            # frame under the cushion
            mb_frame.add_box(x_berth, bz, ty - 0.09, BERTH_X1 - BERTH_X0 + 0.04, BERTH_W + 0.04, 0.06, 0)
            # support brackets at the wall end
            mb_frame.add_box(side * (BERTH_X1 - 0.02), bz, ty - 0.22, 0.05, BERTH_W, 0.28, 0)
            # upright post joining the tiers on the aisle side
            mb_frame.add_box(side * (BERTH_X0 + 0.03), bz + BERTH_W / 2 - 0.02, 1.16,
                             0.05, 0.05, 1.62, 0)

    map_x = {
        (-1, "lower"): -1.36, (-1, "middle"): -1.03, (-1, "upper"): -0.70,
        (1, "lower"): 0.70, (1, "middle"): 1.03, (1, "upper"): 1.36,
    }
    for side in (-1, 1):
        x_berth = side * (BERTH_X0 + BERTH_X1) / 2
        for tier, ty in TIER_Y.items():
            num = len([s for s in seats_json if s["row"] == letter]) + 1
            eye_y = ty + (0.52 if tier == "lower" else 0.46)
            seats_json.append({
                "id": f"{letter}{num}",
                "row": letter,
                "num": num,
                # schematic x for the 2D berth chart; the 3D camera uses `eye`
                "x": map_x[(side, tier)],
                "mapY": round(bz, 3),
                "eye": [round(x_berth, 3), round(eye_y, 3), round(-bz, 3)],
                "pos": [round(x_berth, 3), round(ty, 3), round(-bz, 3)],
                "tier": tier,
            })

# ---------------------------------------------------------------- build
mb_floor.build("CoachFloor", [MAT["floor"], MAT["aisle"]])
mb_wall.build("CoachWalls", [MAT["wall"]])
mb_trim.build("CoachTrim", [MAT["trim"]])

mb_sky.build("CoachSky", [MAT["sky"]])
mb_led.build("CoachLed", [MAT["led"]])
mb_fan.build("CoachFans", [MAT["fan"]])
mb_frame.build("BerthFrames", [MAT["frame"]])
berths_obj = mb_berth.build("BerthCushions", [MAT["berth"]])

bev = berths_obj.modifiers.new("Bevel", 'BEVEL')
bev.width = 0.045
bev.segments = 3
bev.limit_method = 'ANGLE'
bev.angle_limit = math.radians(35)
bev.use_clamp_overlap = True
bpy.context.view_layer.objects.active = berths_obj
bpy.ops.object.modifier_apply(modifier=bev.name)

os.makedirs(os.path.dirname(OUT_GLB), exist_ok=True)
bpy.ops.export_scene.gltf(filepath=OUT_GLB, export_format="GLB", export_apply=True, export_yup=True)

seat_map = {
    "version": 1,
    "units": "meters",
    "hall": {
        "width": COACH_W,
        "depth": COACH_L,
        "screen": {
            "y": 0.0, "z": COACH_L / 2, "w": 0.0, "h": 0.0,
            "center": [0.0, 1.40, COACH_L / 2],
            "frontLabel": "DIRECTION OF TRAVEL",
        },
    },
    "rows": row_letters,
    "aisleHalf": AISLE_HALF,
    "seats": seats_json,
}
os.makedirs(os.path.dirname(OUT_JSON), exist_ok=True)
with open(OUT_JSON, "w") as f:
    json.dump(seat_map, f, indent=1)

print(f"sleeper coach: {len(seats_json)} berths, {len(bank_zs)} banks")
