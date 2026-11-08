#!/usr/bin/env python3
"""Fetch real movie posters and widescreen backdrops from TMDB web pages (curl-based)."""
import json
import os
import re
import subprocess
import sys
import urllib.parse

UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

MOVIES = [
    ("avengers_doomsday", "Avengers Doomsday"),
    ("interstellar", "Interstellar"),
    ("oppenheimer", "Oppenheimer"),
    ("dune_part_two", "Dune Part Two"),
    ("spider_verse", "Spider-Man Across the Spider-Verse"),
    ("the_batman", "The Batman"),
]

OUT = sys.argv[1]


def curl_text(url):
    r = subprocess.run(["curl", "-sL", "--max-time", "30", "-H", f"User-Agent: {UA}", url],
                       capture_output=True, text=True)
    return r.stdout


def curl_bin(url, dest):
    r = subprocess.run(["curl", "-sL", "--max-time", "60", "-o", dest, "-w", "%{http_code}",
                        "-H", f"User-Agent: {UA}",
                        "-H", "Referer: https://www.themoviedb.org/",
                        "-H", "Accept: image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
                        url], capture_output=True, text=True)
    return r.stdout.strip() == "200"


def find_movie_page(query):
    html = curl_text("https://www.themoviedb.org/search/movie?query=" + urllib.parse.quote(query))
    m = re.search(r'href="(/movie/\d+-[^"]*)"', html)
    return m.group(1) if m else None


def fetch_art(page):
    html = curl_text("https://www.themoviedb.org" + page)
    imgs = re.findall(r'og:image" content="([^"]+)"', html)
    return (imgs[0] if imgs else None), (imgs[1] if len(imgs) > 1 else None)


def upsize(url, size):
    return re.sub(r'/t/p/w\d+/', f'/t/p/{size}/', url)


os.makedirs(OUT, exist_ok=True)
manifest = {}
for slug, query in MOVIES:
    entry = {}
    page = find_movie_page(query)
    if not page:
        print(f"{slug}: page not found")
        continue
    poster, backdrop = fetch_art(page)
    if poster:
        p = os.path.join(OUT, f"{slug}.jpg")
        if curl_bin(upsize(poster, "w780"), p):
            entry["poster"] = p
            print(f"{slug}: poster {os.path.getsize(p)//1024}KB")
    if backdrop:
        p = os.path.join(OUT, f"screen_{slug}.jpg")
        if curl_bin(upsize(backdrop, "w1280"), p):
            entry["screen"] = p
            print(f"{slug}: backdrop {os.path.getsize(p)//1024}KB")
    manifest[slug] = entry

json.dump(manifest, open("/tmp/asset_manifest2.json", "w"), indent=1)
print("done")
