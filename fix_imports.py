import sys

content = open("src/main/java/me/totalchaos01/chaosclient/util/render/RenderUtil.java").read()

imports = """
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.BufferRenderer;
import org.joml.Matrix4f;
"""

if "import com.mojang.blaze3d.systems.RenderSystem;" not in content:
    content = content.replace("import net.minecraft.client.MinecraftClient;", imports + "import net.minecraft.client.MinecraftClient;")
    open("src/main/java/me/totalchaos01/chaosclient/util/render/RenderUtil.java", "w").write(content)

