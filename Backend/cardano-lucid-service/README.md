# Cardano Lucid Service

A Node.js service that uses Lucid to interact with the Cardano blockchain.

## Setup

1. Install dependencies:
```bash
cd Backend/cardano-lucid-service
npm install
```

2. Configure environment variables in `.env`:
- `CARDANO_NETWORK`: Network to use (preprod, preview, mainnet)
- `BLOCKFROST_PROJECT_ID`: Your Blockfrost project ID
- `WALLET_ADDRESS`: Your Cardano wallet address
- `WALLET_MNEMONIC`: Your 24-word wallet mnemonic

The `.env` file is already configured with your wallet details.

## Running the Service

```bash
npm start
```

The service will start on port 3001 (or the port specified in .env).

## API Endpoints

### Health Check
```
GET /health
```

### Get Wallet Address
```
GET /wallet/address
```

### Get Wallet Balance
```
GET /wallet/balance
```

### Submit Transaction
```
POST /transaction/submit
Content-Type: application/json

{
  "ipfsHash": "Qm...",
  "summaryHash": "abc...",
  "summaries": [
    {
      "bidId": "1",
      "tenderId": "1",
      "bidderName": "Test",
      "validationStatus": "PASSED"
    }
  ]
}
```

### Verify Transaction
```
GET /transaction/verify/:txHash
```

### Get Transaction Details
```
GET /transaction/:txHash
```

### Get UTXOs
```
GET /utxos/:address
```

## Integration with Java Backend

The Java `CardanoService` has been updated to call this Node.js service instead of using cardano-cli. The service URL is configured in `application.properties`:

```properties
lucid.service.url=http://localhost:3001
```

## Flow

1. **Java Backend** calls `/api/scrutiny/final-review`
2. **ScrutinyController** generates summaries and hash
3. **IpfsService** stores data to IPFS (Pinata)
4. **CardanoService** calls Node.js Lucid service to submit transaction
5. **Lucid Service** creates and submits transaction to Cardano
6. **Transaction hash** is returned to Java
7. **BlockchainTransactionService** saves the transaction to database

## Database Storage

Transactions are stored in the `blockchain_transactions` table with:
- `summary_hash`: SHA-256 hash of scrutiny summaries
- `ipfs_hash`: IPFS hash of stored data
- `cardano_transaction_id`: Cardano transaction hash
- `blockchain_url`: Blockfrost URL to view transaction
- `status`: COMPLETED or FAILED
- `created_at`: Timestamp

## Testing

To test the full flow:
1. Start the Node.js service: `cd Backend/cardano-lucid-service && npm start`
2. Start the Java backend: `cd Backend && mvn spring-boot:run`
3. Call: `POST http://localhost:8080/api/scrutiny/final-review`
4. Check database for saved transaction

## Notes

- The wallet must have at least 5 ADA for transactions to work
- Transactions are submitted to the Cardano testnet (preprod) by default
- Make sure the Blockfrost project ID has the correct network permissions
