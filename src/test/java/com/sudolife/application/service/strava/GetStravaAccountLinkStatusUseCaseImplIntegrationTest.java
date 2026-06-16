package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.service.strava.ports.provided.GetStravaAccountLinkStatusUseCase;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.getStravaAccountLinkStatusCommand;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class GetStravaAccountLinkStatusUseCaseImplIntegrationTest {

    @Autowired
    private GetStravaAccountLinkStatusUseCase useCase;

    @Autowired
    private StravaAccountLinkRepository accountLinkRepository;

    @Test
    void execute_returns_linked_status_with_real_repository() {
        accountLinkRepository.save(StravaAccountLink.active(null, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN,
                EXPIRES_AT, LINKED_AT));

        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.linked()).isTrue();
        assertThat(result.athleteId()).isEqualTo(ATHLETE_ID);
    }

    @Test
    void execute_returns_unlinked_status_with_real_repository() {
        StravaLinkStatusResult result = useCase.execute(getStravaAccountLinkStatusCommand());

        assertThat(result.linked()).isFalse();
        assertThat(result.athleteId()).isNull();
    }
}
