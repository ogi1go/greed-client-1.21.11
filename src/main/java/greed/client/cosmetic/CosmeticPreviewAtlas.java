package greed.client.cosmetic;

import greed.client.GreedClient;
import greed.client.cosmetic.CosmeticWarmup;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class CosmeticPreviewAtlas {
   private static final Path path3 = FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("cosmetics-cache");
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6L)).followRedirects(Redirect.NORMAL).build();
   private static final Set<Identifier> set = ConcurrentHashMap.newKeySet();
   private static final Set<Identifier> set2 = ConcurrentHashMap.newKeySet();
   private static final Map<Integer, CosmeticPreviewAtlas.AtlasEntry> map = new ConcurrentHashMap<>();
   private static Map<String, String> map2;
   private static long timeMillis;

   public static boolean matchesCriteria(Identifier identifier, String local5942_1) {
      boolean local5942_2 = matchesCriteria2(identifier, local5942_1, null);

      try {
         Thread.sleep(120L);
      } catch (InterruptedException exception) {
         Thread.currentThread().interrupt();
      }

      return local5942_2;
   }

   public static boolean checkState() {
      try {
         JsonObject asJsonObject = JsonParser.parseString(new String(getByte("previews/atlas/index.json"), StandardCharsets.UTF_8)).getAsJsonObject();
         float asFloat = asJsonObject.get("cell").getAsFloat();

         for (Entry<String, JsonElement> local5943_3 : asJsonObject.getAsJsonObject("atlases").entrySet()) {
            JsonObject asJsonObject2 = local5943_3.getValue().getAsJsonObject();
            String asString = asJsonObject2.get("file").getAsString();
            float asFloat2 = asJsonObject2.get("width").getAsFloat();
            float asFloat3 = asJsonObject2.get("height").getAsFloat();
            Identifier identifier = Identifier.of("greed", "cosmetics/" + asString);

            for (Entry<String, JsonElement> local5943_10 : asJsonObject2.getAsJsonObject("cells").entrySet()) {
               float asFloat4 = local5943_10.getValue().getAsJsonArray().get(0).getAsFloat();
               float asFloat5 = local5943_10.getValue().getAsJsonArray().get(1).getAsFloat();
               map.put(
                  Integer.parseInt(local5943_10.getKey()),
                  new CosmeticPreviewAtlas.AtlasEntry(
                     identifier,
                     identifier.toString(),
                     (asFloat4 + 0.5F) / asFloat2,
                     (asFloat5 + 0.5F) / asFloat3,
                     (asFloat4 + asFloat - 0.5F) / asFloat2,
                     (asFloat5 + asFloat - 0.5F) / asFloat3
                  )
               );
            }

            CosmeticWarmup.processData2(identifier, asString);
         }

         return true;
      } catch (Exception exception) {
         GreedClient.logger().warn("[Cosmetics] preview atlases not loaded: {}", exception.toString());
         return false;
      }
   }

   private static Map<String, String> getMap() {
      if (map2 != null) {
         return map2;
      }

      if (System.currentTimeMillis() - timeMillis < 30000L) {
         return null;
      }

      try {
         HttpResponse<String> local5944_0 = HTTP.send(
            HttpRequest.newBuilder(URI.create("http://greedgaymarket.fun/cosmetics/manifest.php")).timeout(Duration.ofSeconds(8L)).GET().build(),
            BodyHandlers.ofString(StandardCharsets.UTF_8)
         );
         if (local5944_0.statusCode() != 200) {
            throw new IOException("HTTP " + local5944_0.statusCode());
         }

         JsonObject asJsonObject = JsonParser.parseString(local5944_0.body()).getAsJsonObject().getAsJsonObject("files");
         HashMap<String, String> local5944_2 = new HashMap<>();

         for (Entry<String, JsonElement> local5944_4 : asJsonObject.entrySet()) {
            local5944_2.put(local5944_4.getKey(), local5944_4.getValue().getAsString());
         }

         if (!local5944_2.isEmpty() && Files.isDirectory(path3)) {
            try (Stream<Path> local5944_3 = Files.walk(path3)) {
               for (Path path : local5944_3.sorted(Comparator.reverseOrder()).toList()) {
                  if (!path.equals(path3) && !local5944_2.containsKey(path3.relativize(path).toString().replace('\\', '/'))) {
                     try {
                        Files.delete(path);
                     } catch (IOException exception) {
                     }
                  }
               }
            } catch (IOException exception2) {
            }
         }

         map2 = local5944_2;
         return map2;
      } catch (Exception exception3) {
         if (exception3 instanceof InterruptedException) {
            Thread.currentThread().interrupt();
         }

         timeMillis = System.currentTimeMillis();
         GreedClient.logger().warn("[Cosmetics] manifest not loaded: {}", exception3.toString());
         return null;
      }
   }

   public static CosmeticPreviewAtlas.AtlasEntry getNested0499(CosmeticEntry cosmeticEntry) {
      if (cosmeticEntry == null) {
         return null;
      }

      CosmeticPreviewAtlas.AtlasEntry local5945_1 = map.get(cosmeticEntry.id());
      if (local5945_1 != null && set.contains(local5945_1.atlasId())) {
         return local5945_1;
      }

      Identifier previewId = cosmeticEntry.previewId();
      if (previewId != null && !set.contains(previewId)) {
         CosmeticWarmup.processData2(previewId, cosmeticEntry.previewResource());
      } else if (previewId != null) {
         return new CosmeticPreviewAtlas.AtlasEntry(previewId, previewId.toString(), 0.0F, 0.0F, 1.0F, 1.0F);
      }

      CosmeticWarmup.updateState2();
      return null;
   }

   public static synchronized byte[] getByte(String local5947_0) throws IOException {
      Path path = path3.resolve(local5947_0);
      if (Files.isRegularFile(path)) {
         return Files.readAllBytes(path);
      }

      byte[] bundled = getBundled(local5947_0);
      if (bundled != null) {
         return bundled;
      }

      Map<String, String> local5947_2 = getMap();
      if (local5947_2 == null) {
         throw new IOException("hosting unreachable and no cache for " + local5947_0);
      }

      String local5947_3 = local5947_2.get(local5947_0);
      if (local5947_3 == null) {
         throw new FileNotFoundException(local5947_0 + " missing on hosting");
      }

      HttpResponse<byte[]> local5947_4;
      try {
         local5947_4 = HTTP.send(
            HttpRequest.newBuilder(URI.create("http://greedgaymarket.fun/cosmetics/" + local5947_0 + "?v=" + local5947_3))
               .timeout(Duration.ofSeconds(20L))
               .GET()
               .build(),
            BodyHandlers.ofByteArray()
         );
      } catch (InterruptedException exception) {
         Thread.currentThread().interrupt();
         throw new IOException(exception);
      }

      if (local5947_4.statusCode() != 200) {
         throw new IOException("HTTP " + local5947_4.statusCode() + " for " + local5947_0);
      }

      byte[] byteArray2 = local5947_4.body();
      if (!formatText(byteArray2).equals(local5947_3)) {
         throw new IOException("broken download " + local5947_0);
      }

      Files.createDirectories(path.getParent());
      Path path2 = path.resolveSibling(path.getFileName() + ".part");
      Files.write(path2, byteArray2);
      Files.move(path2, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      return byteArray2;
   }

   private static byte[] getBundled(String local5947_0) {
      String[] candidates = local5947_0.startsWith("models/")
         ? new String[]{local5947_0, local5947_0.substring("models/".length())}
         : new String[]{local5947_0};

      for (String candidate : candidates) {
         try (InputStream inputStream = CosmeticPreviewAtlas.class.getResourceAsStream("/assets/greed/cosmetics/" + candidate)) {
            if (inputStream != null) {
               return inputStream.readAllBytes();
            }
         } catch (IOException exception) {
         }
      }

      return null;
   }

   public static boolean matchesCriteria2(Identifier identifier, String local5948_1, Runnable runnable) {
      if (set2.contains(identifier)) {
         if (runnable != null) {
            MinecraftClient.getInstance().execute(runnable);
         }

         return true;
      } else {
         try {
            NativeImage nativeImage = NativeImage.read(getByte(local5948_1));
            set2.add(identifier);
            MinecraftClient.getInstance().execute(() -> {
               NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(identifier::toString, nativeImage);
               nativeImageBackedTexture.setImage(null);
               MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, nativeImageBackedTexture);
               set.add(identifier);
               if (runnable != null) {
                  runnable.run();
               }
            });
            return true;
         } catch (Exception exception) {
            GreedClient.logger().warn("[Cosmetics] texture {} not loaded: {}", local5948_1, exception.toString());
            return false;
         }
      }
   }

   private static String formatText(byte[] byteArray) {
      try {
         return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(byteArray));
      } catch (Exception exception) {
         throw new IllegalStateException(exception);
      }
   }

   private CosmeticPreviewAtlas() {
   }

   public record AtlasEntry(Identifier atlasId, String atlasTexture, float u0, float v0, float u1, float v1) {
   }
}
