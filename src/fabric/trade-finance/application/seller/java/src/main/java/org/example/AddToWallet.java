/*
SPDX-License-Identifier: Apache-2.0
*/

package org.example;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;

public class AddToWallet {

  private static X509Certificate readX509Certificate(final Path certificatePath)
      throws IOException, CertificateException {
    try (Reader certificateReader = Files.newBufferedReader(certificatePath, StandardCharsets.UTF_8)) {
      return Identities.readX509Certificate(certificateReader);
    }
  }

  private static PrivateKey getPrivateKey(final Path privateKeyPath) throws IOException, InvalidKeyException {
    try (Reader privateKeyReader = Files.newBufferedReader(privateKeyPath, StandardCharsets.UTF_8)) {
      return Identities.readPrivateKey(privateKeyReader);
    }
  }

  public static void main(final String[] args) {
    try {
      // A wallet stores a collection of identities
      final Path walletPath = Paths.get(".", "wallet");
      final Wallet wallet = Wallets.newFileSystemWallet(walletPath);

      final Path credentialPath = Paths.get("..", "..", "..", "..", "test-network", "organizations",
          "peerOrganizations", "seller.example.com", "users", "User1@seller.example.com", "msp");
      System.out.println("credentialPath: " + credentialPath.toString());
      // final Path certificatePath = credentialPath.resolve(Paths.get("signcerts",
      // "User1@seller.example.com-cert.pem"));
      final Path certificatePath = credentialPath.resolve(Paths.get("signcerts", "cert.pem"));
      System.out.println("certificatePem: " + certificatePath.toString());

      Path privateKeyPath = null;
      try (Stream<Path> paths = Files.find(credentialPath.resolve(Paths.get("keystore")), Integer.MAX_VALUE,
          (path, attrs) -> attrs.isRegularFile() && path.toString().endsWith("_sk"))) {
        privateKeyPath = paths.findAny().get();
      }

      // final Path privateKeyPath = credentialPath.resolve(Paths.get("keystore",
      // "priv_sk"));

      final X509Certificate certificate = readX509Certificate(certificatePath);

      final String identityLabel = new LdapName(certificate.getSubjectX500Principal().getName()).getRdns().stream()
          .filter(i -> i.getType().equalsIgnoreCase("CN")).findFirst().get().getValue().toString();

      final PrivateKey privateKey = getPrivateKey(privateKeyPath);
      final Identity identity = Identities.newX509Identity("SellerMSP", certificate, privateKey);

      wallet.put(identityLabel, identity);

      System.out.println("Write wallet info into " + walletPath.toString() + " successfully.");

    } catch (IOException | CertificateException | InvalidKeyException | InvalidNameException e) {
      System.err.println("Error adding to wallet");
      e.printStackTrace();
    }
  }

}
