import sys
content = open("src/main/java/me/totalchaos01/chaosclient/module/impl/render/Tracers.java").read()

content = content.replace("RenderUtil.drawGradientLine(ctx, centerX, centerY, target[0], target[1], lw, startColor, endColor);", "RenderUtil.drawGradientLine(ctx, centerX, centerY, target[0], target[1], lw * 1.5f, startColor, endColor);")

open("src/main/java/me/totalchaos01/chaosclient/module/impl/render/Tracers.java", "w").write(content)
