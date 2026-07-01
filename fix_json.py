import json

files = [
    "/Users/carlo/Locale/AndroidStudio/Android InOrario/app/src/main/assets/rfi_stations.json",
    "/Users/carlo/Locale/Xcode/In Orario/In Orario/rfi_stations.json"
]

for fpath in files:
    with open(fpath, "r") as f:
        stations = json.load(f)
    for s in stations:
        if s.get("lat") and (s["lat"] > 50 or s["lat"] < 35 or s["lon"] < 6 or s["lon"] > 19):
            print(f"Removing bad coords from {s['name']}")
            del s["lat"]
            del s["lon"]
    with open(fpath, "w") as f:
        json.dump(stations, f, indent=4)
