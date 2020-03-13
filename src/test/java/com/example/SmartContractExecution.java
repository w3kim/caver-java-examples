package com.example;

import com.klaytn.caver.Caver;
import com.klaytn.caver.crypto.KlayCredentials;
import com.klaytn.caver.methods.response.KlayTransactionReceipt;
import com.klaytn.caver.tx.gas.DefaultGasProvider;
import com.klaytn.caver.tx.manager.PollingTransactionReceiptProcessor;
import com.klaytn.caver.tx.manager.TransactionManager;
import com.klaytn.caver.tx.model.SmartContractExecutionTransaction;
import com.klaytn.caver.utils.ChainId;
import org.junit.Before;
import org.junit.Test;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;

public class SmartContractExecution {

    private static final int CHAIN_ID = ChainId.BAOBAB_TESTNET;
    private static final DefaultGasProvider GAS_PROVIDER = new DefaultGasProvider();
    private static final BigInteger GAS_LIMIT = GAS_PROVIDER.getGasLimit();

    private Caver caver;
    private KlayCredentials sender;

    private String contractAddress = "0x069dc518E1d41A7C9F7bEbAA222f4652a6B27307";
    private long amount = 1_000_000;
    private String recipient = "0x8f8196f8719904974292f678febcdef97d5842a8";

    @Before
    public void setup() {
        sender = KlayCredentials.create("0xcde47f7c9da383c1c32b9cde6a8ce3752bbf38b43dd4ac89cb0405e7f846458c");
        caver = Caver.build("https://api.baobab.klaytn.net:8651");
    }

    @Test
    public void erc20_ManualTransfer() {
        try {
            Function function = new Function(
                    "transfer",
                    Arrays.asList(
                            new Address(recipient),
                            new Uint256(amount)
                    ),
                    Arrays.asList(new TypeReference<Bool>() {
                    }));

            // Using web3j FunctionEncoder is fine
            String functionCallData = FunctionEncoder.encode(function);

            // Allow TransactionManager to handle nonce and signing
            TransactionManager manager = new TransactionManager.Builder(caver, sender)
                    .setTransactionReceiptProcessor(new PollingTransactionReceiptProcessor(caver, 1000, 10))
                    .setChaindId(CHAIN_ID)
                    .build();

            KlayTransactionReceipt.TransactionReceipt receipt = manager.executeTransaction(
                    SmartContractExecutionTransaction.create(sender.getAddress(),
                            contractAddress,
                            BigInteger.ZERO, // value should set to zero unless you are transferring KLAY to the contract
                            Numeric.hexStringToByteArray(functionCallData), // use Numeric.hexStringToByteArray for safe conversion
                            GAS_LIMIT)
            );

            System.out.println(receipt.getStatus()); // 0x1
        } catch (Exception e) {
            // handle exceptions
        }

    }

    @Test
    public void erc20_TransferUsingGeneratedClass() {
        try {
            // Alternative
            MyERC20 contract = MyERC20.load(contractAddress, caver, sender, CHAIN_ID, GAS_PROVIDER);
            KlayTransactionReceipt.TransactionReceipt receipt = contract.transfer(recipient, new BigInteger(Long.toString(amount))).send();
            System.out.println(receipt.getStatus());
        } catch (Exception e) {

        }
    }
}
