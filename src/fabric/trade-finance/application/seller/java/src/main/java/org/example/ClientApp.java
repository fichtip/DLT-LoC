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
					"peerOrganizations", "seller.example.com", "connection-seller.yaml");

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
				// if (false) {
				contract.submitTransaction("createOrder", "1", "100", "2", "10", "2", "Karlsplatz 13, 1040 Wien",
						"2020-09-20");
				contract.submitTransaction("createOrder", "2", "123587", "5", "750", "4", "Ballhausplatz 2, 1010 Wien",
						"2020-12-01");
				contract.submitTransaction("createOrder", "3", "68754", "1", "1337", "2", "Michaelerkuppel, 1010 Wien",
						"2020-08-15");

				result = contract.evaluateTransaction("queryAllOrders");
				System.out.println("List of all orders:");
				System.out.println(new String(result));
				System.out.println("------------------------------------");
				// }
				System.out.println("Wait until order with id 2 is set to state CONFIRMED");
				result = contract.evaluateTransaction("queryOrder", "2");
				Order order = Order.deserialize(result);
				System.out.println(Order.deserialize(result));
				while (order.getState() != Order.State.CONFIRMED) {
					System.out.println("order 2 state is:" + order.getState());
					Thread.sleep(5000);
					result = contract.evaluateTransaction("queryOrder", "2");
					order = Order.deserialize(result);
				}

				contract.submitTransaction("shipOrder", "2", "1AXCAW311");
				System.out.println("shipped order 2");
				result = contract.evaluateTransaction("queryOrder", "2");
				System.out.println(Order.deserialize(result));
				System.out.println("------------------------------------");

			}
		} catch (GatewayException | IOException | TimeoutException | InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
