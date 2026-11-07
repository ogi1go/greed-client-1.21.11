package greed.client.module.misc;

import greed.client.util.ClientChat;
import greed.client.event.EventListener;
import greed.client.render.FontFace;
import greed.client.event.HudRenderEvent;
import greed.client.setting.KeybindSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;
import greed.client.util.GuiScale;
import greed.client.render.WorldProjection;
import greed.client.render.FontText;
import greed.client.ui.hud.NotificationHud;
import greed.client.render.ShapeDrawing;
import greed.client.render.ThemeColors;
import greed.client.ui.hud.HudRenderContext;
import greed.client.ui.hud.HudStyle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "Party",
   description = "Пати с друзьями: видно участников на любом расстоянии и общие метки. Управление через .party",
   category = ModuleCategory.MISC
)
public final class PartyModule extends Module {
   private static PartyModule partyModule;
   public KeybindSetting waypointKey;
   public KeybindSetting playerWaypointKey;
   private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
   private final ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "greed-party");
      thread.setDaemon(true);
      return thread;
   });
   private final String sessionId = UUID.randomUUID().toString();
   private final AtomicBoolean atomicBoolean = new AtomicBoolean();
   private final HashMap<String, Vec3d> memberPositions = new HashMap<>();
   private final HashSet<String> knownMarkIds = new HashSet<>();
   private volatile PartyModule.DataRecord2 currentParty;
   private String partyCode;
   private long lastBeatMillis;
   private long lastRenderMillis;
   private boolean waypointKeyPressed;
   private boolean playerWaypointKeyPressed;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.world != null) {
         boolean waypointPressed = this.waypointKey.getBinding().isPressed(mc.getWindow().getHandle());
         if (waypointPressed && !this.waypointKeyPressed && mc.currentScreen == null) {
            this.markLookedAtPoint();
         }

         this.waypointKeyPressed = waypointPressed;
         boolean playerWaypointPressed = this.playerWaypointKey.getBinding().isPressed(mc.getWindow().getHandle());
         if (playerWaypointPressed && !this.playerWaypointKeyPressed && mc.currentScreen == null) {
            this.markLookedAtPlayer();
         }

         this.playerWaypointKeyPressed = playerWaypointPressed;
         if (this.partyCode != null && System.currentTimeMillis() - this.lastBeatMillis >= 500L && this.atomicBoolean.compareAndSet(false, true)) {
            this.lastBeatMillis = System.currentTimeMillis();
            JsonObject jsonObject = this.getJsonObject("beat");
            jsonObject.addProperty("x", mc.player.getX());
            jsonObject.addProperty("y", mc.player.getY());
            jsonObject.addProperty("z", mc.player.getZ());
            jsonObject.addProperty("hp", mc.player.getHealth() + mc.player.getAbsorptionAmount());
            this.executorService.execute(() -> {
               try {
                  JsonObject jsonObject2 = this.getJsonObject2(jsonObject);
                  mc.execute(() -> this.handleBeatResponse(jsonObject2));
               } catch (Exception exception) {
               } finally {
                  this.atomicBoolean.set(false);
               }
            });
         }
      }
   };
   private final EventListener<HudRenderEvent> hudRenderListener = hudRenderEvent -> {
      PartyModule.DataRecord2 party = this.currentParty;
      if (party != null && mc.player != null && mc.world != null) {
         long nowMillis = System.currentTimeMillis();
         float lerpFactor = Math.min(1.0F, (float)(nowMillis - this.lastRenderMillis) / 1000.0F * 8.0F);
         this.lastRenderMillis = nowMillis;
         HudRenderContext.renderElement(hudRenderEvent.getDrawContext());
         String hostKey = this.formatText();
         String dimKey = this.formatText3();
         String playerName = mc.player.getGameProfile().name();

         for (PartyModule.DataRecord member : party.members()) {
            if (!member.nick().equals(playerName) && member.host().equals(hostKey) && member.dim().equals(dimKey)) {
               AbstractClientPlayerEntity abstractClientPlayerEntity = this.getAbstractClientPlayerEntity(member.nick());
               Vec3d vec3d = abstractClientPlayerEntity != null ? abstractClientPlayerEntity.getLerpedPos(hudRenderEvent.computeFloat()) : member.position();
               Vec3d vec3d2 = this.memberPositions.get(member.nick());
               vec3d2 = vec3d2 != null && abstractClientPlayerEntity == null ? vec3d2.lerp(vec3d, lerpFactor) : vec3d;
               this.memberPositions.put(member.nick(), vec3d2);
               String healthText = (int)Math.ceil(
                     abstractClientPlayerEntity != null ? abstractClientPlayerEntity.getHealth() + abstractClientPlayerEntity.getAbsorptionAmount() : member.health()
                  )
                  + "hp";
               this.renderMarker(hudRenderEvent, vec3d2.add(0.0, 2.3, 0.0), member.nick(), healthText, HudStyle.computeInt4(1.0F), -1.0F);
            }
         }

         this.memberPositions.keySet().removeIf(trackedNick -> party.members().stream().noneMatch(member -> member.nick().equals(trackedNick)));

         for (PartyModule.DataRecord3 mark : party.marks()) {
            if (mark.host().equals(hostKey) && mark.dim().equals(dimKey)) {
               if (mark.type().equals("player")) {
                  AbstractClientPlayerEntity abstractClientPlayerEntity2 = this.getAbstractClientPlayerEntity(mark.target());
                  Vec3d vec3d3 = abstractClientPlayerEntity2 != null ? abstractClientPlayerEntity2.getLerpedPos(hudRenderEvent.computeFloat()) : mark.position();
                  float distance = (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(vec3d3);
                  this.renderMarker(
                     hudRenderEvent,
                     vec3d3.add(0.0, 2.9, 0.0),
                     "Цель " + mark.target(),
                     mark.by(),
                     -43691,
                     Math.max(0.3F, 1.0F - distance * 0.008F)
                  );
               } else {
                  this.renderMarker(hudRenderEvent, mark.position(), "Метка", mark.by(), ThemeColors.intVal9, -1.0F);
               }
            }
         }
      }
   };

   private void sendRequest(JsonObject jsonObject, Consumer<JsonObject> onSuccess) {
      this.executorService.execute(() -> {
         try {
            JsonObject jsonObject2 = this.getJsonObject2(jsonObject);
            mc.execute(() -> {
               if (jsonObject2.get("ok").getAsBoolean()) {
                  onSuccess.accept(jsonObject2);
               } else {
                  ClientChat.processData8("&cПати: " + this.formatText2(jsonObject2.get("error").getAsString()));
               }
            });
         } catch (Exception exception) {
            mc.execute(() -> ClientChat.processData8("&cПати: сервер недоступен"));
         }
      });
   }

   private String formatText() {
      IntegratedServer server = mc.getServer();
      if (server != null) {
         return "lan:" + (server.isRemote() ? server.getServerPort() : 0);
      } else if (mc.getNetworkHandler() == null
         || !(
            mc.getNetworkHandler().getConnection().getAddress() instanceof InetSocketAddress inetSocketAddress
               && inetSocketAddress.getAddress() != null
               && (inetSocketAddress.getAddress().isLoopbackAddress() || inetSocketAddress.getAddress().isSiteLocalAddress())
         )) {
         ServerInfo currentServerEntry = mc.getCurrentServerEntry();
         return currentServerEntry == null ? "" : currentServerEntry.address.trim().toLowerCase(Locale.ROOT).replaceFirst(":25565$", "").replaceFirst("\\.$", "");
      } else {
         return "lan:" + inetSocketAddress.getPort();
      }
   }

   public void processData2(String targetNick) {
      if (this.ensureInParty()) {
         JsonObject jsonObject = this.getJsonObject("kick");
         jsonObject.addProperty("target", targetNick);
         this.sendRequest(jsonObject, jsonObject2 -> {
            this.applyPartyData(jsonObject2);
            ClientChat.processData8("&7" + targetNick + "&r исключён из пати.");
         });
      }
   }

   private void handleBeatResponse(JsonObject jsonObject) {
      if (this.partyCode != null) {
         if (jsonObject.get("ok").getAsBoolean()) {
            this.applyPartyData(jsonObject);
         } else {
            String asString = jsonObject.get("error").getAsString();
            if (asString.equals("not_member") || asString.equals("not_found")) {
               String previousPartyCode = this.partyCode;
               this.clearPartyState();
               ClientChat.processData8(
                  asString.equals("not_found") ? "Пати &7" + previousPartyCode + "&r распущено." : "Тебя больше нет в пати &7" + previousPartyCode + "&r."
               );
            }
         }
      }
   }

   private void clearPartyState() {
      this.partyCode = null;
      this.currentParty = null;
      this.memberPositions.clear();
      this.knownMarkIds.clear();
   }

   private JsonObject getJsonObject(String action) {
      JsonObject jsonObject = this.getJsonObject3(action);
      jsonObject.addProperty("code", this.partyCode);
      jsonObject.addProperty("host", this.formatText());
      jsonObject.addProperty("dim", this.formatText3());
      return jsonObject;
   }

   public void updateState2() {
      if (this.partyCode == null) {
         ClientChat.processData8("Ты не в пати.");
      } else {
         JsonObject jsonObject = this.getJsonObject("leave");
         this.clearPartyState();
         this.sendRequest(jsonObject, jsonObject2 -> ClientChat.processData8("Ты вышел из пати."));
      }
   }

   public void processData4(String code) {
      if (mc.player != null) {
         if (!this.isEnabled()) {
            this.setEnabled(true);
         }

         if (this.partyCode != null) {
            this.updateState2();
         }

         JsonObject jsonObject = this.getJsonObject3("join");
         jsonObject.addProperty("code", code.toUpperCase(Locale.ROOT));
         this.sendRequest(jsonObject, jsonObject2 -> {
            this.applyPartyData(jsonObject2);
            ClientChat.processData8("Ты в пати &a" + this.partyCode + "&r, владелец: &7" + jsonObject2.get("owner").getAsString());
         });
      }
   }

   private void markLookedAtPoint() {
      if (this.ensureInParty()) {
         HitResult hitResult = mc.player.raycast(256.0, 1.0F, false);
         if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == Type.BLOCK) {
            Vec3d vec3d = blockHitResult.getBlockPos().toCenterPos().add(0.0, 0.5, 0.0);
            this.sendMark("point", "", vec3d);
         } else {
            NotificationHud.processData3(this.category.getIcon(), "Метка", "Слишком далеко", true);
         }
      }
   }

   private void renderMarker(HudRenderEvent hudRenderEvent, Vec3d vec3d, String name, String suffix, int color, float forcedAlpha) {
      Vec3d vec3d2 = WorldProjection.getVec3d2(vec3d, hudRenderEvent.computeInt2(), hudRenderEvent.computeInt());
      if (vec3d2 != null) {
         float uiScale = GuiScale.computeInt() / GuiScale.computeFloat();
         float screenX = (float)vec3d2.x * uiScale;
         float screenY = (float)vec3d2.y * uiScale;
         float distance = (float)mc.player.getEntityPos().distanceTo(vec3d);
         String distanceText = String.format(Locale.US, "%.0fм", distance);
         float nameWidth = FontText.computeFloat(FontFace.SEMI_BOLD, name, 6.0F);
         float suffixWidth = FontText.computeFloat(FontFace.SEMI_BOLD, suffix, 6.0F);
         float distanceWidth = FontText.computeFloat(FontFace.SEMI_BOLD, distanceText, 6.0F);
         float totalWidth = 4.0F + nameWidth + 3.0F + suffixWidth + 3.0F + distanceWidth + 4.0F;
         float boxX = Math.round((screenX - totalWidth / 2.0F) * 2.0F) / 2.0F;
         float boxY = Math.round((screenY - 6.5F) * 2.0F) / 2.0F;
         HudRenderContext.processData3(screenX, screenY, forcedAlpha > 0.0F ? forcedAlpha : (float)Math.max(0.6, 1.1 - distance / 200.0));
         HudStyle.processData2(boxX, boxY, totalWidth, 13.0F, 1.0F, 4.5F, ShapeDrawing.TypeOption.LOW);
         float textY = boxY + 6.5F - 3.5F;
         FontText.processData3(FontFace.SEMI_BOLD, name, boxX + 4.0F, textY, 6.0F, color);
         FontText.processData3(
            FontFace.SEMI_BOLD, suffix, boxX + 4.0F + nameWidth + 3.0F, textY, 6.0F, HudStyle.computeInt2(1.0F)
         );
         FontText.processData3(
            FontFace.SEMI_BOLD,
            distanceText,
            boxX + 4.0F + nameWidth + 3.0F + suffixWidth + 3.0F,
            textY,
            6.0F,
            HudStyle.computeInt2(1.0F)
         );
         HudRenderContext.updateState();
      }
   }

   public void processData6(boolean locked) {
      if (this.ensureInParty()) {
         JsonObject jsonObject = this.getJsonObject("lock");
         jsonObject.addProperty("locked", locked);
         this.sendRequest(jsonObject, jsonObject2 -> {
            this.applyPartyData(jsonObject2);
            ClientChat.processData8(locked ? "Пати &cзакрыто&r, новые игроки не зайдут." : "Пати &aоткрыто&r для входа.");
         });
      }
   }

   private void sendMark(String markType, String targetNick, Vec3d vec3d) {
      JsonObject jsonObject = this.getJsonObject("mark");
      jsonObject.addProperty("type", markType);
      jsonObject.addProperty("target", targetNick);
      jsonObject.addProperty("x", vec3d.x);
      jsonObject.addProperty("y", vec3d.y);
      jsonObject.addProperty("z", vec3d.z);
      this.sendRequest(
         jsonObject,
         jsonObject2 -> {
            this.applyPartyData(jsonObject2);
            NotificationHud.processData3(
               this.category.getIcon(), "Метка", markType.equals("player") ? "Отмечен " + targetNick : "Метка поставлена", true
            );
         }
      );
   }

   private void applyPartyData(JsonObject jsonObject) {
      PartyModule.DataRecord2 previousParty = this.currentParty;
      ArrayList<PartyModule.DataRecord> members = new ArrayList<>();

      for (JsonElement jsonElement : jsonObject.getAsJsonArray("members")) {
         JsonObject asJsonObject = jsonElement.getAsJsonObject();
         members.add(
            new PartyModule.DataRecord(
               asJsonObject.get("nick").getAsString(),
               asJsonObject.get("host").getAsString(),
               asJsonObject.get("dim").getAsString(),
               new Vec3d(asJsonObject.get("x").getAsDouble(), asJsonObject.get("y").getAsDouble(), asJsonObject.get("z").getAsDouble()),
               asJsonObject.get("hp").getAsFloat()
            )
         );
      }

      ArrayList<PartyModule.DataRecord3> marks = new ArrayList<>();

      for (JsonElement jsonElement2 : jsonObject.getAsJsonArray("marks")) {
         JsonObject asJsonObject2 = jsonElement2.getAsJsonObject();
         marks.add(
            new PartyModule.DataRecord3(
               asJsonObject2.get("id").getAsString(),
               asJsonObject2.get("by").getAsString(),
               asJsonObject2.get("type").getAsString(),
               asJsonObject2.get("target").getAsString(),
               asJsonObject2.get("host").getAsString(),
               asJsonObject2.get("dim").getAsString(),
               new Vec3d(asJsonObject2.get("x").getAsDouble(), asJsonObject2.get("y").getAsDouble(), asJsonObject2.get("z").getAsDouble())
            )
         );
      }

      PartyModule.DataRecord2 party = new PartyModule.DataRecord2(
         jsonObject.get("code").getAsString(), jsonObject.get("owner").getAsString(), jsonObject.get("locked").getAsBoolean(), members, marks
      );
      this.partyCode = party.code();
      this.currentParty = party;
      if (previousParty != null) {
         String localPlayerName = mc.player == null ? "" : mc.player.getGameProfile().name();

         for (PartyModule.DataRecord member : members) {
            if (previousParty.members().stream().noneMatch(previousMember -> previousMember.nick().equals(member.nick()))) {
               ClientChat.processData8("&a+ &7" + member.nick() + "&r зашёл в пати.");
            }
         }

         for (PartyModule.DataRecord member : previousParty.members()) {
            if (members.stream().noneMatch(currentMember -> currentMember.nick().equals(member.nick()))) {
               ClientChat.processData8("&c- &7" + member.nick() + "&r вышел из пати.");
            }
         }

         for (PartyModule.DataRecord3 mark : marks) {
            if (this.knownMarkIds.add(mark.id()) && !mark.by().equals(localPlayerName)) {
               NotificationHud.processData3(
                  this.category.getIcon(),
                  mark.by(),
                  mark.type().equals("player") ? "отметил " + mark.target() : "поставил метку",
                  true
               );
            }
         }

         this.knownMarkIds.removeIf(knownMarkId -> marks.stream().noneMatch(mark -> mark.id().equals(knownMarkId)));
      } else {
         for (PartyModule.DataRecord3 mark : marks) {
            this.knownMarkIds.add(mark.id());
         }
      }
   }

   public void updateState4() {
      PartyModule.DataRecord2 party = this.currentParty;
      if (party == null) {
         ClientChat.processData8("Ты не в пати. &7.party create&r или &7.party join <код>");
      } else {
         StringBuilder stringBuilder = new StringBuilder();

         for (PartyModule.DataRecord member : party.members()) {
            stringBuilder.append(stringBuilder.isEmpty() ? "" : "&r, ")
               .append(member.nick().equals(party.owner()) ? "&e" : "&7")
               .append(member.nick());
         }

         ClientChat.processData8("Пати &a" + party.code() + "&r " + (party.locked() ? "&c(закрыто)" : "&a(открыто)") + "&r: " + stringBuilder);
      }
   }

   private String formatText2(String errorCode) {
      return switch (errorCode) {
         case "not_found" -> "пати с таким кодом нет";
         case "locked" -> "пати закрыто";
         case "full" -> "пати заполнено";
         case "not_owner" -> "это может только создатель пати";
         case "not_member" -> "ты не в этом пати";
         default -> errorCode;
      };
   }

   private AbstractClientPlayerEntity getAbstractClientPlayerEntity(String playerName) {
      for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
         if (abstractClientPlayerEntity.getGameProfile().name().equalsIgnoreCase(playerName)) {
            return abstractClientPlayerEntity;
         }
      }

      return null;
   }

   public static PartyModule getInstance() {
      if (partyModule == null) {
         partyModule = new PartyModule();
      }

      return partyModule;
   }

   private JsonObject getJsonObject2(JsonObject jsonObject) throws Exception {
      HttpResponse<String> response = this.httpClient
         .send(
            HttpRequest.newBuilder(URI.create("http://greedgaymarket.fun/party/party.php"))
               .timeout(Duration.ofSeconds(6L))
               .header("Content-Type", "application/json")
               .header("X-Greed-Key", "dlx_7Qm2vX9pK4tR8wZ1nB6s")
               .POST(BodyPublishers.ofString(jsonObject.toString(), StandardCharsets.UTF_8))
               .build(),
            BodyHandlers.ofString(StandardCharsets.UTF_8)
         );
      return JsonParser.parseString(response.body()).getAsJsonObject();
   }

   private JsonObject getJsonObject3(String action) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("action", action);
      jsonObject.addProperty("nick", mc.player == null ? "" : mc.player.getGameProfile().name());
      jsonObject.addProperty("session", this.sessionId);
      return jsonObject;
   }

   private boolean ensureInParty() {
      if (this.partyCode != null) {
         return true;
      }

      ClientChat.processData8("Ты не в пати. &7.party create&r или &7.party join <код>");
      return false;
   }

   private String formatText3() {
      return mc.world == null ? "" : mc.world.getRegistryKey().getValue().getPath();
   }

   private void markLookedAtPlayer() {
      if (this.ensureInParty()) {
         AbstractClientPlayerEntity abstractClientPlayerEntity = null;
         double bestAngle = 4.0;
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d rotationVec = mc.player.getRotationVec(1.0F);

         for (AbstractClientPlayerEntity abstractClientPlayerEntity2 : mc.world.getPlayers()) {
            if (abstractClientPlayerEntity2 != mc.player) {
               Vec3d vec3d = abstractClientPlayerEntity2.getBoundingBox().getCenter().subtract(eyePos);
               double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, vec3d.normalize().dotProduct(rotationVec)))));
               if (angle < bestAngle) {
                  bestAngle = angle;
                  abstractClientPlayerEntity = abstractClientPlayerEntity2;
               }
            }
         }

         if (abstractClientPlayerEntity == null) {
            NotificationHud.processData3(this.category.getIcon(), "Метка", "Наведись на игрока", true);
         } else {
            this.sendMark("player", abstractClientPlayerEntity.getGameProfile().name(), abstractClientPlayerEntity.getEntityPos());
         }
      }
   }

   public void updateState6() {
      if (mc.player != null) {
         if (!this.isEnabled()) {
            this.setEnabled(true);
         }

         if (this.partyCode != null) {
            this.updateState2();
         }

         JsonObject jsonObject = this.getJsonObject3("create");
         this.sendRequest(jsonObject, jsonObject2 -> {
            this.applyPartyData(jsonObject2);
            ClientChat.processData8("Пати создано, код: &a" + this.partyCode + "&r. Друзья заходят через &7.party join " + this.partyCode);
         });
      }
   }

   public boolean matchesCriteria(String playerName) {
      PartyModule.DataRecord2 party = this.currentParty;
      if (party != null && playerName != null) {
         for (PartyModule.DataRecord member : party.members()) {
            if (member.nick().equalsIgnoreCase(playerName)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private PartyModule() {
      this.waypointKey = this.addSetting(new KeybindSetting("Метка", "Ставит метку туда, куда смотришь. Видят все в пати."));
      this.playerWaypointKey = this.addSetting(new KeybindSetting("Метка на игрока", "Отмечает игрока, на которого смотришь. Видят все в пати."));
   }

   @Override
   protected void onDisable() {
      if (this.partyCode != null) {
         this.updateState2();
      }
   }

   private record DataRecord(String nick, String host, String dim, Vec3d position, float health) {
   }

   private record DataRecord2(String code, String owner, boolean locked, ArrayList<PartyModule.DataRecord> members, ArrayList<PartyModule.DataRecord3> marks) {
   }

   private record DataRecord3(String id, String by, String type, String target, String host, String dim, Vec3d position) {
   }
}
