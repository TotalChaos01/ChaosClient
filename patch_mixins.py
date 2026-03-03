import json

with open("src/main/resources/chaosclient.mixins.json") as f:
    data = json.load(f)

if "MixinMultiplayerScreen" not in data["client"]:
    data["client"].append("MixinMultiplayerScreen")

with open("src/main/resources/chaosclient.mixins.json", "w") as f:
    json.dump(data, f, indent=2)
