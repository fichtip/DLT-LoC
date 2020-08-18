package org.example;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;

public class ClientApp {
	private static final String CONTRACT = "CONTRACT_NAME";
	private static final String CHANNEL = "CHANNEL_NAME";

	public static void main(final String[] args) {
		final Gateway.Builder builder = Gateway.createBuilder();

		String contractName = "trade-finance";
		String channelName = "mychannel";
		// get the name of the contract, in case it is overridden
		final Map<String, String> envvar = System.getenv();
		if (envvar.containsKey(CONTRACT)) {
			contractName = envvar.get(CONTRACT);
		}
		if (envvar.containsKey(CHANNEL)) {
			channelName = envvar.get(CHANNEL);
		}

		try {
			// A wallet stores a collection of identities
			final Path walletPath = Paths.get(".", "wallet");
			final Wallet wallet = Wallets.newFileSystemWallet(walletPath);
			System.out.println("Read wallet info from: " + walletPath);

			final String userName = "user1";

			final Path connectionProfile = Paths.get("..", "..", "..", "..", "test-network", "organizations",
					"peerOrganizations", "buyer.example.com", "connection-buyer.yaml");

			// Set connection options on the gateway builder
			builder.identity(wallet, userName).networkConfig(connectionProfile).discovery(false);

			// Connect to gateway using application specified parameters
			try (Gateway gateway = builder.connect()) {

				// get the network and contract
				final Network network = gateway.getNetwork(channelName);
				final Contract contract = network.getContract(contractName);

				byte[] result;

				result = contract.evaluateTransaction("queryAllOrders");
				System.out.println("List of all orders:");
				System.out.println(new String(result));
				System.out.println("------------------------------------");

				result = contract.evaluateTransaction("queryAllOrders");
				System.out.println("Result of 1st transaction:");
				System.out.println(new String(result));
				System.out.println("------------------------------------");

				contract.submitTransaction("cancelOrder", "1");
				System.out.println("Cancelled order 1");
				result = contract.evaluateTransaction("queryOrder", "1");
				System.out.println(new String(result));
				System.out.println("------------------------------------");

				contract.submitTransaction("confirmOrder", "2");
				System.out.println("Confirmed order 2");
				result = contract.evaluateTransaction("queryOrder", "2");
				System.out.println(new String(result));
				System.out.println("------------------------------------");

				System.out.println("Check if delivery date of order 3 has passed");
				result = contract.submitTransaction("deliveryDatePassed", "3");
				System.out.println(new String(result));
				result = contract.evaluateTransaction("queryOrder", "3");
				System.out.println(new String(result));
				System.out.println("------------------------------------");

				System.out.println("Wait until order with id 2 is set to state SHIPPED");
				result = contract.evaluateTransaction("queryOrder", "2");
				Order order = Order.deserialize(result);
				System.out.println(Order.deserialize(result));
				while (order.getState() != Order.State.SHIPPED) {
					System.out.println("order 2 state is:" + order.getState());
					Thread.sleep(5000);
					result = contract.evaluateTransaction("queryOrder", "2");
					order = Order.deserialize(result);
				}

				contract.submitTransaction("signArrival", "2");
				System.out.println("Signed arrival of order 2");
				result = contract.evaluateTransaction("queryOrder", "2");
				System.out.println(new String(result));
				System.out.println("------------------------------------");
			}
		} catch (GatewayException | IOException | TimeoutException | InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
