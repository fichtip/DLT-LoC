

function createSeller {

  echo
	echo "Enroll the CA admin"
  echo
	mkdir -p organizations/peerOrganizations/seller.example.com/

	export FABRIC_CA_CLIENT_HOME=${PWD}/organizations/peerOrganizations/seller.example.com/
#  rm -rf $FABRIC_CA_CLIENT_HOME/fabric-ca-client-config.yaml
#  rm -rf $FABRIC_CA_CLIENT_HOME/msp

  set -x
  fabric-ca-client enroll -u https://admin:adminpw@localhost:7054 --caname ca-seller --tls.certfiles ${PWD}/organizations/fabric-ca/seller/tls-cert.pem
  set +x

  echo 'NodeOUs:
  Enable: true
  ClientOUIdentifier:
    Certificate: cacerts/localhost-7054-ca-seller.pem
    OrganizationalUnitIdentifier: client
  PeerOUIdentifier:
    Certificate: cacerts/localhost-7054-ca-seller.pem
    OrganizationalUnitIdentifier: peer
  AdminOUIdentifier:
    Certificate: cacerts/localhost-7054-ca-seller.pem
    OrganizationalUnitIdentifier: admin
  OrdererOUIdentifier:
    Certificate: cacerts/localhost-7054-ca-seller.pem
    OrganizationalUnitIdentifier: orderer' > ${PWD}/organizations/peerOrganizations/seller.example.com/msp/config.yaml

  echo
  echo "Add affiliation seller"
  echo
  set -x
  fabric-ca-client affiliation add seller --tls.certfiles ${PWD}/organizations/fabric-ca/seller/tls-cert.pem
  set +x

  echo
	echo "Register peer0"
  echo
  set -x
	fabric-ca-client register --caname ca-seller --id.name peer0 --id.secret peer0pw --id.type peer --tls.certfiles ${PWD}/organizations/fabric-ca/seller/tls-cert.pem
  set +x

  echo
  echo "Register user"
  echo
  set -x
  fabric-ca-client register --caname ca-seller --id.name user1 --id.secret user1pw --id.type client --id.affiliation seller --tls.certfiles ${PWD}/organizations/fabric-ca/seller/tls-cert.pem
  set +x

  echo
  echo "Register the org admin"
  echo
  set -x
  fabric-ca-client register --caname ca-seller --id.name selleradmin --id.secret selleradminpw --id.type admin --id.affiliation seller --tls.certfiles ${PWD}/organizations/fabric-ca/seller/tls-cert.pem
  set +x

	mkdir -p organizations/peerOrganizations/seller.example.com/peers
  mkdir -p organizations/peerOrganizations/seller.example.com/peers/peer0.seller.example.com

  echo
  echo "## Generate the peer0 msp"
  echo
  set -x
	fabric-ca-client enroll -u https://peer0:peer0pw@localhost:7054 --caname ca-seller -M ${PWD}/organizations/peerOrganizations/seller.example.com/peers/peer0.seller.example.com/msp --csr.hosts peer0.seller.example.com --tls.certfiles ${PWD}/organizations/fabric-ca/seller/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/seller.example.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/seller.example.com/peers/peer0.seller.example.com/msp/config.yaml

  echo
  echo "## Generate the peer0-tls certificates"
  echo
  set -x
  fabric-ca-client enroll -u https://peer0:peer0pw@localhost:7054 --caname ca-seller -M ${PWD}/organizations/peerOrganizations/seller.example.com/peers/peer0.seller.example.com/tls --enrollment.profile tls --csr.hosts peer0.seller.example.com --csr.hosts localhost --tls.certfiles ${PWD}/organizations/fabric-ca/seller/tls-cert.pem
  set +x


  cp ${PWD}/organizations/peerOrganizations/seller.example.com/peers/peer0.seller.example.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/seller.example.com/peers/peer0.seller.example.com/tls/ca.crt
  cp ${PWD}/organizations/peerOrganizations/seller.example.com/peers/peer0.seller.example.com/tls/signcerts/* ${PWD}/organizations/peerOrganizations/seller.example.com/peers/peer0.seller.example.com/tls/server.crt
  cp ${PWD}/organizations/peerOrganizations/seller.example.com/peers/peer0.seller.example.com/tls/keystore/* ${PWD}/organizations/peerOrganizations/seller.example.com/peers/peer0.seller.example.com/tls/server.key

  mkdir ${PWD}/organizations/peerOrganizations/seller.example.com/msp/tlscacerts
  cp ${PWD}/organizations/peerOrganizations/seller.example.com/peers/peer0.seller.example.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/seller.example.com/msp/tlscacerts/ca.crt

  mkdir ${PWD}/organizations/peerOrganizations/seller.example.com/tlsca
  cp ${PWD}/organizations/peerOrganizations/seller.example.com/peers/peer0.seller.example.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/seller.example.com/tlsca/tlsca.seller.example.com-cert.pem

  mkdir ${PWD}/organizations/peerOrganizations/seller.example.com/ca
  cp ${PWD}/organizations/peerOrganizations/seller.example.com/peers/peer0.seller.example.com/msp/cacerts/* ${PWD}/organizations/peerOrganizations/seller.example.com/ca/ca.seller.example.com-cert.pem

  mkdir -p organizations/peerOrganizations/seller.example.com/users
  mkdir -p organizations/peerOrganizations/seller.example.com/users/User1@seller.example.com

  echo
  echo "## Generate the user msp"
  echo
  set -x
	fabric-ca-client enroll -u https://user1:user1pw@localhost:7054 --caname ca-seller -M ${PWD}/organizations/peerOrganizations/seller.example.com/users/User1@seller.example.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/seller/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/seller.example.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/seller.example.com/users/User1@seller.example.com/msp/config.yaml

  mkdir -p organizations/peerOrganizations/seller.example.com/users/Admin@seller.example.com

  echo
  echo "## Generate the org admin msp"
  echo
  set -x
	fabric-ca-client enroll -u https://selleradmin:selleradminpw@localhost:7054 --caname ca-seller -M ${PWD}/organizations/peerOrganizations/seller.example.com/users/Admin@seller.example.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/seller/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/seller.example.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/seller.example.com/users/Admin@seller.example.com/msp/config.yaml

}


function createBuyer {

  echo
	echo "Enroll the CA admin"
  echo
	mkdir -p organizations/peerOrganizations/buyer.example.com/

	export FABRIC_CA_CLIENT_HOME=${PWD}/organizations/peerOrganizations/buyer.example.com/
#  rm -rf $FABRIC_CA_CLIENT_HOME/fabric-ca-client-config.yaml
#  rm -rf $FABRIC_CA_CLIENT_HOME/msp

  set -x
  fabric-ca-client enroll -u https://admin:adminpw@localhost:9054 --caname ca-buyer --tls.certfiles ${PWD}/organizations/fabric-ca/buyer/tls-cert.pem
  set +x

  echo 'NodeOUs:
  Enable: true
  ClientOUIdentifier:
    Certificate: cacerts/localhost-9054-ca-buyer.pem
    OrganizationalUnitIdentifier: client
  PeerOUIdentifier:
    Certificate: cacerts/localhost-9054-ca-buyer.pem
    OrganizationalUnitIdentifier: peer
  AdminOUIdentifier:
    Certificate: cacerts/localhost-9054-ca-buyer.pem
    OrganizationalUnitIdentifier: admin
  OrdererOUIdentifier:
    Certificate: cacerts/localhost-9054-ca-buyer.pem
    OrganizationalUnitIdentifier: orderer' > ${PWD}/organizations/peerOrganizations/buyer.example.com/msp/config.yaml

  echo
  echo "Add affiliation buyer"
  echo
  set -x
  fabric-ca-client affiliation add buyer --tls.certfiles ${PWD}/organizations/fabric-ca/buyer/tls-cert.pem
  set +x

  echo
	echo "Register peer0"
  echo
  set -x
	fabric-ca-client register --caname ca-buyer --id.name peer0 --id.secret peer0pw --id.type peer --tls.certfiles ${PWD}/organizations/fabric-ca/buyer/tls-cert.pem
  set +x

  echo
  echo "Register user"
  echo
  set -x
  fabric-ca-client register --caname ca-buyer --id.name user1 --id.secret user1pw --id.type client --id.affiliation buyer --tls.certfiles ${PWD}/organizations/fabric-ca/buyer/tls-cert.pem
  set +x

  echo
  echo "Register the org admin"
  echo
  set -x
  fabric-ca-client register --caname ca-buyer --id.name buyeradmin --id.secret buyeradminpw --id.type admin --id.affiliation buyer --tls.certfiles ${PWD}/organizations/fabric-ca/buyer/tls-cert.pem
  set +x

	mkdir -p organizations/peerOrganizations/buyer.example.com/peers
  mkdir -p organizations/peerOrganizations/buyer.example.com/peers/peer0.buyer.example.com

  echo
  echo "## Generate the peer0 msp"
  echo
  set -x
	fabric-ca-client enroll -u https://peer0:peer0pw@localhost:9054 --caname ca-buyer -M ${PWD}/organizations/peerOrganizations/buyer.example.com/peers/peer0.buyer.example.com/msp --csr.hosts peer0.buyer.example.com --tls.certfiles ${PWD}/organizations/fabric-ca/buyer/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/buyer.example.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/buyer.example.com/peers/peer0.buyer.example.com/msp/config.yaml

  echo
  echo "## Generate the peer0-tls certificates"
  echo
  set -x
  fabric-ca-client enroll -u https://peer0:peer0pw@localhost:9054 --caname ca-buyer -M ${PWD}/organizations/peerOrganizations/buyer.example.com/peers/peer0.buyer.example.com/tls --enrollment.profile tls --csr.hosts peer0.buyer.example.com --csr.hosts localhost --tls.certfiles ${PWD}/organizations/fabric-ca/buyer/tls-cert.pem
  set +x


  cp ${PWD}/organizations/peerOrganizations/buyer.example.com/peers/peer0.buyer.example.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/buyer.example.com/peers/peer0.buyer.example.com/tls/ca.crt
  cp ${PWD}/organizations/peerOrganizations/buyer.example.com/peers/peer0.buyer.example.com/tls/signcerts/* ${PWD}/organizations/peerOrganizations/buyer.example.com/peers/peer0.buyer.example.com/tls/server.crt
  cp ${PWD}/organizations/peerOrganizations/buyer.example.com/peers/peer0.buyer.example.com/tls/keystore/* ${PWD}/organizations/peerOrganizations/buyer.example.com/peers/peer0.buyer.example.com/tls/server.key

  mkdir ${PWD}/organizations/peerOrganizations/buyer.example.com/msp/tlscacerts
  cp ${PWD}/organizations/peerOrganizations/buyer.example.com/peers/peer0.buyer.example.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/buyer.example.com/msp/tlscacerts/ca.crt

  mkdir ${PWD}/organizations/peerOrganizations/buyer.example.com/tlsca
  cp ${PWD}/organizations/peerOrganizations/buyer.example.com/peers/peer0.buyer.example.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/buyer.example.com/tlsca/tlsca.buyer.example.com-cert.pem

  mkdir ${PWD}/organizations/peerOrganizations/buyer.example.com/ca
  cp ${PWD}/organizations/peerOrganizations/buyer.example.com/peers/peer0.buyer.example.com/msp/cacerts/* ${PWD}/organizations/peerOrganizations/buyer.example.com/ca/ca.buyer.example.com-cert.pem

  mkdir -p organizations/peerOrganizations/buyer.example.com/users
  mkdir -p organizations/peerOrganizations/buyer.example.com/users/User1@buyer.example.com

  echo
  echo "## Generate the user msp"
  echo
  set -x
	fabric-ca-client enroll -u https://user1:user1pw@localhost:9054 --caname ca-buyer -M ${PWD}/organizations/peerOrganizations/buyer.example.com/users/User1@buyer.example.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/buyer/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/buyer.example.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/buyer.example.com/users/User1@buyer.example.com/msp/config.yaml

  mkdir -p organizations/peerOrganizations/buyer.example.com/users/Admin@buyer.example.com

  echo
  echo "## Generate the org admin msp"
  echo
  set -x
	fabric-ca-client enroll -u https://buyeradmin:buyeradminpw@localhost:9054 --caname ca-buyer -M ${PWD}/organizations/peerOrganizations/buyer.example.com/users/Admin@buyer.example.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/buyer/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/buyer.example.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/buyer.example.com/users/Admin@buyer.example.com/msp/config.yaml

}

function createFreight {

  echo
	echo "Enroll the CA admin"
  echo
	mkdir -p organizations/peerOrganizations/freight.example.com/

	export FABRIC_CA_CLIENT_HOME=${PWD}/organizations/peerOrganizations/freight.example.com/
#  rm -rf $FABRIC_CA_CLIENT_HOME/fabric-ca-client-config.yaml
#  rm -rf $FABRIC_CA_CLIENT_HOME/msp

  set -x
  fabric-ca-client enroll -u https://admin:adminpw@localhost:11054 --caname ca-freight --tls.certfiles ${PWD}/organizations/fabric-ca/freight/tls-cert.pem
  set +x

  echo 'NodeOUs:
  Enable: true
  ClientOUIdentifier:
    Certificate: cacerts/localhost-11054-ca-freight.pem
    OrganizationalUnitIdentifier: client
  PeerOUIdentifier:
    Certificate: cacerts/localhost-11054-ca-freight.pem
    OrganizationalUnitIdentifier: peer
  AdminOUIdentifier:
    Certificate: cacerts/localhost-11054-ca-freight.pem
    OrganizationalUnitIdentifier: admin
  OrdererOUIdentifier:
    Certificate: cacerts/localhost-11054-ca-freight.pem
    OrganizationalUnitIdentifier: orderer' > ${PWD}/organizations/peerOrganizations/freight.example.com/msp/config.yaml

  echo
  echo "Add affiliation freight"
  echo
  set -x
  fabric-ca-client affiliation add freight --tls.certfiles ${PWD}/organizations/fabric-ca/freight/tls-cert.pem
  set +x


  echo
	echo "Register peer0"
  echo
  set -x
	fabric-ca-client register --caname ca-freight --id.name peer0 --id.secret peer0pw --id.type peer --tls.certfiles ${PWD}/organizations/fabric-ca/freight/tls-cert.pem
  set +x

  echo
  echo "Register user"
  echo
  set -x
  fabric-ca-client register --caname ca-freight --id.name user1 --id.secret user1pw --id.type client --id.affiliation freight --tls.certfiles ${PWD}/organizations/fabric-ca/freight/tls-cert.pem
  set +x

  echo
  echo "Register the org admin"
  echo
  set -x
  fabric-ca-client register --caname ca-freight --id.name freightadmin --id.secret freightadminpw --id.type admin --id.affiliation freight --tls.certfiles ${PWD}/organizations/fabric-ca/freight/tls-cert.pem
  set +x

	mkdir -p organizations/peerOrganizations/freight.example.com/peers
  mkdir -p organizations/peerOrganizations/freight.example.com/peers/peer0.freight.example.com

  echo
  echo "## Generate the peer0 msp"
  echo
  set -x
	fabric-ca-client enroll -u https://peer0:peer0pw@localhost:11054 --caname ca-freight -M ${PWD}/organizations/peerOrganizations/freight.example.com/peers/peer0.freight.example.com/msp --csr.hosts peer0.freight.example.com --tls.certfiles ${PWD}/organizations/fabric-ca/freight/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/freight.example.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/freight.example.com/peers/peer0.freight.example.com/msp/config.yaml

  echo
  echo "## Generate the peer0-tls certificates"
  echo
  set -x
  fabric-ca-client enroll -u https://peer0:peer0pw@localhost:11054 --caname ca-freight -M ${PWD}/organizations/peerOrganizations/freight.example.com/peers/peer0.freight.example.com/tls --enrollment.profile tls --csr.hosts peer0.freight.example.com --csr.hosts localhost --tls.certfiles ${PWD}/organizations/fabric-ca/freight/tls-cert.pem
  set +x


  cp ${PWD}/organizations/peerOrganizations/freight.example.com/peers/peer0.freight.example.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/freight.example.com/peers/peer0.freight.example.com/tls/ca.crt
  cp ${PWD}/organizations/peerOrganizations/freight.example.com/peers/peer0.freight.example.com/tls/signcerts/* ${PWD}/organizations/peerOrganizations/freight.example.com/peers/peer0.freight.example.com/tls/server.crt
  cp ${PWD}/organizations/peerOrganizations/freight.example.com/peers/peer0.freight.example.com/tls/keystore/* ${PWD}/organizations/peerOrganizations/freight.example.com/peers/peer0.freight.example.com/tls/server.key

  mkdir ${PWD}/organizations/peerOrganizations/freight.example.com/msp/tlscacerts
  cp ${PWD}/organizations/peerOrganizations/freight.example.com/peers/peer0.freight.example.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/freight.example.com/msp/tlscacerts/ca.crt

  mkdir ${PWD}/organizations/peerOrganizations/freight.example.com/tlsca
  cp ${PWD}/organizations/peerOrganizations/freight.example.com/peers/peer0.freight.example.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/freight.example.com/tlsca/tlsca.freight.example.com-cert.pem

  mkdir ${PWD}/organizations/peerOrganizations/freight.example.com/ca
  cp ${PWD}/organizations/peerOrganizations/freight.example.com/peers/peer0.freight.example.com/msp/cacerts/* ${PWD}/organizations/peerOrganizations/freight.example.com/ca/ca.freight.example.com-cert.pem

  mkdir -p organizations/peerOrganizations/freight.example.com/users
  mkdir -p organizations/peerOrganizations/freight.example.com/users/User1@freight.example.com

  echo
  echo "## Generate the user msp"
  echo
  set -x
	fabric-ca-client enroll -u https://user1:user1pw@localhost:11054 --caname ca-freight -M ${PWD}/organizations/peerOrganizations/freight.example.com/users/User1@freight.example.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/freight/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/freight.example.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/freight.example.com/users/User1@freight.example.com/msp/config.yaml

  mkdir -p organizations/peerOrganizations/freight.example.com/users/Admin@freight.example.com

  echo
  echo "## Generate the org admin msp"
  echo
  set -x
	fabric-ca-client enroll -u https://freightadmin:freightadminpw@localhost:11054 --caname ca-freight -M ${PWD}/organizations/peerOrganizations/freight.example.com/users/Admin@freight.example.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/freight/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/freight.example.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/freight.example.com/users/Admin@freight.example.com/msp/config.yaml

}

function createOrderer {

  echo
	echo "Enroll the CA admin"
  echo
	mkdir -p organizations/ordererOrganizations/example.com

	export FABRIC_CA_CLIENT_HOME=${PWD}/organizations/ordererOrganizations/example.com
#  rm -rf $FABRIC_CA_CLIENT_HOME/fabric-ca-client-config.yaml
#  rm -rf $FABRIC_CA_CLIENT_HOME/msp

  set -x
  fabric-ca-client enroll -u https://admin:adminpw@localhost:8054 --caname ca-orderer --tls.certfiles ${PWD}/organizations/fabric-ca/ordererOrg/tls-cert.pem
  set +x

  echo 'NodeOUs:
  Enable: true
  ClientOUIdentifier:
    Certificate: cacerts/localhost-8054-ca-orderer.pem
    OrganizationalUnitIdentifier: client
  PeerOUIdentifier:
    Certificate: cacerts/localhost-8054-ca-orderer.pem
    OrganizationalUnitIdentifier: peer
  AdminOUIdentifier:
    Certificate: cacerts/localhost-8054-ca-orderer.pem
    OrganizationalUnitIdentifier: admin
  OrdererOUIdentifier:
    Certificate: cacerts/localhost-8054-ca-orderer.pem
    OrganizationalUnitIdentifier: orderer' > ${PWD}/organizations/ordererOrganizations/example.com/msp/config.yaml


  echo
	echo "Register orderer"
  echo
  set -x
	fabric-ca-client register --caname ca-orderer --id.name orderer --id.secret ordererpw --id.type orderer --tls.certfiles ${PWD}/organizations/fabric-ca/ordererOrg/tls-cert.pem
    set +x

  echo
  echo "Register the orderer admin"
  echo
  set -x
  fabric-ca-client register --caname ca-orderer --id.name ordererAdmin --id.secret ordererAdminpw --id.type admin --tls.certfiles ${PWD}/organizations/fabric-ca/ordererOrg/tls-cert.pem
  set +x

	mkdir -p organizations/ordererOrganizations/example.com/orderers
  mkdir -p organizations/ordererOrganizations/example.com/orderers/example.com

  mkdir -p organizations/ordererOrganizations/example.com/orderers/orderer.example.com

  echo
  echo "## Generate the orderer msp"
  echo
  set -x
	fabric-ca-client enroll -u https://orderer:ordererpw@localhost:8054 --caname ca-orderer -M ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp --csr.hosts orderer.example.com --csr.hosts localhost --tls.certfiles ${PWD}/organizations/fabric-ca/ordererOrg/tls-cert.pem
  set +x

  cp ${PWD}/organizations/ordererOrganizations/example.com/msp/config.yaml ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/config.yaml

  echo
  echo "## Generate the orderer-tls certificates"
  echo
  set -x
  fabric-ca-client enroll -u https://orderer:ordererpw@localhost:8054 --caname ca-orderer -M ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls --enrollment.profile tls --csr.hosts orderer.example.com --csr.hosts localhost --tls.certfiles ${PWD}/organizations/fabric-ca/ordererOrg/tls-cert.pem
  set +x

  cp ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/tlscacerts/* ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/ca.crt
  cp ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/signcerts/* ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/server.crt
  cp ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/keystore/* ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/server.key

  mkdir ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts
  cp ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/tlscacerts/* ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem

  mkdir ${PWD}/organizations/ordererOrganizations/example.com/msp/tlscacerts
  cp ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/tlscacerts/* ${PWD}/organizations/ordererOrganizations/example.com/msp/tlscacerts/tlsca.example.com-cert.pem

  mkdir -p organizations/ordererOrganizations/example.com/users
  mkdir -p organizations/ordererOrganizations/example.com/users/Admin@example.com

  echo
  echo "## Generate the admin msp"
  echo
  set -x
	fabric-ca-client enroll -u https://ordererAdmin:ordererAdminpw@localhost:8054 --caname ca-orderer -M ${PWD}/organizations/ordererOrganizations/example.com/users/Admin@example.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/ordererOrg/tls-cert.pem
  set +x

  cp ${PWD}/organizations/ordererOrganizations/example.com/msp/config.yaml ${PWD}/organizations/ordererOrganizations/example.com/users/Admin@example.com/msp/config.yaml


}
