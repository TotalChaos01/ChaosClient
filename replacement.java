    public static void drawSmoothLine(DrawContext ctx, double x1, double y1, double x2, double y2,
                                       float lineWidth, int color) {
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Since GL_LINE_SMOOTH might be deprecated in core profile, we just draw standard lines
        // A better approach in modern MC is a thin rotated quad, but we'll use DEBUG_LINES for now.
        RenderSystem.lineWidth(lineWidth);
        
        float a = (color >> 24 & 255) / 255.0f;
        float r = (color >> 16 & 255) / 255.0f;
        float g = (color >> 8 & 255) / 255.0f;
        float b = (color & 255) / 255.0f;
        
        buffer.vertex(matrix, (float)x1, (float)y1, 0.0f).color(r, g, b, a);
        buffer.vertex(matrix, (float)x2, (float)y2, 0.0f).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
    }

    public static void drawGradientLine(DrawContext ctx, double x1, double y1, double x2, double y2,
                                         float lineWidth, int startColor, int endColor) {
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(lineWidth);
        
        float a1 = (startColor >> 24 & 255) / 255.0f, r1 = (startColor >> 16 & 255) / 255.0f, g1 = (startColor >> 8 & 255) / 255.0f, b1 = (startColor & 255) / 255.0f;
        float a2 = (endColor >> 24 & 255) / 255.0f, r2 = (endColor >> 16 & 255) / 255.0f, g2 = (endColor >> 8 & 255) / 255.0f, b2 = (endColor & 255) / 255.0f;
        
        buffer.vertex(matrix, (float)x1, (float)y1, 0.0f).color(r1, g1, b1, a1);
        buffer.vertex(matrix, (float)x2, (float)y2, 0.0f).color(r2, g2, b2, a2);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
    }
