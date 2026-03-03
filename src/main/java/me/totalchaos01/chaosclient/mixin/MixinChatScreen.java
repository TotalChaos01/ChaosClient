package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Intercepts TAB key in ChatScreen to provide:
 *   .command  — ChaosClient command tab-completion
 *   #command  — Baritone command tab-completion (common commands)
 */
@Mixin(ChatScreen.class)
public class MixinChatScreen {

    @Shadow protected TextFieldWidget chatField;

    @Unique private List<String> chaosCompletions = new ArrayList<>();
    @Unique private int chaosCompletionIndex = -1;
    @Unique private String chaosLastPartial = "";

    /** Common Baritone commands for tab-completion */
    @Unique private static final List<String> BARITONE_COMMANDS = Arrays.asList(
            "goto", "mine", "follow", "farm", "explore", "path", "stop", "cancel",
            "pause", "resume", "build", "schematica", "sel", "click",
            "thisway", "goal", "wp", "spawn", "home", "sethome",
            "set", "version", "help", "invert", "come", "surface",
            "tunnel", "axis", "proc", "repack", "blacklist"
    );

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput keyInput, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (chatField == null) return;
            if (keyInput.key() != GLFW.GLFW_KEY_TAB) return;

            String text = chatField.getText();
            if (text == null) return;

            // ─── ChaosClient .command completion ───
            if (text.startsWith(".")) {
                handleChaosCompletion(text, cir);
                return;
            }

            // ─── Baritone #command completion ───
            if (text.startsWith("#")) {
                handleBaritoneCompletion(text, cir);
                return;
            }
        } catch (Exception ignored) {
            // Defensive — never crash the chat screen
        }
    }

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

        // First try to get Baritone's own command list via reflection
        List<String> commands = getBaritoneCommandsReflection();
        if (commands == null) {
            commands = BARITONE_COMMANDS; // fallback to hardcoded list
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

    /** Try to get Baritone command names via reflection. Returns null if Baritone not loaded. */
    @Unique
    private List<String> getBaritoneCommandsReflection() {
        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = apiClass.getMethod("getProvider").invoke(null);
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object cmdManager = baritone.getClass().getMethod("getCommandManager").invoke(baritone);
            Object registry = cmdManager.getClass().getMethod("getRegistry").invoke(cmdManager);
            // registry.entries() returns Collection<ICommand>
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
