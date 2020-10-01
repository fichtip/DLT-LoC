package com.template;

import com.google.common.collect.ImmutableList;
import com.template.flows.*;
import com.template.states.OrderState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode sellerNode;
    private StartedMockNode buyerNode;
    private StartedMockNode freightNode;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.template.contracts"),
                TestCordapp.findCordapp("com.template.flows"))));
        sellerNode = network.createPartyNode(new CordaX500Name("Seller", "Berlin", "DE"));
        buyerNode = network.createPartyNode(new CordaX500Name("Buyer", "Vienna", "AT"));
        freightNode = network.createPartyNode(new CordaX500Name("Freight Company", "New York", "US"));
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        for (StartedMockNode node : ImmutableList.of(sellerNode, buyerNode, freightNode)) {
            node.registerInitiatedFlow(CancelOrderResponder.class);
            node.registerInitiatedFlow(CheckDeliveryDateResponder.class);
            node.registerInitiatedFlow(ConfirmOrderResponder.class);
            node.registerInitiatedFlow(CreateOrderResponder.class);
            node.registerInitiatedFlow(ShipOrderResponder.class);
            node.registerInitiatedFlow(SignArrivalResponder.class);
        }
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void createOrderTest() throws ExecutionException, InterruptedException {
        CreateOrder flow = new CreateOrder("Buyer", "1", 100, 2.0, "10 EUR", "2 EUR", "Karlsplatz 13, 1040 Wien", "2020-09-30");
        CordaFuture<String> future = sellerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Order with ID '1' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' added.");
    }

    @Test
    public void cancelOrderTest() throws ExecutionException, InterruptedException {
        FlowLogic<String> flow = new CreateOrder("Buyer", "1", 100, 2.0, "10 EUR", "2 EUR", "Karlsplatz 13, 1040 Wien", "2020-09-30");
        CordaFuture<String> future = sellerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Order with ID '1' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' added.");

        flow = new CancelOrder("1");
        future = buyerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Cancel flow for order with ID '1' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' executed.");
    }

    @Test
    public void confirmOrderTest() throws ExecutionException, InterruptedException {
        FlowLogic<String> flow = new CreateOrder("Buyer", "2", 123587, 5.0, "750 EUR", "4 EUR", "Ballhausplatz 2, 1010 Wien", "2020-12-01");
        CordaFuture<String> future = sellerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Order with ID '2' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' added.");

        flow = new ConfirmOrder("2");
        future = buyerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Confirm order flow for order with ID '2' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' executed.");
    }

    @Test
    public void shipOrderTest() throws ExecutionException, InterruptedException {
        FlowLogic<String> flow = new CreateOrder("Buyer", "2", 123587, 5.0, "750 EUR", "4 EUR", "Ballhausplatz 2, 1010 Wien", "2020-12-01");
        CordaFuture<String> future = sellerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Order with ID '2' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' added.");

        flow = new ConfirmOrder("2");
        future = buyerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Confirm order flow for order with ID '2' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' executed.");

        flow = new ShipOrder("2", "Freight Company", "XAFDWEQ");
        future = sellerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Ship order flow for order with ID '2' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' executed.");
    }

    @Test
    public void signArrivalTest() throws ExecutionException, InterruptedException {
        FlowLogic<String> flow = new CreateOrder("Buyer", "2", 123587, 5.0, "750 EUR", "4 EUR", "Ballhausplatz 2, 1010 Wien", "2020-12-01");
        CordaFuture<String> future = sellerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Order with ID '2' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' added.");

        flow = new ConfirmOrder("2");
        future = buyerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Confirm order flow for order with ID '2' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' executed.");

        flow = new ShipOrder("2", "Freight Company", "XAFDWEQ");
        future = sellerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Ship order flow for order with ID '2' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' executed.");

        flow = new SignArrival("2");
        future = buyerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("The arrival of the order with ID '2' has been signed by '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "'");

        flow = new SignArrival("2");
        future = freightNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("The arrival of the order with ID '2' has been signed by '" + freightNode.getInfo().getLegalIdentities().get(0).getName() + "'");

        // We check the recorded order in all three vaults.
        for (StartedMockNode node : ImmutableList.of(sellerNode, buyerNode, freightNode)) {
            node.transaction(() -> {
                List<StateAndRef<OrderState>> orders = node.getServices().getVaultService().queryBy(OrderState.class).getStates();
                assertEquals(1, orders.size());
                OrderState recordedState = orders.get(0).getState().getData();
                assertEquals(recordedState.getOrderState(), OrderState.State.DELIVERED);
                return null;
            });
        }
    }

    @Test
    public void checkDeliveryDateTest() throws ExecutionException, InterruptedException {
        FlowLogic<String> flow = new CreateOrder("Buyer", "3", 68754, 1.0, "1337 EUR", "2 EUR", "Michaelerkuppel, 1010 Wien", "2020-08-15");
        CordaFuture<String> future = sellerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Order with ID '3' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' added.");

        flow = new CheckDeliveryDate("3");
        future = buyerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Check delivery date flow for order with ID '3' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' executed.");

        // We check the recorded order in all three vaults.
        for (StartedMockNode node : ImmutableList.of(sellerNode, buyerNode)) {
            node.transaction(() -> {
                List<StateAndRef<OrderState>> orders = node.getServices().getVaultService().queryBy(OrderState.class).getStates();
                assertEquals(1, orders.size());
                OrderState recordedState = orders.get(0).getState().getData();
                assertEquals(OrderState.State.PASSED, recordedState.getOrderState());
                return null;
            });
        }
    }

    @Test(expected = Exception.class)
    public void confirmCancelledOrderTest() throws ExecutionException, InterruptedException {
        FlowLogic<String> flow = new CreateOrder("Buyer", "1", 100, 2.0, "10 EUR", "2 EUR", "Karlsplatz 13, 1040 Wien", "2020-09-30");
        CordaFuture<String> future = sellerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Order with ID '1' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' added.");

        flow = new CancelOrder("1");
        future = buyerNode.startFlow(flow);
        network.runNetwork();
        assert future.get().contains("Cancel flow for order with ID '1' of buyer '" + buyerNode.getInfo().getLegalIdentities().get(0).getName() + "' executed.");

        flow = new ConfirmOrder("1");
        future = buyerNode.startFlow(flow);
        network.runNetwork();
        future.get();
    }

    @Test(expected = Exception.class)
    public void createOrderHighShippingTest() throws ExecutionException, InterruptedException {
        CreateOrder flow = new CreateOrder("Buyer", "1", 100, 2.0, "10 EUR", "2 EUR0", "Karlsplatz 13, 1040 Wien", "2020-09-30");
        CordaFuture<String> future = sellerNode.startFlow(flow);
        network.runNetwork();
        future.get();
    }
}
