package com.logistics.core.crash;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CrashReportNotifier")
class CrashReportNotifierTest {

    @Test
    @DisplayName("shows the status line only to operators who haven't silenced it")
    void gateLogic() {
        assertThat(CrashReportNotifier.shouldNotify(true, true)).isTrue();    // notices on + operator
        assertThat(CrashReportNotifier.shouldNotify(false, true)).isFalse();  // notices silenced
        assertThat(CrashReportNotifier.shouldNotify(true, false)).isFalse();  // not an operator
    }

    @Test
    @DisplayName("OFF state shows [OFF] that enables, with a More Info link")
    void inviteWhenOff() {
        Component invite = CrashReportNotifier.buildInvite(false);
        assertThat(invite.getString())
                .contains("Crash reporting is").contains("[OFF]").contains("[Hide Notification]").contains("[More Info]");
        assertThat(commandOf(invite, "[OFF]")).isEqualTo("/logistics config crash_reporting_enabled true");
        assertThat(commandOf(invite, "[Hide Notification]"))
                .isEqualTo("/logistics config crash_reporting_notify_operators false");
        assertThat(infoLinkPresent(invite)).isTrue();
    }

    @Test
    @DisplayName("ON state shows [ON] that disables")
    void inviteWhenOn() {
        Component invite = CrashReportNotifier.buildInvite(true);
        assertThat(invite.getString()).contains("[ON]");
        assertThat(commandOf(invite, "[ON]")).isEqualTo("/logistics config crash_reporting_enabled false");
    }

    /** Command bound to the run-command click on the sibling whose visible text equals {@code label}. */
    private static String commandOf(Component invite, String label) {
        for (Component part : invite.getSiblings()) {
            if (part.getString().equals(label)
                    && part.getStyle().getClickEvent() instanceof ClickEvent.RunCommand run) {
                return run.command();
            }
        }
        return null;
    }

    private static boolean infoLinkPresent(Component invite) {
        for (Component part : invite.getSiblings()) {
            if (part.getStyle().getClickEvent() instanceof ClickEvent.OpenUrl open
                    && open.uri().toString().contains("CRASH_REPORTING.md")) {
                return true;
            }
        }
        return false;
    }
}
