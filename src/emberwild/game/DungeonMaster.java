package emberwild.game;

import emberwild.rules.Skill;
import emberwild.util.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/**
 * The improv Dungeon Master. Players say or do anything in character; the DM answers in the voice of the scene's NPC
 * and may call for a roleplay check. It never changes the plot (flags, scenes, loot) — it adds colour, and a great
 * roleplay success earns Heroic Inspiration.
 *
 * <p>Providers (configured by the host in the ☰ menu, or with --ai flags):
 * <ul>
 *   <li><b>rules</b> — built in, no setup: keyword intent → skill check → hand-written reactions.</li>
 *   <li><b>ollama</b> — a local model through Ollama's OpenAI-compatible API (default http://localhost:11434).</li>
 *   <li><b>openai</b> — any OpenAI-compatible endpoint (LM Studio, llama.cpp server, vLLM, OpenAI…).</li>
 *   <li><b>anthropic</b> — Claude via the Messages API (key from ANTHROPIC_API_KEY or the settings file).</li>
 * </ul>
 */
public final class DungeonMaster {
    public String provider = "rules", url = "", model = "", apiKey = "";
    private final Path settingsFile;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public DungeonMaster(Path settingsFile) {
        this.settingsFile = settingsFile;
        load();
        String env = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey.isBlank() && env != null) apiKey = env;
    }

    public record Reply(String npc, String npcLine, String narration, String skill, int dc, String source) {}

    // ------------------------------------------------------------------------------------ settings
    void load() {
        try {
            if (settingsFile != null && Files.exists(settingsFile)) {
                Map<String, Object> m = Json.parseObject(Files.readString(settingsFile));
                Map<String, Object> ai = Json.map(m, "ai");
                if (ai != null) {
                    provider = Json.str(ai, "provider", provider);
                    url = Json.str(ai, "url", url);
                    model = Json.str(ai, "model", model);
                    apiKey = Json.str(ai, "apiKey", apiKey);
                }
            }
        } catch (Exception e) { System.err.println("Could not read settings: " + e.getMessage()); }
    }

    public synchronized void configure(String provider, String url, String model, String apiKey) {
        this.provider = provider == null || provider.isBlank() ? "rules" : provider;
        this.url = url == null ? "" : url.trim();
        this.model = model == null ? "" : model.trim();
        if (apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("••")) this.apiKey = apiKey.trim();
        try {
            if (settingsFile != null) {
                Map<String, Object> all = Files.exists(settingsFile) ? Json.parseObject(Files.readString(settingsFile)) : new LinkedHashMap<>();
                all.put("ai", Json.obj("provider", this.provider, "url", this.url, "model", this.model, "apiKey", this.apiKey));
                Files.createDirectories(settingsFile.getParent());
                Files.writeString(settingsFile, Json.pretty(all), StandardCharsets.UTF_8);
            }
        } catch (Exception e) { System.err.println("Could not save settings: " + e.getMessage()); }
    }

    public Map<String, Object> describe() {
        return Json.obj("provider", provider, "url", url, "model", model, "hasKey", !apiKey.isBlank(),
                "label", switch (provider) {
                    case "ollama" -> "Local AI (Ollama" + (model.isBlank() ? "" : ": " + model) + ")";
                    case "openai" -> "AI (" + (model.isBlank() ? "OpenAI-compatible" : model) + ")";
                    case "anthropic" -> "Claude" + (model.isBlank() ? "" : " (" + model + ")");
                    default -> "Built-in improv rules";
                });
    }

    // ------------------------------------------------------------------------------------ main entry
    /** Produce the DM's reaction. Runs outside the game lock (it may call a model over the network). */
    public Reply respond(Map<String, Object> ctx) {
        if (!provider.equals("rules")) {
            try {
                Reply r = askModel(ctx);
                if (r != null) return r;
            } catch (Exception e) {
                System.err.println("AI DM unavailable (" + e.getMessage() + ") — using built-in rules.");
            }
        }
        return rules(ctx);
    }

    /** Short narration of how a roleplay check turned out. */
    public String outcome(Map<String, Object> ctx, boolean success) {
        if (!provider.equals("rules")) {
            try {
                String sys = "You are the Dungeon Master of a warm, funny, family-friendly D&D 2024 game. Reply with ONE or TWO short sentences of vivid narration, present tense, no dice numbers, no game mechanics words. Never change the story's facts or give items.";
                String user = context(ctx) + "\nThe hero's " + Json.str(ctx, "skillLabel", "check") + " check " + (success ? "SUCCEEDED" : "FAILED")
                        + ". Describe how " + Json.str(ctx, "npc", "the world") + " reacts.";
                String t = chat(sys, user, 160);
                if (t != null && !t.isBlank()) return clean(t);
            } catch (Exception ignored) {}
        }
        String npc = Json.str(ctx, "npc", "");
        List<String> win = npc.isEmpty() ? List.of("It works — better than you dared hope.", "The moment lands perfectly.", "Fortune smiles, and so does everyone watching.")
                : List.of(npc + " is clearly won over.", npc + " laughs despite themselves, and the mood softens.", npc + " nods slowly. \"Fair enough.\"");
        List<String> lose = npc.isEmpty() ? List.of("It doesn't quite come off.", "Nothing happens — except some awkward silence.", "The moment passes you by.")
                : List.of(npc + " looks distinctly unimpressed.", npc + " raises an eyebrow and says nothing at all.", npc + " snorts. \"Nice try.\"");
        List<String> l = success ? win : lose;
        return l.get(new Random().nextInt(l.size()));
    }

    // ------------------------------------------------------------------------------------ language model
    private String context(Map<String, Object> ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Campaign: ").append(Json.str(ctx, "campaign", "")).append("\n");
        sb.append("Scene: ").append(Json.str(ctx, "scene", "")).append("\n");
        String text = Json.str(ctx, "sceneText", "");
        if (text.length() > 1200) text = text.substring(0, 1200) + "…";
        sb.append("What the players see: ").append(text.replace("*", "")).append("\n");
        if (!Json.str(ctx, "npc", "").isEmpty()) sb.append("NPC present: ").append(Json.str(ctx, "npc", "")).append(" (").append(Json.str(ctx, "npcTitle", "")).append(")\n");
        sb.append("Party: ").append(Json.str(ctx, "party", "")).append("\n");
        String recent = Json.str(ctx, "recent", "");
        if (!recent.isBlank()) sb.append("Recent conversation:\n").append(recent).append("\n");
        sb.append(Json.str(ctx, "hero", "A hero")).append(" (").append(Json.str(ctx, "heroDesc", "")).append(") ")
                .append(Json.str(ctx, "kind", "say").equals("do") ? "does this: " : "says: \"").append(Json.str(ctx, "text", ""))
                .append(Json.str(ctx, "kind", "say").equals("do") ? "" : "\"");
        return sb.toString();
    }

    private Reply askModel(Map<String, Object> ctx) throws Exception {
        String skills = String.join(", ", Arrays.stream(Skill.values()).map(Skill::key).toList());
        String sys = """
                You are the Dungeon Master of a warm, funny, family-friendly D&D 2024 tabletop game. A player has just said or done something in character.
                React like a great improv DM: keep it short and vivid, stay consistent with the scene, never change plot facts, never give items, gold or information that would skip the adventure.
                Reply ONLY with a JSON object, no markdown:
                {"npc": "name of who answers, or empty if no one", "npcLine": "what that character says aloud (max 2 sentences, in their voice)", "narration": "one short sentence describing what happens (present tense)", "skill": "one of [%s] if the action's outcome is genuinely uncertain, else empty", "dc": 10-18 or 0}
                Only ask for a check when the hero tries to persuade, deceive, intimidate, perform, search, sneak, climb, recall lore or similar. Casual talk needs no check.
                """.formatted(skills);
        String raw = chat(sys, context(ctx), 300);
        if (raw == null) return null;
        int a = raw.indexOf('{'), b = raw.lastIndexOf('}');
        if (a < 0 || b <= a) return new Reply(Json.str(ctx, "npc", ""), clean(raw), "", "", 0, provider);
        Map<String, Object> m = Json.parseObject(raw.substring(a, b + 1));
        String skill = Json.str(m, "skill", "").toLowerCase(Locale.ROOT).replace(' ', '_');
        int dc = Math.max(0, Math.min(20, Json.integer(m, "dc", 0)));
        if (Skill.of(skill) == null) { skill = ""; dc = 0; }
        if (!skill.isEmpty() && dc < 8) dc = 12;
        return new Reply(clean(Json.str(m, "npc", "")), clean(Json.str(m, "npcLine", "")), clean(Json.str(m, "narration", "")), skill, dc, provider);
    }

    /** One chat completion. Returns the text, or null. */
    String chat(String system, String user, int maxTokens) throws Exception {
        HttpRequest req;
        switch (provider) {
            case "anthropic" -> {
                if (apiKey.isBlank()) throw new IllegalStateException("no Anthropic API key");
                String body = Json.write(Json.obj("model", model.isBlank() ? "claude-haiku-5-5" : model, "max_tokens", maxTokens, "system", system,
                        "messages", List.of(Json.obj("role", "user", "content", user))));
                req = HttpRequest.newBuilder(URI.create(url.isBlank() ? "https://api.anthropic.com/v1/messages" : url))
                        .timeout(Duration.ofSeconds(30)).header("content-type", "application/json").header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            }
            default -> {
                String base = url.isBlank() ? (provider.equals("ollama") ? "http://localhost:11434" : "http://localhost:1234") : url.replaceAll("/+$", "");
                if (!base.endsWith("/chat/completions")) base = base.endsWith("/v1") ? base + "/chat/completions" : base + "/v1/chat/completions";
                String mdl = model.isBlank() && provider.equals("ollama") ? firstOllamaModel() : model;
                String body = Json.write(Json.obj("model", mdl, "max_tokens", maxTokens, "temperature", 0.8, "stream", false,
                        "messages", List.of(Json.obj("role", "system", "content", system), Json.obj("role", "user", "content", user))));
                HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base)).timeout(Duration.ofSeconds(45)).header("content-type", "application/json");
                if (!apiKey.isBlank() && !provider.equals("ollama")) b.header("Authorization", "Bearer " + apiKey);
                req = b.POST(HttpRequest.BodyPublishers.ofString(body)).build();
            }
        }
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + res.statusCode() + " " + res.body().substring(0, Math.min(200, res.body().length())));
        Map<String, Object> m = Json.parseObject(res.body());
        if (provider.equals("anthropic")) {
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> c : Json.maps(m, "content")) if ("text".equals(Json.str(c, "type", ""))) sb.append(Json.str(c, "text", ""));
            return sb.toString();
        }
        List<Map<String, Object>> choices = Json.maps(m, "choices");
        if (choices.isEmpty()) return null;
        return Json.str(Json.map(choices.get(0), "message"), "content", null);
    }

    private String firstOllamaModel() throws Exception {
        String base = url.isBlank() ? "http://localhost:11434" : url.replaceAll("/v1.*$", "").replaceAll("/+$", "");
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(base + "/api/tags")).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
        List<Map<String, Object>> models = Json.maps(Json.parseObject(res.body()), "models");
        if (models.isEmpty()) throw new IllegalStateException("Ollama has no models installed");
        return Json.str(models.get(0), "name", "llama3.2");
    }

    /** Test the configured provider: returns a short message. */
    public String test() {
        if (provider.equals("rules")) return "Built-in improv rules are always available.";
        try {
            String t = chat("Reply with exactly: The dice are ready.", "Say it.", 20);
            return t == null ? "No reply from the model." : "Connected — the model says: " + clean(t);
        } catch (Exception e) { return "Could not reach the model: " + e.getMessage(); }
    }

    private static String clean(String s) {
        if (s == null) return "";
        s = s.replaceAll("(?s)<think>.*?</think>", "").replace("```json", "").replace("```", "").trim();
        if (s.length() > 400) s = s.substring(0, 400);
        return s;
    }

    // ------------------------------------------------------------------------------------ built-in improv rules
    private static final Object[][] INTENTS = {
            {"persuasion", 12, new String[]{"please", "convince", "persuade", "trust me", "help us", "we need", "reason", "deal", "offer", "promise", "negotiat", "beg", "flatter", "compliment", "charm", "smile", "wink", "flirt"}},
            {"deception", 13, new String[]{"lie", "pretend", "bluff", "trick", "fake", "disguise", "actually we're", "we are from", "i am the", "con "}},
            {"intimidation", 13, new String[]{"threat", "or else", "scare", "intimidat", "growl", "back off", "last warning", "you'll regret", "i'll burn"}},
            {"insight", 12, new String[]{"is he lying", "is she lying", "read", "suspicious", "what do they want", "motive", "truth"}},
            {"perception", 12, new String[]{"look around", "listen", "watch", "spot", "notice", "scan", "keep an eye"}},
            {"investigation", 12, new String[]{"search", "inspect", "examine", "investigate", "check the", "study", "clue"}},
            {"performance", 11, new String[]{"sing", "dance", "play my", "perform", "juggle", "joke", "toast", "story"}},
            {"athletics", 12, new String[]{"climb", "lift", "push", "jump", "swim", "break", "force", "arm wrestle"}},
            {"stealth", 12, new String[]{"sneak", "hide", "quietly", "creep", "tiptoe"}},
            {"arcana", 12, new String[]{"magic", "arcane", "rune", "spell", "enchant", "glyph"}},
            {"history", 12, new String[]{"remember", "legend", "history", "recall", "heard of"}},
            {"religion", 12, new String[]{"pray", "bless", "god", "holy", "temple"}},
            {"animal_handling", 12, new String[]{"pet", "calm the", "feed the", "good boy", "horse", "wolf"}},
            {"sleight_of_hand", 13, new String[]{"pickpocket", "steal", "palm", "swipe", "lift his", "lift her"}},
            {"medicine", 11, new String[]{"bandage", "heal", "tend", "wound"}},
    };

    Reply rules(Map<String, Object> ctx) {
        String text = Json.str(ctx, "text", "").toLowerCase(Locale.ROOT);
        String npc = Json.str(ctx, "npc", "");
        String hero = Json.str(ctx, "hero", "the hero").split(" ")[0];
        Random r = new Random();
        for (Object[] in : INTENTS) {
            for (String kw : (String[]) in[2]) {
                if (text.contains(kw)) {
                    String skill = (String) in[0];
                    String line = npc.isEmpty() ? "" : switch (skill) {
                        case "persuasion" -> pick(r, "Hmm. Go on, then — convince me.", "You've a silver tongue, " + hero + ". Let's see if it's real silver.", "I'm listening… for now.");
                        case "deception" -> pick(r, "Is that so?", "Huh. Funny, you don't look like that.", "…Really.");
                        case "intimidation" -> pick(r, "Was that a threat?", "Easy now. Easy.", "You don't scare me. Much.");
                        case "performance" -> pick(r, "Oh, this I have to see.", "Go on then, give us a show!", "The whole room's watching you now.");
                        default -> pick(r, "What are you up to?", "Careful there.", "Hm?");
                    };
                    String narration = switch (skill) {
                        case "perception", "investigation" -> hero + " takes a careful look around…";
                        case "stealth" -> hero + " tries to move without a sound…";
                        case "athletics" -> hero + " sets their feet and gives it everything…";
                        default -> "";
                    };
                    return new Reply(npc, line, narration, skill, (int) in[1], "rules");
                }
            }
        }
        if (text.endsWith("?")) {
            String line = npc.isEmpty() ? "" : pick(r, "Ask me again after another drink.", "Now that is a good question.", "If I knew that, I wouldn't still be here, would I?", "Some things are better left unasked… but I'll think on it.");
            return new Reply(npc, line, npc.isEmpty() ? "Only the wind answers." : "", "", 0, "rules");
        }
        String line = npc.isEmpty() ? "" : pick(r, "Ha! I like you.", "If you say so, friend.", "Mm. Noted.", "Well, aren't you a character.", "You lot are going to be trouble, I can tell.", "Heh. Fair.");
        return new Reply(npc, line, npc.isEmpty() ? pick(r, "Your words hang in the air, and the world takes note.", "Somewhere nearby, someone chuckles.", "The moment passes, but it was a good one.") : "", "", 0, "rules");
    }

    private static String pick(Random r, String... s) { return s[r.nextInt(s.length)]; }
}
