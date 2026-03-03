import sys
content = open("src/main/java/me/totalchaos01/chaosclient/module/impl/render/ESP.java").read()

content = content.replace("boolean anyVisible = false;", "boolean anyVisible = false;\n        int visibleCount = 0;")
content = content.replace("if (screen == null) continue;", "if (screen == null || screen[2] < 0 || screen[2] > 1.0) continue;\n            visibleCount++;")
content = content.replace("if (!anyVisible) return;", "if (visibleCount < 4) return;")

open("src/main/java/me/totalchaos01/chaosclient/module/impl/render/ESP.java", "w").write(content)

