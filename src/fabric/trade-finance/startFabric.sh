#!/bin/bash
#
# Copyright IBM Corp All Rights Reserved
#
# SPDX-License-Identifier: Apache-2.0
#
# Exit on first error
set -e

# don't rewrite paths for Windows Git Bash users
export MSYS_NO_PATHCONV=1
starttime=$(date +%s)
CC_SRC_LANGUAGE=${1:-"typescript"}
CC_SRC_LANGUAGE=`echo "$CC_SRC_LANGUAGE" | tr [:upper:] [:lower:]`

if [ "$CC_SRC_LANGUAGE" = "go" -o "$CC_SRC_LANGUAGE" = "golang" ] ; then
	CC_SRC_PATH="../trade-finance/chaincode/go/"
elif [ "$CC_SRC_LANGUAGE" = "javascript" ]; then
	CC_SRC_PATH="../trade-finance/chaincode/javascript/"
elif [ "$CC_SRC_LANGUAGE" = "java" ]; then
	CC_SRC_PATH="../trade-finance/chaincode/java"
elif [ "$CC_SRC_LANGUAGE" = "typescript" ]; then
	CC_SRC_PATH="../trade-finance/chaincode/typescript/"
else
	echo The chaincode language ${CC_SRC_LANGUAGE} is not supported by this script
	echo Supported chaincode languages are: go, java, javascript, and typescript
	exit 1
fi

# clean out any old identites in the wallets
rm -rf application/javascript/wallet/*
rm -rf application/java/wallet/*
rm -rf application/typescript/wallet/*
rm -rf application/go/wallet/*

# launch network; create channel, add third org and join peer to channel
pushd ../test-network
./network.sh down
./network.sh up createChannel -ca
./network.sh deployCC -ccn trade-finance -ccv 1 -ccl ${CC_SRC_LANGUAGE} -ccp ${CC_SRC_PATH}
popd

cat <<EOF

Total setup execution time : $(($(date +%s) - starttime)) secs ...

Next, use the trade-finance applications to interact with the deployed trade-finance contract.
The trade-finance applications are available in the java directory of the specific company.

Java:

  Start by changing into the "java" directory:
    cd application/{seller|buyer|freight}/java

  Then, install dependencies and run the test using:
    mvn test

	The seller test will invoke the sample client app and perform the following:
	  - Enroll User1.seller and import it into the wallet (if it does not already exist there)
	  - Query all orders
		- Add three new orders
		- Output the added orders
		- Ship the order with id 2 if it got already confirmed

	The buyer test will invoke the sample client app and perform the following:
		- Enroll User1.buyer and import it into the wallet (if it does not already exist there)
		- Query all orders
		- Cancel the order with id 1
		- Confirm the order with id 2
		- Check if the delivery date of the order with 3 passed
		- Sign the arrival of the order with id 2

	The freight test will invoke the sample client app and perform the following:
		- Enroll User1.freight and import it into the wallet (if it does not already exist there)
		- Query all orders
		- Sign the arrival of the order with id 2

EOF
