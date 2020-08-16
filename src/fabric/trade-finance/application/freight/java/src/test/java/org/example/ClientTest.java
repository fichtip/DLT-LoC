/*
SPDX-License-Identifier: Apache-2.0
*/

package org.example;

import org.junit.Test;

public class ClientTest {

	@Test
	public void testTrade() throws Exception {
		AddToWallet.main(null);
		ClientApp.main(null);
	}
}
