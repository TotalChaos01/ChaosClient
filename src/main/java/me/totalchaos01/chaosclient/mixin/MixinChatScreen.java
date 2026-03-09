package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.command.Command;
import me.totalchaos01.chaosclient.font.ChaosFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Chat screen mixin providing:
 * 1. Visual command suggestions popup when typing "."
 * 2. TAB completion for .commands and #baritone
 * 3. Arrow key navigation through suggestions
 */
@Mixin(ChatScreen.class)
public class MixinChatScreen {

    @Shadow protected TextFieldWidget chatField;

    // TAB completion state
    @Unique private List<String> chaosCompletions = new ArrayList<>();
    @Unique private int chaosCompletionIndex = -1;
    @Unique private String chaosLastPartial = "";

    // Visual suggestions state
    @Unique private List<String[]> chaos$suggestions = new ArrayList<>(); // [name, description, usage]
    @Unique private int chaos$selectedSuggestion = -1;
    @Unique private String chaos$lastSuggestionInput = "";

    @Unique private static final List<String> BARITONE_COMMANDS = Arrays.asList(
            "goto", "mine", "follow", "farm", "explore", "path", "stop", "cancel",
            "pause", "resume", "build", "schematica", "sel", "click",
            "thisway", "goal", "wp", "spawn", "home", "sethome",
            "set", "version", "help", "invert", "come", "surface",
            "tunnel", "axis", "proc", "repack", "blacklist"
    );

    // ─── Visual suggestions rendering ─────────────────────

    @Inject(method = "render", at = @At("RETURN"))
    private void chaos$onRender(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            if (chatField == null) return;
            String text = chatField.getText();
            if (text == null || !text.startsWith(".")) {
                chaos$suggestions.clear();
                chaos$selectedSuggestion = -1;
                return;
            }

            chaos$updateSuggestions(text);
            if (chaos$suggestions.isEmpty()) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            int screenHeight = mc.getWindow().getScaledHeight();

            // Position popup just above the chat input
            int popupX = 4;
            int popupY = screenHeight - 28 - (chaos$suggestions.size() * 14) - 6;
            int maxWidth = 0;

            // Calculate max width
            for (String[] s : chaos$suggestions) {
                String line = "." + s[0] + "  §7" + s[1];
                int w = ChaosFont.getWidth(line) + 12;
                maxWidth = Math.max(maxWidth, w);
            }
            maxWidth = Math.max(maxWidth, 120);

            // Draw background
            ctx.fill(popupX, popupY - 2, popupX + maxWidth, popupY + chaos$suggestions.size() * 14 + 2, 0xE0101020);

            // Draw top accent line
            ctx.fill(popupX, popupY - 2, popupX + maxWidth, popupY - 1, 0xFFAA55FF);

            // Draw each suggestion
            for (int i = 0; i < chaos$suggestions.size(); i++) {
                String[] s = chaos$suggestions.get(i);
                int y = popupY + i * 14;
                boolean selected = (i == chaos$selectedSuggestion);

                if (selected) {
                    ctx.fill(popupX, y - 1, popupX + maxWidth, y + 13, 0x40AA55FF);
                }

                // Command name in accent color, description in gray
                String cmdName = "§d." + s[0];
                String desc = " §8— §7" + s[1];
                ChaosFont.drawWithShadow(ctx, cmdName + desc, popupX + 4, y + 2, 0xFFFFFFFF);
            }
        } catch (Exception ignored) {}
    }

    @Unique
    private void chaos$updateSuggestions(String text) {
        if (text.equals(chaos$lastSuggestionInput)) return;
        chaos$lastSuggestionInput = text;

        String partial = text.substring(1).toLowerCase().split("\\s+")[0];
        chaos$suggestions.clear();
        chaos$selectedSuggestion = -1;

        if (ChaosClient.getInstance() == null || ChaosClient.getInstance().getCommandManager() == null) return;

        for (Command cmd : ChaosClient.getInstance().getCommandManager().getCommands()) {
            if (partial.isEmpty() || cmd.getName().toLowerCase().startsWith(partial)) {
                chaos$suggestions.add(new String[]{cmd.getName(), cmd.getDescription(), cmd.getUsage()});
            }
            if (chaos$suggestions.size() >= 10) break;
        }
    }

    // ─── TAB + Arrow key handling ─────────────────────────

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput keyInput, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (chatField == null) return;
            String text = chatField.getText();
            if (text == null) return;

            // Arrow Up/Down — navigate visual suggestions
            if (text.startsWith(".") && !chaos$suggestions.isEmpty()) {
                if (keyInput.key() == GLFW.GLFW_KEY_UP) {
                    chaos$selectedSuggestion = Math.max(0, chaos$selectedSuggestion - 1);
                    chaos$applySuggestion();
                    cir.setReturnValue(true);
                    return;
                }
                if (keyInput.key() == GLFW.GLFW_KEY_DOWN) {
                    chaos$selectedSuggestion = Math.min(chaos$suggestions.size() - 1, chaos$selectedSuggestion + 1);
                    chaos$applySuggestion();
                    cir.setReturnValue(true);
                    return;
                }
            }

            // TAB — cycle completions
            if (keyInput.key() != GLFW.GLFW_KEY_TAB) return;

            if (text.startsWith(".")) {
                // If we have visual suggestions, use them
                if (!chaos$suggestions.isEmpty()) {
                    chaos$selectedSuggestion = (chaos$selectedSuggestion + 1) % chaos$suggestions.size();
                    chaos$applySuggestion();
                    cir.setReturnValue(true);
                    return;
                }
                handleChaosCompletion(text, cir);
                return;
            }

            if (text.startsWith("#")) {
                handleBaritoneCompletion(text, cir);
            }
        } catch (Exception ignored) {}
    }

    @Unique
    private void chaos$applySuggestion() {
        if (chaos$selectedSuggestion < 0 || chaos$selectedSuggestion >= chaos$suggestions.size()) return;
        String name = chaos$suggestions.get(chaos$selectedSuggestion)[0];
        chatField.setText("." + name + " ");
        // Reset suggestion input tracking so it doesn't re-filter
        chaos$lastSuggestionInput = chatField.getText();
    }

    // ─── Original TAB completion (fallback) ───────────────

    @Unique
    private void handleChaosCompletion(String text, CallbackInfoReturnable<Boolean> cir) {
        if (ChaosClient.getInstance() == null || ChaosClient.getInstance().getCommandManager() == null) return;

        String partial = text.substring(1);

        if (!partial.equals(chaosLastPartial)) {
            chaosLastPartial = partial;
            chaosCompletions = new ArrayList<>(
                    ChaosClient.getInstance().getCommandManager().getCompletions(partial)
            );
            chaosCompletionIndex = -1;
        }

        if (chaosCompletions.isEmpty()) return;

        chaosCompletionIndex = (chaosCompletionIndex + 1) % chaosCompletions.size();
        String completion = chaosCompletions.get(chaosCompletionIndex);
        chatField.setText("." + completion + (completion.contains(" ") ? "" : " "));
        cir.setReturnValue(true);
    }

    @Unique
    private void handleBaritoneCompletion(String text, CallbackInfoReturnable<Boolean> cir) {
        String partial = text.substring(1).toLowerCase();

        List<String> commands = getBaritoneCommandsReflection();
        if (commands == null) {
            commands = BARITONE_COMMANDS;
        }

        final String match = partial;

        if (!partial.equals(chaosLastPartial)) {
            chaosLastPartial = partial;
            chaosCompletions = commands.stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(match))
                    .sorted()
                    .collect(Collectors.toList());
            chaosCompletionIndex = -1;
        }

        if (chaosCompletions.isEmpty()) return;

        chaosCompletionIndex = (chaosCompletionIndex + 1) % chaosCompletions.size();
        String completion = chaosCompletions.get(chaosCompletionIndex);
        chatField.setText("#" + completion + " ");
        cir.setReturnValue(true);
    }

    @Unique
    private List<String> getBaritoneCommandsReflection() {
        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = apiClass.getMethod("getProvider").invoke(null);
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object cmdManager = baritone.getClass().getMethod("getCommandManager").invoke(baritone);
            Object registry = cmdManager.getClass().getMethod("getRegistry").invoke(cmdManager);
            @SuppressWarnings("unchecked")
            Iterable<Object> entries = (Iterable<Object>) registry.getClass().getMethod("entries").invoke(registry);
            List<String> names = new ArrayList<>();
            for (Object entry : entries) {
                @SuppressWarnings("unchecked")
                List<String> entryNames = (List<String>) entry.getClass().getMethod("getNames").invoke(entry);
                if (entryNames != null && !entryNames.isEmpty()) {
                    names.add(entryNames.get(0));
                }
            }
            return names.isEmpty() ? null : names;
        } catch (Exception e) {
            return null;
        }
    }
}
