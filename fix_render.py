import sys
content = open("src/main/java/me/totalchaos01/chaosclient/util/render/RenderUtil.java").read()

content = content.replace("int steps = Math.max(1, Math.min((int) (len * 1.5), 400));", "int steps = Math.max(1, Math.min((int) (len * 0.4), 80));")
content = content.replace("ctx.fill(px - hlw, py - hlw, px + hlw + 1, py + hlw + 1, color);", "ctx.fill(px - hlw - 1, py - hlw - 1, px + hlw + 2, py + hlw + 2, color);")

content = content.replace("int steps = Math.max(1, Math.min((int) (len * 1.2), 300));", "int steps = Math.max(1, Math.min((int) (len * 0.4), 80));")
content = content.replace("ctx.fill(px - offset, py + i", "ctx.fill(px - offset, py + i")

open("src/main/java/me/totalchaos01/chaosclient/util/render/RenderUtil.java", "w").write(content)
