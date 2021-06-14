package org.hyperledger.bpa.impl.rules.definitions;

import com.deliveredtechnologies.rulebook.model.rulechain.cor.CoRRuleBook;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data @NoArgsConstructor
public abstract class BaseRule extends CoRRuleBook<Boolean> {

    private UUID taskId;

    public BaseRule(UUID taskId) {
        this.taskId = taskId;
        super.setDefaultResult(Boolean.FALSE);
    }
}
