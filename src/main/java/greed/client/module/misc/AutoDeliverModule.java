package greed.client.module.misc;

import greed.client.util.ClientChat;
import greed.client.event.EventListener;
import greed.client.setting.LegacyNumberSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.setting.NumericTextSetting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(name = "AutoDeliver", description = "Автоматическая выдача валюты по заказам FunPay.", category = ModuleCategory.MISC)
public final class AutoDeliverModule extends Module {
   private static AutoDeliverModule autoDeliverModule;
   public NumericTextSetting goldenKey;
   public NumericTextSetting pricePer1M;
   public LegacyNumberSetting legacyNumberSetting;
   public NumericTextSetting anarchyDelivery;
   private static final Pattern pattern = Pattern.compile("data-app-data=\"([^\"]*)\"");
   private static final Pattern pattern2 = Pattern.compile("<form[^>]*action=\"([^\"]*)\"");
   private static final Pattern pattern3 = Pattern.compile("<input[^>]*>");
   private static final Pattern pattern4 = Pattern.compile("(\\d+)\\s*шт\\.");
   private static final Pattern pattern5 = Pattern.compile("Анархия-(\\d+)");
   private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).followRedirects(Redirect.NORMAL).build();
   private ScheduledExecutorService scheduledExecutorService;
   private String userId;
   private String csrfToken;
   private final HashMap<String, String> cookieMap = new HashMap<>();
   private String chatTag = Integer.toHexString((int)(System.nanoTime() & 2147483647L));
   private ArrayList<AutoDeliverModule.DeliveryWorker> workers;
   private ArrayList<AutoDeliverModule.DeliveryOrder> orders;
   private final ArrayList<String> processedOrderIds = new ArrayList<>();
   private final ConcurrentLinkedQueue<AutoDeliverModule.DeliveryConfig> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
   private long lastActionTimeMillis;
   private long lastAnarchyCommandTimeMillis;
   private boolean idleAtHub;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.world != null && System.currentTimeMillis() - this.lastActionTimeMillis >= 300L) {
         AutoDeliverModule.DeliveryConfig activeConfig = this.concurrentLinkedQueue.peek();
         if (activeConfig != null) {
            this.idleAtHub = false;
            if (activeConfig.fundsReceived) {
               if (mc.currentScreen != null
                  && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler
                  && mc.currentScreen.getTitle().getString().contains("Кикнуть")) {
                  Inventory inventory = genericContainerScreenHandler.getInventory();

                  for (int index = 0; index < inventory.size(); index++) {
                     if (inventory.getStack(index).getName().getString().contains("Подтвердить")) {
                        InventoryUtils.sendSlotClickPacket(index, 0, SlotActionType.PICKUP, true);
                        mc.player.closeHandledScreen();
                        this.concurrentLinkedQueue.poll();
                        this.lastActionTimeMillis = System.currentTimeMillis();
                        if (this.scheduledExecutorService != null) {
                           this.scheduledExecutorService
                              .execute(
                                 () -> this.sendChatMessage(
                                    activeConfig.orderChatNodeId,
                                    "✅ Готово, валюта у вас — приятной игры!\n\n\ud83d\ude4f Подтвердите заказ и оставьте отзыв, мне это очень поможет:\n\ud83d\udd17 https://funpay.com/orders/"
                                       + (activeConfig.orderId.startsWith("#") ? activeConfig.orderId.substring(1) : activeConfig.orderId)
                                       + "/\n\n\ud83d\udc9b Спасибо за покупку!"
                                 )
                              );
                        }

                        return;
                     }
                  }
               }

               if (System.currentTimeMillis() - activeConfig.lastKickTimeMillis >= 1500L) {
                  activeConfig.lastKickTimeMillis = System.currentTimeMillis();
                  this.lastActionTimeMillis = System.currentTimeMillis();
                  mc.player.networkHandler.sendChatCommand("clan kick " + activeConfig.playerName);
               }
            } else if (this.getCurrentAnarchy() != this.getConfiguredAnarchy()) {
               if (System.currentTimeMillis() - this.lastAnarchyCommandTimeMillis >= 5000L) {
                  this.lastAnarchyCommandTimeMillis = System.currentTimeMillis();
                  this.lastActionTimeMillis = System.currentTimeMillis();
                  mc.player.networkHandler.sendChatCommand(this.anarchyDelivery.getText().replace("/", ""));
               }
            } else if (!activeConfig.joinedClan) {
               if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(activeConfig.playerName) != null) {
                  if (System.currentTimeMillis() - activeConfig.lastInviteTimeMillis >= 2500L) {
                     activeConfig.lastInviteTimeMillis = System.currentTimeMillis();
                     this.lastActionTimeMillis = System.currentTimeMillis();
                     mc.player.networkHandler.sendChatCommand("clan invite " + activeConfig.playerName);
                  }
               }
            } else if (!activeConfig.invested) {
               activeConfig.invested = true;
               this.lastActionTimeMillis = System.currentTimeMillis();
               mc.player.networkHandler.sendChatCommand("clan invest " + activeConfig.units * 1000000L);
            } else if (activeConfig.rightsGranted) {
               if (!activeConfig.rightsMessageSent) {
                  activeConfig.rightsMessageSent = true;
                  if (this.scheduledExecutorService != null) {
                     this.scheduledExecutorService
                        .execute(
                           () -> this.sendChatMessage(
                              activeConfig.orderChatNodeId,
                              "\ud83d\udcb0 Права выданы, ваши "
                                 + activeConfig.units
                                 + " кк уже лежат в казне клана!\n\n\ud83d\udce5 Напишите в игровом чате: /clan withdraw all\n\ud83d\udcb5 Деньги упадут вам сразу же\n\n⚡ После вывода я выкину вас из клана — так и задумано"
                           )
                        );
                  }
               }
            } else {
               if (mc.currentScreen != null && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler2) {
                  Inventory inventory2 = genericContainerScreenHandler2.getInventory();
                  int memberSlot = -1;
                  int membersListSlot = -1;

                  for (int index2 = 0; index2 < inventory2.size(); index2++) {
                     String string = inventory2.getStack(index2).getName().getString();
                     if (memberSlot == -1 && string.toLowerCase().contains(activeConfig.playerName.toLowerCase())) {
                        memberSlot = index2;
                     }

                     if (membersListSlot == -1 && string.contains("Список участников")) {
                        membersListSlot = index2;
                     }
                  }

                  if (memberSlot != -1) {
                     InventoryUtils.sendSlotClickPacket(memberSlot, 0, SlotActionType.QUICK_MOVE, true);
                     mc.player.closeHandledScreen();
                     activeConfig.rightsGranted = true;
                     this.lastActionTimeMillis = System.currentTimeMillis();
                     return;
                  }

                  if (membersListSlot != -1) {
                     InventoryUtils.sendSlotClickPacket(membersListSlot, 0, SlotActionType.PICKUP, true);
                     this.lastActionTimeMillis = System.currentTimeMillis();
                     return;
                  }
               }

               if (System.currentTimeMillis() - activeConfig.lastClanMenuTimeMillis >= 1500L) {
                  activeConfig.lastClanMenuTimeMillis = System.currentTimeMillis();
                  this.lastActionTimeMillis = System.currentTimeMillis();
                  mc.player.networkHandler.sendChatCommand("clan menu");
               }
            }
         } else if (!this.idleAtHub) {
            this.idleAtHub = true;
            this.lastActionTimeMillis = System.currentTimeMillis();
            mc.player.networkHandler.sendChatCommand("hub");
         }
      }
   };
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
         String var6 = gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase();

         for (AutoDeliverModule.DeliveryConfig pendingConfig : this.concurrentLinkedQueue) {
            if (pendingConfig.playerName != null && var6.contains(pendingConfig.playerName.toLowerCase())) {
               if (var6.contains("присоединился к клану")) {
                  pendingConfig.joinedClan = true;
               } else if (var6.contains("из казны клана")) {
                  pendingConfig.fundsReceived = true;
               }
            }
         }
      }
   };

   private void sendChatMessage(String nodeId, String message) {
      if (nodeId != null && !nodeId.isEmpty()) {
         try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("node", nodeId);
            jsonObject.addProperty("last_message", -1);
            jsonObject.addProperty("content", message);
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("action", "chat_message");
            jsonObject2.add("data", jsonObject);
            JsonObject jsonObject3 = new JsonObject();
            jsonObject3.addProperty("type", "chat_node");
            jsonObject3.addProperty("id", nodeId);
            jsonObject3.addProperty("tag", "00000000");
            JsonObject jsonObject4 = new JsonObject();
            jsonObject4.addProperty("node", nodeId);
            jsonObject4.addProperty("last_message", -1);
            jsonObject4.addProperty("content", "");
            jsonObject3.add("data", jsonObject4);
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(jsonObject3);
            if (this.isResponseSuccessful(this.getHttpResponse(jsonArray.toString(), jsonObject2.toString()))) {
               return;
            }

            this.userId = null;
            if (this.refreshSession()) {
               this.getHttpResponse(jsonArray.toString(), jsonObject2.toString());
            }
         } catch (Exception exception) {
         }
      }
   }

   private boolean isResponseSuccessful(HttpResponse<String> response) {
      if (response.statusCode() != 200) {
         return false;
      }

      try {
         JsonObject asJsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
         if (asJsonObject.has("error") && !asJsonObject.get("error").isJsonNull()) {
            return false;
         } else {
            return asJsonObject.has("response") && asJsonObject.get("response").isJsonObject()
               ? !asJsonObject.getAsJsonObject("response").has("error") || asJsonObject.getAsJsonObject("response").get("error").isJsonNull()
               : true;
         }
      } catch (Exception exception) {
         return false;
      }
   }

   private boolean isPriceAcceptable(double pricePerUnit) {
      if (this.pricePer1M.getText().isBlank()) {
         return true;
      }

      try {
         double configuredPrice = Double.parseDouble(this.pricePer1M.getText().replace(',', '.').replaceAll("[^0-9.]", ""));
         return configuredPrice <= 0.0 || Math.abs(pricePerUnit - configuredPrice) <= configuredPrice * 0.25;
      } catch (Exception exception) {
         return true;
      }
   }

   private String formatText(String html, String attribute) {
      Matcher matcher = Pattern.compile(attribute + "=\"([^\"]*)\"").matcher(html);
      return matcher.find() ? matcher.group(1) : "";
   }

   private String formatText2() {
      StringBuilder stringBuilder = new StringBuilder("golden_key=").append(this.goldenKey.getText());

      for (String cookieName : this.cookieMap.keySet()) {
         stringBuilder.append("; ").append(cookieName).append('=').append(this.cookieMap.get(cookieName));
      }

      return stringBuilder.toString();
   }

   @Override
   protected void onEnable() {
      if (this.goldenKey.getText().isBlank()) {
         ClientChat.processData8("Укажите Golden-Key в настройках AutoDeliver");
         this.toggle();
      } else {
         this.userId = null;
         this.workers = null;
         this.orders = null;
         this.processedOrderIds.clear();
         this.concurrentLinkedQueue.clear();
         this.idleAtHub = false;
         this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "greed-funpay-deliver");
            thread.setDaemon(true);
            return thread;
         });
         this.scheduledExecutorService.scheduleWithFixedDelay(this::refreshDeliveries, 0L, 4L, TimeUnit.SECONDS);
      }
   }

   private boolean refreshSession() {
      if (this.userId != null) {
         return true;
      }

      try {
         this.cookieMap.clear();
         HttpResponse<String> response = this.httpClient
            .send(
               HttpRequest.newBuilder(URI.create("https://funpay.com/"))
                  .timeout(Duration.ofSeconds(5L))
                  .header("cookie", "golden_key=" + this.goldenKey.getText())
                  .GET()
                  .build(),
               BodyHandlers.ofString()
            );
         if (this.formatText4(response.body(), "user-link-name").isEmpty()) {
            return false;
         }

         Matcher matcher = pattern.matcher(response.body());
         if (!matcher.find()) {
            return false;
         }

         JsonObject asJsonObject = JsonParser.parseString(this.formatText3(matcher.group(1))).getAsJsonObject();
         this.userId = asJsonObject.get("userId").getAsString();
         this.csrfToken = asJsonObject.get("csrf-token").getAsString();
         this.captureCookies(response);
         this.workers = null;
         return true;
      } catch (Exception exception) {
         return false;
      }
   }

   private void refreshDeliveries() {
      try {
         if (!this.refreshSession()) {
            return;
         }

         this.syncWorkers();
         this.syncOrders();
      } catch (Exception exception) {
         this.userId = null;
      }
   }

   @Override
   protected void onDisable() {
      if (this.scheduledExecutorService != null) {
         this.scheduledExecutorService.shutdownNow();
      }

      this.scheduledExecutorService = null;
      this.concurrentLinkedQueue.clear();
   }

   private ArrayList<AutoDeliverModule.DeliveryOrder> getArrayList() {
      ArrayList<AutoDeliverModule.DeliveryOrder> resultOrders = new ArrayList<>();

      try {
         HttpResponse<String> response = this.httpClient
            .send(
               HttpRequest.newBuilder(URI.create("https://funpay.com/orders/trade"))
                  .timeout(Duration.ofSeconds(5L))
                  .header("cookie", this.formatText2())
                  .header("accept", "*/*")
                  .header("x-requested-with", "XMLHttpRequest")
                  .POST(BodyPublishers.noBody())
                  .build(),
               BodyHandlers.ofString()
            );
         this.captureCookies(response);
         String body = response.body();

         for (String itemHtml : this.getArrayList3(body, "tc-item")) {
            try {
               String orderDescHtml = itemHtml.contains("order-desc") ? itemHtml.substring(itemHtml.indexOf("order-desc")) : "";
               if (this.formatText4(itemHtml, "tc-status").equalsIgnoreCase("Оплачен") && this.formatText4(orderDescHtml, "text-muted").contains("Валюта")) {
                  String orderDescription = this.formatText4(itemHtml, "order-desc");
                  int quantity = 1;
                  Matcher matcher = pattern4.matcher(orderDescription);

                  while (matcher.find()) {
                     quantity = Integer.parseInt(matcher.group(1));
                  }

                  double totalPrice = Double.parseDouble(this.formatText4(itemHtml, "tc-price").replace(',', '.').replaceAll("[^0-9.]", ""));
                  if (this.isPriceAcceptable(totalPrice / quantity)) {
                     String buyerName = this.formatText4(itemHtml, "media-user-name");
                     if (!buyerName.isEmpty()) {
                        resultOrders.add(new AutoDeliverModule.DeliveryOrder(this.formatText4(itemHtml, "tc-order"), buyerName, totalPrice, quantity));
                     }
                  }
               }
            } catch (Exception exception) {
            }
         }

         return resultOrders;
      } catch (Exception exception2) {
         return null;
      }
   }

   private void captureCookies(HttpResponse<String> response) {
      for (String setCookie : response.headers().allValues("set-cookie")) {
         String cookiePair = setCookie.split(";", 2)[0].trim();
         if (cookiePair.indexOf(61) > 0 && !cookiePair.startsWith("golden_key=")) {
            this.cookieMap.put(cookiePair.substring(0, cookiePair.indexOf(61)), cookiePair.substring(cookiePair.indexOf(61) + 1));
         }
      }
   }

   private int getConfiguredAnarchy() {
      String digits = this.anarchyDelivery.getText().replaceAll("\\D", "");
      return digits.isEmpty() ? -1 : Integer.parseInt(digits);
   }

   private ArrayList<AutoDeliverModule.DeliveryWorker> getArrayList2() throws Exception {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("type", "chat_bookmarks");
      jsonObject.addProperty("id", this.userId);
      jsonObject.addProperty("tag", this.chatTag);
      jsonObject.addProperty("data", false);
      JsonArray jsonArray = new JsonArray();
      jsonArray.add(jsonObject);
      ArrayList<AutoDeliverModule.DeliveryWorker> resultWorkers = new ArrayList<>();

      for (JsonElement jsonElement : JsonParser.parseString(this.getHttpResponse(jsonArray.toString(), String.valueOf(false)).body())
         .getAsJsonObject()
         .getAsJsonArray("objects")) {
         JsonObject asJsonObject = jsonElement.getAsJsonObject();
         if ("chat_bookmarks".equalsIgnoreCase(asJsonObject.get("type").getAsString())) {
            this.chatTag = asJsonObject.get("tag").getAsString();

            for (String contactHtml : this.getArrayList3(asJsonObject.getAsJsonObject("data").get("html").getAsString(), "contact-item")) {
               AutoDeliverModule.DeliveryWorker createdWorker = new AutoDeliverModule.DeliveryWorker();
               createdWorker.contactNodeId = this.formatText(contactHtml, "data-id");
               createdWorker.lastMessageId = this.formatText(contactHtml, "data-node-msg").isEmpty()
                  ? 0L
                  : Long.parseLong(this.formatText(contactHtml, "data-node-msg"));
               createdWorker.buyerLogin = this.formatText4(contactHtml, "media-user-name");
               if (!createdWorker.contactNodeId.isEmpty() && !createdWorker.buyerLogin.isEmpty()) {
                  resultWorkers.add(createdWorker);
               }
            }
         }
      }

      return resultWorkers;
   }

   private ArrayList<String> getArrayList3(String html, String className) {
      ArrayList<String> elements = new ArrayList<>();
      Matcher matcher = this.getPattern(className).matcher(html);

      int blockStart;
      for (blockStart = -1; matcher.find(); blockStart = Math.max(html.lastIndexOf(60, matcher.start()), 0)) {
         if (blockStart != -1) {
            elements.add(html.substring(blockStart, matcher.start()));
         }
      }

      if (blockStart != -1) {
         elements.add(html.substring(blockStart));
      }

      return elements;
   }

   private String formatText3(String html) {
      return html.replace("&quot;", "\"")
         .replace("&#039;", "'")
         .replace("&apos;", "'")
         .replace("&lt;", "<")
         .replace("&gt;", ">")
         .replace("&nbsp;", " ")
         .replace(" ", " ")
         .replace("&amp;", "&");
   }

   private void refundOrder(String orderId) {
      try {
         HttpResponse<String> response = this.httpClient
            .send(
               HttpRequest.newBuilder(URI.create("https://funpay.com/orders/" + (orderId.startsWith("#") ? orderId.substring(1) : orderId) + "/"))
                  .timeout(Duration.ofSeconds(5L))
                  .header("cookie", this.formatText2())
                  .GET()
                  .build(),
               BodyHandlers.ofString()
            );
         this.captureCookies(response);
         String body = response.body();
         if (!body.contains("modal-refund")) {
            return;
         }

         String refundHtml = body.substring(body.indexOf("modal-refund"));
         Matcher matcher = pattern2.matcher(refundHtml);
         if (!matcher.find()) {
            return;
         }

         String formHtml = refundHtml.contains("</form>") ? refundHtml.substring(0, refundHtml.indexOf("</form>")) : refundHtml;
         StringBuilder stringBuilder = new StringBuilder();
         Matcher matcher2 = pattern3.matcher(formHtml);

         while (matcher2.find()) {
            if (matcher2.group().contains("type=\"hidden\"")) {
               stringBuilder.append(URLEncoder.encode(this.formatText(matcher2.group(), "name"), StandardCharsets.UTF_8))
                  .append('=')
                  .append(URLEncoder.encode(this.formatText(matcher2.group(), "value"), StandardCharsets.UTF_8))
                  .append('&');
            }
         }

         stringBuilder.append("csrf_token=").append(URLEncoder.encode(this.csrfToken == null ? "" : this.csrfToken, StandardCharsets.UTF_8));
         this.httpClient
            .send(
               HttpRequest.newBuilder(URI.create(matcher.group(1).startsWith("http") ? matcher.group(1) : "https://funpay.com" + matcher.group(1)))
                  .timeout(Duration.ofSeconds(5L))
                  .header("cookie", this.formatText2())
                  .header("accept", "*/*")
                  .header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                  .header("x-requested-with", "XMLHttpRequest")
                  .POST(BodyPublishers.ofString(stringBuilder.toString(), StandardCharsets.UTF_8))
                  .build(),
               BodyHandlers.ofString()
            );
      } catch (Exception exception) {
      }
   }

   private int getCurrentAnarchy() {
      if (mc.world == null) {
         return -1;
      }

      ScoreboardObjective objectiveForSlot = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      if (objectiveForSlot == null) {
         return -1;
      }

      Matcher matcher = pattern5.matcher(objectiveForSlot.getDisplayName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""));
      return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
   }

   private void syncWorkers() throws Exception {
      ArrayList<AutoDeliverModule.DeliveryWorker> fetchedWorkers = this.getArrayList2();
      if (!fetchedWorkers.isEmpty()) {
         ArrayList<AutoDeliverModule.DeliveryWorker> previousWorkers = this.workers;
         this.workers = fetchedWorkers;
         if (previousWorkers != null) {
            for (AutoDeliverModule.DeliveryWorker worker : fetchedWorkers) {
               AutoDeliverModule.DeliveryWorker existingWorker = previousWorkers.stream()
                  .filter(oldWorker -> oldWorker.contactNodeId.equals(worker.contactNodeId))
                  .findFirst()
                  .orElse(null);
               long previousMessageId = existingWorker != null ? existingWorker.lastMessageId : 0L;
               if (existingWorker != null) {
                  worker.dialogState = existingWorker.dialogState;
                  worker.nickname = existingWorker.nickname;
                  worker.deliveryStartTimeMillis = existingWorker.deliveryStartTimeMillis;
               }

               if (worker.lastMessageId > previousMessageId) {
                  JsonObject jsonObject = new JsonObject();
                  jsonObject.addProperty("type", "chat_node");
                  jsonObject.addProperty("id", worker.contactNodeId);
                  jsonObject.addProperty("tag", "00000000");
                  JsonObject jsonObject2 = new JsonObject();
                  jsonObject2.addProperty("node", worker.contactNodeId);
                  jsonObject2.addProperty("last_message", -1);
                  jsonObject2.addProperty("content", "");
                  jsonObject.add("data", jsonObject2);
                  JsonArray jsonArray = new JsonArray();
                  jsonArray.add(jsonObject);

                  for (JsonElement jsonElement : JsonParser.parseString(this.getHttpResponse(jsonArray.toString(), String.valueOf(false)).body())
                     .getAsJsonObject()
                     .getAsJsonArray("objects")) {
                     JsonObject asJsonObject = jsonElement.getAsJsonObject();
                     if ("chat_node".equalsIgnoreCase(asJsonObject.get("type").getAsString())) {
                        for (JsonElement jsonElement2 : asJsonObject.getAsJsonObject("data").getAsJsonArray("messages")) {
                           JsonObject asJsonObject2 = jsonElement2.getAsJsonObject();
                           if (asJsonObject2.get("id").getAsLong() > previousMessageId
                              && !asJsonObject2.get("author").getAsString().equals(this.userId)
                              && !asJsonObject2.get("author").getAsString().equals("0")) {
                              String messageText = this.formatText4(asJsonObject2.get("html").getAsString(), "chat-msg-text");
                              String normalizedMessage = messageText.replaceAll("[^a-zA-Zа-яА-Я]", "").toLowerCase();
                              AutoDeliverModule.DeliveryOrder matchedOrder = this.findOrderByBuyer(worker.buyerLogin);
                              if (matchedOrder == null
                                 || worker.dialogState != AutoDeliverModule.TypeOption.NICKNAME
                                 || !normalizedMessage.equals("отмена") && !normalizedMessage.equals("otmena")) {
                                 if (worker.dialogState == AutoDeliverModule.TypeOption.NICKNAME) {
                                    worker.nickname = messageText.replaceAll("[^a-zA-Z0-9_]", "");
                                    worker.dialogState = AutoDeliverModule.TypeOption.CONFIRM_NICKNAME;
                                    this.sendChatMessage(
                                       worker.contactNodeId,
                                       "\ud83d\udd0e Проверьте данные перед выдачей\n\n\ud83d\udc64 Ник: "
                                          + worker.nickname
                                          + "\n\ud83d\udce6 Сумма: "
                                          + (matchedOrder != null ? matchedOrder.units() : 0)
                                          + " кк\n\n✅ Всё верно — напишите «Да» или «+»\n✏️ Ошибся в нике — напишите «Нет» или «-»"
                                    );
                                 } else if (worker.dialogState == AutoDeliverModule.TypeOption.CONFIRM_NICKNAME) {
                                    if (!normalizedMessage.equals("да") && !normalizedMessage.equals("da") && !messageText.contains("+") && !messageText.contains("✓")) {
                                       if (normalizedMessage.equals("нет") || normalizedMessage.equals("net") || messageText.contains("-") || messageText.contains("✗")
                                          )
                                        {
                                          worker.dialogState = AutoDeliverModule.TypeOption.NICKNAME;
                                          this.sendChatMessage(worker.contactNodeId, "✏️ Хорошо, напишите ваш игровой ник ещё раз одним сообщением");
                                       }
                                    } else if (matchedOrder != null && matchedOrder.units() > 0) {
                                       AutoDeliverModule.DeliveryConfig newConfig = new AutoDeliverModule.DeliveryConfig();
                                       newConfig.playerName = worker.nickname;
                                       newConfig.orderChatNodeId = worker.contactNodeId;
                                       newConfig.orderId = matchedOrder.id();
                                       newConfig.units = matchedOrder.units();
                                       this.concurrentLinkedQueue.add(newConfig);
                                       this.sendChatMessage(
                                          worker.contactNodeId,
                                          "\ud83d\ude80 Данные приняты, начинаю выдачу!\n\n1️⃣ Зайдите на Анархию-"
                                             + this.getConfiguredAnarchy()
                                             + "\n2️⃣ Примите приглашение в клан — оно уже летит вам\n3️⃣ Дождитесь моего сообщения и напишите /clan withdraw all\n\n\ud83d\udce6 К выдаче: "
                                             + matchedOrder.units()
                                             + " кк"
                                       );
                                       worker.dialogState = AutoDeliverModule.TypeOption.DELIVERY;
                                       worker.deliveryStartTimeMillis = System.currentTimeMillis();
                                    } else {
                                       this.sendChatMessage(worker.contactNodeId, "⏳ Не вижу ваш оплаченный заказ, напишите мне через минуту — выдам сразу же!");
                                    }
                                 }
                              } else {
                                 worker.dialogState = null;
                                 this.refundOrder(matchedOrder.id());
                                 this.sendChatMessage(
                                    worker.contactNodeId,
                                    "↩️ Заказ отменён, средства вернулись на ваш баланс FunPay\n\n\ud83d\udc9b Будем рады видеть вас снова!"
                                 );
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private AutoDeliverModule() {
      this.goldenKey = this.addSetting(new NumericTextSetting("Golden-Key", "Ключ сессии FunPay из cookie golden_key.", "", 64));
      this.pricePer1M = this.addSetting(
         new NumericTextSetting("Цена за 1кк", "Своя цена лота без комиссии FunPay, пусто - обслуживать любые заказы на валюту.", "", 16)
      );
      this.legacyNumberSetting = this.addSetting(
         new LegacyNumberSetting("Продавать при сумме от ляма", "Заказы меньше этой суммы бот возвращает покупателю.", 10.0, 1.0, 50.0, 1.0)
      );
      this.anarchyDelivery = this.addSetting(new NumericTextSetting("Анархия выдачи", "Куда бот летит выдавать и куда зовёт покупателя.", "an305", 16));
   }

   private Pattern getPattern(String className) {
      return Pattern.compile("class=\"[^\"]*(?<![-\\w])" + className + "(?![-\\w])[^\"]*\"");
   }

   private String formatText4(String html, String className) {
      Matcher matcher = this.getPattern(className).matcher(html);
      if (!matcher.find()) {
         return "";
      }

      int tagEnd = html.indexOf(62, matcher.end());

      while (tagEnd != -1) {
         int tagStart = html.indexOf(60, tagEnd + 1);
         String text = this.formatText3(tagStart == -1 ? html.substring(tagEnd + 1) : html.substring(tagEnd + 1, tagStart))
            .trim();
         if (!text.isEmpty()) {
            return text;
         }

         if (tagStart == -1) {
            return "";
         }

         tagEnd = html.indexOf(62, tagStart);
      }

      return "";
   }

   private HttpResponse<String> getHttpResponse(String objectsJson, String requestJson) throws Exception {
      String formBody = "objects="
         + URLEncoder.encode(objectsJson == null ? "" : objectsJson, StandardCharsets.UTF_8)
         + "&request="
         + URLEncoder.encode(requestJson == null ? "" : requestJson, StandardCharsets.UTF_8)
         + "&csrf_token="
         + URLEncoder.encode(this.csrfToken == null ? "" : this.csrfToken, StandardCharsets.UTF_8);
      HttpResponse<String> response = this.httpClient
         .send(
            HttpRequest.newBuilder(URI.create("https://funpay.com/runner/"))
               .timeout(Duration.ofSeconds(5L))
               .header("cookie", this.formatText2())
               .header("accept", "*/*")
               .header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
               .header("x-requested-with", "XMLHttpRequest")
               .POST(BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
               .build(),
            BodyHandlers.ofString()
         );
      this.captureCookies(response);
      return response;
   }

   private void syncOrders() {
      ArrayList<AutoDeliverModule.DeliveryOrder> fetchedOrders = this.getArrayList();
      if (fetchedOrders != null) {
         ArrayList<AutoDeliverModule.DeliveryOrder> previousOrders = this.orders;
         this.orders = fetchedOrders;
         if (previousOrders != null) {
            if (this.workers != null) {
               for (AutoDeliverModule.DeliveryOrder order : previousOrders) {
                  if (!fetchedOrders.stream().anyMatch(currentOrder -> currentOrder.id().equals(order.id()))) {
                     this.workers
                        .stream()
                        .filter(workerByBuyer -> workerByBuyer.buyerLogin.equalsIgnoreCase(order.buyerLogin()))
                        .filter(workerWithDialog -> workerWithDialog.dialogState != null)
                        .findFirst()
                        .ifPresent(resetWorker -> resetWorker.dialogState = null);
                  }
               }

               for (AutoDeliverModule.DeliveryOrder order : fetchedOrders) {
                  if (!this.processedOrderIds.contains(order.id())) {
                     this.processedOrderIds.add(order.id());
                     this.workers
                        .stream()
                        .filter(matchingWorker -> matchingWorker.buyerLogin.equalsIgnoreCase(order.buyerLogin()))
                        .filter(idleWorker -> idleWorker.dialogState == null)
                        .findFirst()
                        .ifPresent(
                           targetWorker -> {
                              if (order.units() < this.legacyNumberSetting.getValue()) {
                                 this.refundOrder(order.id());
                                 this.sendChatMessage(
                                    targetWorker.contactNodeId,
                                    "❌ Минимальный заказ — "
                                       + this.legacyNumberSetting.intValue()
                                       + " кк, ваш меньше\n\n\ud83d\udcb8 Средства уже вернулись на ваш баланс FunPay, извините за неудобство!"
                                 );
                              } else {
                                 this.sendChatMessage(
                                    targetWorker.contactNodeId,
                                    "\ud83d\udc4b Привет! На связи автовыдача валюты — greed client\n\n\ud83d\udce6 Ваш заказ: "
                                       + order.units()
                                       + " кк\n✏️ Напишите ваш игровой ник одним сообщением\n\nℹ️ Передумали? Напишите «Отмена» — сразу вернём средства"
                                 );
                                 targetWorker.dialogState = AutoDeliverModule.TypeOption.NICKNAME;
                              }
                           }
                        );
                  }
               }
            }
         } else {
            for (AutoDeliverModule.DeliveryOrder order : fetchedOrders) {
               this.processedOrderIds.add(order.id());
            }
         }
      }
   }

   public static AutoDeliverModule getInstance() {
      if (autoDeliverModule == null) {
         autoDeliverModule = new AutoDeliverModule();
      }

      return autoDeliverModule;
   }

   private AutoDeliverModule.DeliveryOrder findOrderByBuyer(String login) {
      return this.orders == null
         ? null
         : this.orders.stream().filter(orderByBuyer -> orderByBuyer.buyerLogin().equalsIgnoreCase(login)).findFirst().orElse(null);
   }

   private static final class DeliveryConfig {
      private String playerName;
      private String orderChatNodeId;
      private String orderId;
      private int units;
      private long lastInviteTimeMillis;
      private long lastClanMenuTimeMillis;
      private long lastKickTimeMillis;
      private boolean joinedClan;
      private boolean invested;
      private boolean rightsGranted;
      private boolean rightsMessageSent;
      private boolean fundsReceived;
   }

   private record DeliveryOrder(String id, String buyerLogin, double price, int units) {
   }

   private static final class DeliveryWorker {
      private String contactNodeId;
      private String buyerLogin;
      private String nickname;
      private long lastMessageId;
      private AutoDeliverModule.TypeOption dialogState;
      private long deliveryStartTimeMillis;
   }

   private enum TypeOption {
      NICKNAME,
      CONFIRM_NICKNAME,
      DELIVERY;
   }
}
