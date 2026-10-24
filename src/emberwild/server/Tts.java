package emberwild.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Optional neural text-to-speech with Piper (https://github.com/rhasspy/piper) — fast, offline, free, and far more
 * natural than browser voices. If a piper executable and at least one voice model (*.onnx + *.onnx.json) are found,
 * every browser at the table can use it; otherwise clients fall back to their best built-in voices.
 *
 * Looks for:  --piper PATH  |  $EMBERWILD_PIPER  |  ./piper/piper(.exe)   and voices in  ./voices/  (or --voices DIR)
 */
public final class Tts {
    private final Path piper, voices, cache;

    public Tts(Path home, String piperArg, String voicesArg) {
        Path exe = null;
        List<String> candidates = new ArrayList<>();
        if (piperArg != null) candidates.add(piperArg);
        if (System.getenv("EMBERWILD_PIPER") != null) candidates.add(System.getenv("EMBERWILD_PIPER"));
        candidates.add(home.resolve("piper").resolve("piper").toString());
        candidates.add(home.resolve("piper").resolve("piper.exe").toString());
        for (String c : candidates) { Path p = Paths.get(c); if (Files.isRegularFile(p) && Files.isExecutable(p)) { exe = p; break; } }
        this.piper = exe;
        this.voices = voicesArg != null ? Paths.get(voicesArg) : home.resolve("voices");
        this.cache = home.resolve("saves").resolve("tts-cache");
    }

    public boolean available() { return piper != null && !voices().isEmpty(); }

    /** Voice model names (file names without .onnx). */
    public List<String> voices() {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(voices)) return out;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(voices, "*.onnx")) {
            for (Path p : ds) if (Files.exists(Paths.get(p + ".json"))) out.add(p.getFileName().toString().replaceAll("\\.onnx$", ""));
        } catch (IOException ignored) {}
        Collections.sort(out);
        return out;
    }

    /** Synthesize speech to WAV bytes (cached). rate: 0.5..2 (higher = faster). */
    public synchronized byte[] speak(String voice, String text, double rate, int speaker) throws Exception {
        if (!available()) throw new IllegalStateException("Piper is not set up");
        List<String> vs = voices();
        if (voice == null || !vs.contains(voice)) voice = vs.get(0);
        text = text.replaceAll("[*_#`]", "").replaceAll("\\s+", " ").trim();
        if (text.length() > 800) text = text.substring(0, 800);
        double lengthScale = Math.max(0.5, Math.min(2.0, 1.0 / Math.max(0.5, rate)));
        String key = sha1(voice + "|" + lengthScale + "|" + speaker + "|" + text);
        Files.createDirectories(cache);
        Path out = cache.resolve(key + ".wav");
        if (Files.exists(out)) return Files.readAllBytes(out);
        List<String> cmd = new ArrayList<>(List.of(piper.toString(), "--model", voices.resolve(voice + ".onnx").toString(),
                "--output_file", out.toString(), "--length_scale", String.format(Locale.ROOT, "%.2f", lengthScale), "--sentence_silence", "0.25"));
        if (speaker >= 0) { cmd.add("--speaker"); cmd.add(String.valueOf(speaker)); }
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        try (Writer w = new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8)) { w.write(text); }
        p.getInputStream().readAllBytes();
        if (!p.waitFor(30, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new IllegalStateException("Piper timed out"); }
        if (!Files.exists(out)) throw new IllegalStateException("Piper produced no audio");
        return Files.readAllBytes(out);
    }

    private static String sha1(String s) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
