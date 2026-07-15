package com.sudolife.adapter.driven.api.training;

import com.sudolife.application.service.training.AiRunningPlanProposal;
import com.sudolife.application.service.training.TrainingSnapshot;
import com.sudolife.application.service.training.ports.required.AiRunningPlanProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpAiRunningPlanProvider implements AiRunningPlanProvider {

    private final RestClient restClient;

    public HttpAiRunningPlanProvider(@Value("${ai.running-plan-provider-url}") String providerUrl) {
        this.restClient = RestClient.create(providerUrl);
    }

    @Override
    public AiRunningPlanProposal draft(TrainingSnapshot snapshot) {
        return restClient.post()
                .uri("/running-plan-proposals")
                .body(snapshot)
                .retrieve()
                .body(AiRunningPlanProposal.class);
    }
}
