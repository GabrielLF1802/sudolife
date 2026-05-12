package com.sudolife.application.model.strava;

import org.junit.jupiter.api.Test;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.LINK_ID;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.UNLINKED_AT;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.activeStravaAccountLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StravaAccountLinkUnitTest {

    @Test
    void active_creates_active_link_without_unlinked_at() {
        StravaAccountLink link = StravaAccountLink.active(LINK_ID, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN,
                EXPIRES_AT, LINKED_AT);

        assertThat(link.isLinked()).isTrue();
        assertThat(link.isInactive()).isFalse();
        assertThat(link.getUnlinkedAt()).isNull();
    }

    @Test
    void deactivate_marks_link_inactive_with_unlinked_at() {
        StravaAccountLink link = activeStravaAccountLink();

        link.deactivate(UNLINKED_AT);

        assertThat(link.isLinked()).isFalse();
        assertThat(link.isInactive()).isTrue();
        assertThat(link.getUnlinkedAt()).isEqualTo(UNLINKED_AT);
        assertThat(link.getAccessToken()).isNull();
        assertThat(link.getRefreshToken()).isNull();
        assertThat(link.getExpiresAt()).isNull();
    }

    @Test
    void constructor_throws_when_inactive_link_has_no_unlinked_at() {
        assertThatThrownBy(() -> new StravaAccountLink(LINK_ID, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN,
                EXPIRES_AT, false, LINKED_AT, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_throws_when_active_link_has_unlinked_at() {
        assertThatThrownBy(() -> new StravaAccountLink(LINK_ID, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN,
                EXPIRES_AT, true, LINKED_AT, UNLINKED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_throws_when_inactive_link_has_authorization_data() {
        assertThatThrownBy(() -> new StravaAccountLink(LINK_ID, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, null, null,
                false, LINKED_AT, UNLINKED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
