package greed.client.util;

import greed.client.ui.UiTheme;

import greed.client.event.EventListener;
import greed.client.MinecraftAccess;
import greed.client.event.PlayerTickEvent;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import ru.dreamix.protection.api.impl.profile.objects.RoleObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.SuggestCommand;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.text.StyleSpriteSource.Font;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class IrcClient implements MinecraftAccess {
   private static IrcClient ircClient;
   private static final Style style = Style.EMPTY.withFont(new Font(Identifier.of("greed", "irc"))).withColor(16777215).withoutShadow();
   private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
   private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
   private Path path;
   private boolean flag;
   private volatile boolean suspended;
   private volatile boolean flag2;
   private long timeMillis;
   private HashMap<String, String> map = new HashMap<>();
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (this.flag && !this.suspended && !this.flag2 && System.currentTimeMillis() - this.timeMillis >= 2000L) {
         this.flag2 = true;
         this.timeMillis = System.currentTimeMillis();
         this.getCompletableFuture(this.getJsonObject("poll")).whenComplete((jsonObject, throwable) -> {
            this.flag2 = false;
            if (jsonObject != null && jsonObject.has("messages")) {
               for (JsonElement jsonElement : jsonObject.getAsJsonArray("messages")) {
                  mc.execute(() -> this.processData3(jsonElement.getAsJsonObject()));
               }
            }
         });
      }
   };

   private JsonObject getJsonObject(String local4430_1) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("action", local4430_1);
      jsonObject.addProperty("name", ProfileAPI.get().getModel().getName());
      jsonObject.addProperty("role", ProfileAPI.get().getModel().getRole().name());
      jsonObject.addProperty("nick", mc.getSession().getUsername());
      return jsonObject;
   }

   public void processData(String local4431_1) {
      if (this.suspended) {
         return;
      }

      if (!local4431_1.isBlank()) {
         JsonObject jsonObject = this.getJsonObject("send");
         Matcher matcher = Pattern.compile("^([\\p{L}\\p{N}_.\\-]{1,24}),\\s*(.+)$").matcher(local4431_1);
         boolean local4431_4 = matcher.matches() && this.map.containsKey(matcher.group(1).toLowerCase(Locale.ROOT));
         jsonObject.addProperty("text", local4431_4 ? matcher.group(2) : local4431_1);
         if (local4431_4) {
            jsonObject.addProperty("replyTo", matcher.group(1));
            jsonObject.addProperty("replyToNick", this.map.get(matcher.group(1).toLowerCase(Locale.ROOT)));
         }

         this.getCompletableFuture(jsonObject).thenAccept(jsonObject2 -> {
            if (jsonObject2 == null) {
               ClientChat.processData8("&cIRC сейчас недоступен, попробуй позже.");
            } else if (!jsonObject2.get("ok").getAsBoolean()) {
               ClientChat.processData8("&c" + (jsonObject2.has("message") ? jsonObject2.get("message").getAsString() : "Сообщение не отправлено."));
            } else {
               this.timeMillis = 0L;
            }
         });
      }
   }

   private int computeInt(RoleObject roleObject) {
      return switch (roleObject.name()) {
         case "BETA" -> 11832575;
         case "YOUTUBE" -> 16734826;
         case "ADMIN" -> 16757575;
         default -> 13226457;
      };
   }

   private RoleObject getRoleObject(String local4434_1) {
      try {
         return RoleObject.valueOf(local4434_1);
      } catch (Exception exception) {
         return RoleObject.DEFAULT;
      }
   }

   public void updateState() {
      this.getCompletableFuture(this.getJsonObject("online"))
         .thenAccept(
            jsonObject -> {
               if (jsonObject != null && jsonObject.has("online")) {
                  MutableText text = Text.literal("В IRC сейчас " + jsonObject.getAsJsonArray("online").size() + ": ");
                  boolean local4439_3 = true;

                  for (JsonElement jsonElement : jsonObject.getAsJsonArray("online")) {
                     if (!local4439_3) {
                        text.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
                     }

                     local4439_3 = false;
                     RoleObject roleObject = this.getRoleObject(jsonElement.getAsJsonObject().get("role").getAsString());
                     text.append(Text.literal(String.valueOf((char)(57360 + roleObject.ordinal()))).setStyle(style)).append(" ");
                     text.append(
                        Text.literal(jsonElement.getAsJsonObject().get("name").getAsString()).setStyle(Style.EMPTY.withColor(this.computeInt(roleObject)))
                     );
                  }

                  ClientChat.info(text);
               } else {
                  ClientChat.processData8("&cIRC сейчас недоступен, попробуй позже.");
               }
            }
         );
   }

   public boolean checkState() {
      return this.flag;
   }

   private CompletableFuture<JsonObject> getCompletableFuture(JsonObject jsonObject) {
      return this.httpClient
         .sendAsync(
            HttpRequest.newBuilder(URI.create("http://greedgaymarket.fun/irc/irc.php"))
               .timeout(Duration.ofSeconds(6L))
               .header("Content-Type", "application/json")
               .header("X-Greed-Key", "dlx_7Qm2vX9pK4tR8wZ1nB6s")
               .POST(BodyPublishers.ofString(jsonObject.toString(), StandardCharsets.UTF_8))
               .build(),
            BodyHandlers.ofString(StandardCharsets.UTF_8)
         )
         .thenApply(local4436_0 -> local4436_0.statusCode() == 200 ? JsonParser.parseString(local4436_0.body()).getAsJsonObject() : null)
         .exceptionally(throwable -> null);
   }

   public void setSuspended(boolean suspended) {
      this.suspended = suspended;
   }

   public void processData2(boolean local4440_1) {
      this.flag = local4440_1;
      this.timeMillis = 0L;
      if (!local4440_1) {
         this.getCompletableFuture(this.getJsonObject("leave"));
      }

      try {
         Files.createDirectories(this.path.getParent());
         JsonObject jsonObject = new JsonObject();
         jsonObject.addProperty("enabled", local4440_1);
         Files.writeString(this.path, jsonObject.toString(), StandardCharsets.UTF_8);
      } catch (Exception exception) {
      }
   }

   private IrcClient() {
      this.path = FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("irc.json");

      try {
         if (Files.exists(this.path)) {
            this.flag = JsonParser.parseString(Files.readString(this.path, StandardCharsets.UTF_8)).getAsJsonObject().get("enabled").getAsBoolean();
         }
      } catch (Exception exception) {
      }
   }

   private void processData3(JsonObject jsonObject) {
      if (this.suspended) {
         return;
      }

      String asString = jsonObject.get("name").getAsString();
      String asString2 = jsonObject.get("text").getAsString();
      String asString3 = jsonObject.get("nick").getAsString();
      String name = ProfileAPI.get().getModel().getName();
      String username = mc.getSession().getUsername();
      String local4444_7 = jsonObject.has("replyTo") ? jsonObject.get("replyTo").getAsString() : "";
      String local4444_8 = jsonObject.has("replyToNick") ? jsonObject.get("replyToNick").getAsString() : "";
      RoleObject roleObject = this.getRoleObject(jsonObject.get("role").getAsString());
      boolean local4444_10 = asString.equalsIgnoreCase(name) && asString3.equalsIgnoreCase(username);
      boolean local4444_11 = !local4444_10 && local4444_7.equalsIgnoreCase(name) && (local4444_8.isEmpty() || local4444_8.equalsIgnoreCase(username));
      boolean local4444_12 = local4444_11 || !local4444_10 && asString2.toLowerCase(Locale.ROOT).contains("@" + name.toLowerCase(Locale.ROOT));
      if (!local4444_10) {
         this.map.put(asString.toLowerCase(Locale.ROOT), asString3);
      }

      Text text = Text.literal(roleObject.toString())
         .setStyle(Style.EMPTY.withColor(this.computeInt(roleObject)))
         .append(
            Text.literal("\nНик в игре: " + (jsonObject.get("nick").getAsString().isEmpty() ? "—" : jsonObject.get("nick").getAsString()))
               .formatted(Formatting.GRAY)
         )
         .append(Text.literal("\nОтправлено: " + dateTimeFormatter.format(Instant.ofEpochSecond(jsonObject.get("time").getAsLong()))).formatted(Formatting.GRAY));
      ClickEvent clickEvent = new SuggestCommand("@" + asString + ", ");
      MutableText text2 = Text.empty();
      text2.append(Text.literal("\ue000").setStyle(style.withColor(local4444_11 ? 16763709 : UiTheme.intVal7 & 16777215)));
      text2.append(Text.literal(" "));
      text2.append(Text.literal(String.valueOf((char)(57600 + Math.floorMod(jsonObject.get("emoji").getAsInt(), 20)))).setStyle(style));
      text2.append(Text.literal(" "));
      HoverEvent hoverEvent = ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.ADMIN) ? new ShowText(text) : null;
      text2.append(Text.literal(String.valueOf((char)(57360 + roleObject.ordinal()))).setStyle(style.withHoverEvent(hoverEvent)));
      text2.append(Text.literal(" "));
      text2.append(
         Text.literal(asString).setStyle(Style.EMPTY.withColor(this.computeInt(roleObject)).withHoverEvent(hoverEvent).withClickEvent(clickEvent))
      );
      if (local4444_11) {
         text2.append(Text.literal(" "))
            .append(Text.literal("\ue014").setStyle(style.withHoverEvent(new ShowText(Text.literal(asString + " ответил тебе")))));
      } else if (!local4444_7.isEmpty()) {
         text2.append(Text.literal(" → ").formatted(Formatting.DARK_GRAY)).append(Text.literal(local4444_7).setStyle(Style.EMPTY.withColor(10134445)));
      }

      text2.append(Text.literal(" » ").formatted(Formatting.DARK_GRAY));
      text2.append(Text.literal(asString2).setStyle(Style.EMPTY.withColor(local4444_11 ? 16765788 : (local4444_12 ? 16769902 : 15593459))));
      text2.append(Text.literal(" "));
      text2.append(
         Text.literal("\ue001")
            .setStyle(style.withColor(9279397).withHoverEvent(new ShowText(Text.literal("Ответить " + asString))).withClickEvent(clickEvent))
      );
      ClientChat.processData4(text2);
      if (local4444_12) {
         SoundPlayer.processData("irc_mention");
         if (local4444_11) {
            mc.inGameHud
               .setOverlayMessage(
                  Text.empty()
                     .append(Text.literal("\ue014").setStyle(style))
                     .append(Text.literal(" " + asString).setStyle(Style.EMPTY.withColor(this.computeInt(roleObject))))
                     .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY))
                     .append(
                        Text.literal(asString2.length() > 60 ? asString2.substring(0, 60) + "…" : asString2).setStyle(Style.EMPTY.withColor(16765788))
                     ),
                  false
               );
         }
      }
   }

   public static IrcClient getInstance() {
      if (ircClient == null) {
         ircClient = new IrcClient();
      }

      return ircClient;
   }
}
