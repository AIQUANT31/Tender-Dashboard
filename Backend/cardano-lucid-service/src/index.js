import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';
import { LucidService } from './lucidService.js';

// Load .env from the parent directory (cardano-lucid-service root)
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
dotenv.config({ path: path.resolve(__dirname, '..', '.env') });

// Debug: Log loaded configuration (masked)
const loadedProjectId = process.env.BLOCKFROST_PROJECT_ID;
console.log('Blockfrost Project ID loaded:', loadedProjectId ? loadedProjectId.substring(0, 10) + '...' : 'NOT LOADED');
console.log('Cardano Network:', process.env.CARDANO_NETWORK);

const app = express();
const port = process.env.PORT || 3001;

app.use(cors());
app.use(express.json());

const lucidService = new LucidService();

// Health check endpoint
app.get('/health', (req, res) => {
  const projectId = process.env.BLOCKFROST_PROJECT_ID;
  res.json({ 
    status: 'ok', 
    service: 'cardano-lucid-service',
    network: process.env.CARDANO_NETWORK,
    projectIdLoaded: !!projectId,
    projectIdPrefix: projectId ? projectId.substring(0, 10) + '...' : 'NOT LOADED'
  });
});

// Get wallet address   
app.get('/wallet/address', (req, res) => {
  try {
    const address = lucidService.getWalletAddress();
    res.json({ success: true, address });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// Get wallet balance
app.get('/wallet/balance', async (req, res) => {
  try {
    const balance = await lucidService.getBalance();
    res.json({ success: true, balance });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// Submit transaction with metadata
app.post('/transaction/submit', async (req, res) => {
  try {
    const { ipfsHash, summaryHash, summaries } = req.body;
    
    if (!ipfsHash) {
      return res.status(400).json({ 
        success: false, 
        message: 'IPFS hash is required' 
      });
    }

    const result = await lucidService.submitTransaction(ipfsHash, summaryHash, summaries || []);
    res.json(result);
  } catch (error) {
    console.error('Transaction error:', error);
    res.status(500).json({ 
      success: false, 
      message: error.message,
      error: error.toString()
    });
  }
});

// Verify transaction
app.get('/transaction/verify/:txHash', async (req, res) => {
  try {
    const { txHash } = req.params;
    const result = await lucidService.verifyTransaction(txHash);
    res.json(result);
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// Get transaction details
app.get('/transaction/:txHash', async (req, res) => {
  try {
    const { txHash } = req.params;
    const result = await lucidService.getTransaction(txHash);
    res.json(result);
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// Get UTXOs for address
app.get('/utxos/:address', async (req, res) => {
  try {
    const { address } = req.params;
    const utxos = await lucidService.getUtxos(address);
    res.json({ success: true, utxos });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

app.listen(port, () => {
  console.log(`Cardano Lucid Service running on port ${port}`);
});

