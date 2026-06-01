package com.logistics.core.crash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CrashReportNotifier")
class CrashReportNotifierTest {

    @Test
    @DisplayName("invites only operators when reporting is off and notices are enabled")
    void gateLogic() {
        assertThat(CrashReportNotifier.shouldNotify(false, true, true)).isTrue();   // off + notices + op
        assertThat(CrashReportNotifier.shouldNotify(true, true, true)).isFalse();   // already reporting
        assertThat(CrashReportNotifier.shouldNotify(false, false, true)).isFalse(); // notices silenced
        assertThat(CrashReportNotifier.shouldNotify(false, true, false)).isFalse(); // not an operator
    }

    @Test
    @DisplayName("invite text names the commands and links the details page")
    void inviteContent() {
        String text = CrashReportNotifier.buildInvite().getString();
        assertThat(text).contains("/logistics crashreports enable");
        assertThat(text).contains("/logistics crashreports notify off");
        assertThat(text).contains("CRASH_REPORTING.md");
    }
}
