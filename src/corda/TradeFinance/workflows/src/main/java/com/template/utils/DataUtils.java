package com.template.utils;

import com.template.states.OrderState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.Collections;
import java.util.List;

public class DataUtils {

    public static StateAndRef<OrderState> getOrder(ServiceHub serviceHub, String orderId) {
        //Check if an order with this ID already exists
        QueryCriteria.LinearStateQueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria()
                .withExternalId(Collections.singletonList(orderId)).withStatus(Vault.StateStatus.UNCONSUMED);
        List<StateAndRef<OrderState>> results = serviceHub.getVaultService().queryBy(OrderState.class, queryCriteria).getStates();
        if (results.isEmpty()) {
            throw new IllegalArgumentException("An order with ID " + orderId + " does not exist or is already consumed.");
        }
        return results.get(0);
    }

}
