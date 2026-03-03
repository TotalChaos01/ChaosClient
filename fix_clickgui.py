import sys
content = open("src/main/java/me/totalchaos01/chaosclient/ui/clickgui/ClickGuiScreen.java").read()

if "ctx.drawTextWithShadow(client.textRenderer, \"ChaosClient v1.1.1\", 6, 6, 0xFFFFFFFF);" in content:
    content = content.replace("ctx.drawTextWithShadow(client.textRenderer, \"ChaosClient v1.1.1\", 6, 6, 0xFFFFFFFF);", """
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(1.5f, 1.5f, 1f);
        ctx.drawTextWithShadow(client.textRenderer, "ChaosClient", 4, 4, 0xFFAAAAAA);
        ctx.getMatrices().popMatrix();
        ctx.drawTextWithShadow(client.textRenderer, "v" + me.totalchaos01.chaosclient.ChaosClient.CLIENT_VERSION, 6, 20, 0xFF666666);
    """)

open("src/main/java/me/totalchaos01/chaosclient/ui/clickgui/ClickGuiScreen.java", "w").write(content)
