package greed;

import greed.client.event.EventBus;
import greed.client.event.EventListener;
import greed.client.event.EventPriority;
import greed.client.setting.BooleanSetting;
import greed.client.setting.MultiChoiceSetting;
import greed.client.setting.NumberSetting;
import greed.client.setting.SingleChoiceSetting;
import greed.client.util.Animation;
import greed.client.util.Easings;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import ru.dreamix.protection.api.impl.profile.objects.RoleObject;

/** Run with assertions enabled; covers reconstructed settings, animations and dispatch. */
public final class RecoveryChecks {
    private static final class Subscribers {
        final StringBuilder order = new StringBuilder();
        @EventPriority(10) final EventListener<String> first = event -> order.append('A');
        final EventListener<String> second = event -> order.append('B');
    }

    public static void main(String[] args) {
        NumberSetting number = new NumberSetting("number", "").withRange(0, 2).withStep(0.25f);
        number.setValue(0.61f);
        assert number.getValue() == 0.5f;
        number.setValue(9);
        assert number.floatValue() == 2.0f && number.doubleValue() == 2.0;

        BooleanSetting flag = new BooleanSetting("flag", "", true);
        flag.toggleValue();
        assert !flag.isChecked() && flag.getDefaultValue();

        SingleChoiceSetting choice = new SingleChoiceSetting("choice", "").withOptions("one", "two");
        choice.selectIndex(99);
        assert choice.isSelected("two");
        choice.cycle();
        assert choice.isSelected("one");

        MultiChoiceSetting choices = new MultiChoiceSetting("choices", "").withOptions("one", "two").withSelection("one").withMinimumSelection(1);
        choices.toggleOption("one");
        assert choices.isSelected("one");
        choices.toggleOption("two");
        assert choices.getSelectedCount() == 2;

        Animation animation = new Animation();
        animation.resetTo(0.25);
        assert animation.floatValue() == 0.25f;
        animation.animateTo(1, 0, Easings.LINEAR);
        animation.update();
        assert animation.isFinished() && animation.floatValue() == 1.0f;

        Subscribers subscribers = new Subscribers();
        EventBus.getInstance().subscribe(subscribers);
        EventBus.getInstance().post("check");
        assert subscribers.order.toString().equals("AB") : subscribers.order;
        EventBus.getInstance().unsubscribe(subscribers);
        EventBus.getInstance().post("check");
        assert subscribers.order.toString().equals("AB");

        ProfileAPI.create("development", 1, RoleObject.DEFAULT);
        assert ProfileAPI.get().getModel().getName().equals("development");
        System.out.println("Recovery checks passed: settings, numeric conversion, animation, listener priority, local profile.");
    }
}
