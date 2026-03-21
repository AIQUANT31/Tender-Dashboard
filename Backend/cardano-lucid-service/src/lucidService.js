import { Lucid, Blockfrost } from "lucid-cardano";

class RetryableBlockfrost extends Blockfrost {
  constructor(url, projectId, options = {}) {
    super(url, projectId);
    this.maxRetries = options.maxRetries || 3;
    this.retryDelay = options.retryDelay || 2000;
  }

  async _retryRequest(fn, ...args) {
    let lastError;
    for (let attempt = 1; attempt <= this.maxRetries; attempt++) {
      try {
        return await fn(...args);
      } catch (error) {
        lastError = error;
        console.log(`Attempt ${attempt}/${this.maxRetries} failed: ${error.message}`);
        if (attempt < this.maxRetries) {
          console.log(`Retrying in ${this.retryDelay}ms...`);
          await new Promise(resolve => setTimeout(resolve, this.retryDelay));
        }
      }
    }                     
    throw lastError; 
  }

  async getUtxos(address) {
    return this._retryRequest(super.getUtxos.bind(this), address);
  }

  async getUtxo(txHash, outputIndex) {
    return this._retryRequest(super.getUtxo.bind(this), txHash, outputIndex);
  }

  async submitTx(tx) {
    return this._retryRequest(super.submitTx.bind(this), tx);
  }
}

export class LucidService {
  constructor() {
    this.network = process.env.CARDANO_NETWORK || 'preprod';
    this.blockfrostProjectId = process.env.BLOCKFROST_PROJECT_ID;
    
    // Validate Blockfrost Project ID  
    if (!this.blockfrostProjectId) {
      console.error('ERROR: BLOCKFROST_PROJECT_ID is not set in environment variables!');
      console.error('Please add BLOCKFROST_PROJECT_ID to your .env file');
    } else if (this.blockfrostProjectId.length < 20) {
      console.error('ERROR: BLOCKFROST_PROJECT_ID appears to be invalid (too short):', this.blockfrostProjectId);
    } else {
      console.log('Blockfrost Project ID loaded:', this.blockfrostProjectId.substring(0, 10) + '...');
    }
    
    this.walletAddress = process.env.WALLET_ADDRESS || 'addr_test1vzr47p0lheukvq48lkcfyp4zmpazms9vm2xgn3w7kqn8cyg0texev';
    this.walletMnemonic = process.env.WALLET_MNEMONIC || '';
    this.metadataLabel = 679;
    
    this.lucid = null;
    this.initPromise = this.init();
  }

  async init() {
    try {
      this.lucid = await Lucid.new(
        new RetryableBlockfrost(
          this.network === 'mainnet' 
            ? 'https://cardano-mainnet.blockfrost.io/api/v0'
            : 'https://cardano-preprod.blockfrost.io/api/v0',
          this.blockfrostProjectId,
          { maxRetries: 3, retryDelay: 2000 }
        ),
        this.network === 'mainnet' ? 'Mainnet' : 'Preprod'
      );
      
      if (this.walletMnemonic) {
        this.lucid.selectWalletFromSeed(this.walletMnemonic);
        // Update walletAddress to the seed-derived address
        this.walletAddress = await this.lucid.wallet.address();
        console.log('Lucid initialized with mnemonic, derived address:', this.walletAddress);
      } else {
        console.log('Lucid initialized without mnemonic - transactions will fail (no signing key)');
      }
      
      console.log('Network:', this.network);
    } catch (error) {
      console.error('Error initializing Lucid:', error);
      throw error;
    }
  }

  /** Ensure Lucid is fully initialized before any operation */
  async ensureInit() {
    await this.initPromise;
    if (!this.lucid) {
      throw new Error('Lucid failed to initialize');
    }
    if (!this.blockfrostProjectId) {
      throw new Error('BLOCKFROST_PROJECT_ID is not configured. Please add it to your .env file.');
    }
    if (this.blockfrostProjectId.length < 20) {
      throw new Error('BLOCKFROST_PROJECT_ID appears to be invalid. Please check your .env file.');
    }
  }

  getWalletAddress() {
    return this.walletAddress;
  }

  async getBalance() {
    try {
      // Use Blockfrost API directly for balance
      const blockfrostUrl = this.network === 'mainnet' 
        ? 'https://cardano-mainnet.blockfrost.io/api/v0'
        : 'https://cardano-preprod.blockfrost.io/api/v0';
      
      const response = await fetch(`${blockfrostUrl}/addresses/${this.walletAddress}`, {
        headers: { 'project_id': this.blockfrostProjectId }
      });
      
      if (!response.ok) {
        throw new Error('Failed to get balance');
      }
      
      const data = await response.json();
      const balance = data.amount.find(a => a.unit === 'lovelace');
      return balance ? balance.quantity : '0';
    } catch (error) {
      console.error('Error getting balance:', error);
      throw error;
    }
  }

  async getUtxos(address) {
    try {
      const blockfrostUrl = this.network === 'mainnet' 
        ? 'https://cardano-mainnet.blockfrost.io/api/v0'
        : 'https://cardano-preprod.blockfrost.io/api/v0';
      
      const response = await fetch(`${blockfrostUrl}/addresses/${address}/utxos`, {
        headers: { 'project_id': this.blockfrostProjectId }
      });
      
      if (!response.ok) {
        throw new Error('Failed to get UTXOs');
      }
      
      const utxos = await response.json();
      return utxos.map(utxo => ({
        txHash: utxo.tx_hash,
        outputIndex: utxo.output_index,
        lovelace: utxo.amount.find(a => a.unit === 'lovelace')?.quantity || '0',
        assets: this.convertUtxoAssets(utxo.amount)
      }));
    } catch (error) {
      console.error('Error getting UTXOs:', error);
      throw error;
    }
  }

  convertAssetsToString(assets) {
    const result = {};
    for (const [key, value] of Object.entries(assets)) {
      if (typeof value === 'bigint') {
        result[key] = value.toString();
      } else {
        result[key] = value;
      }
    }
    return result;
  }

  createMetadata(ipfsHash, summaryHash, summaries) {
    // Return only the metadata VALUE — attachMetadata(label, value) wraps it under the label
    const metadata = {
      ipfs_hash: ipfsHash,
      summary_hash: summaryHash || '',
      timestamp: new Date().toISOString(),
      // network: `cardano-${this.network}`,
      summaries: summaries.map(s => ({
        bid_id: s.bidId || s.bid_id || '',
        tender_id: s.tenderId || s.tender_id || '',
        bidder_name: s.bidderName || s.bidder_name || '',
        // status: s.validationStatus || s.status || ''
      }))
    };
    return metadata;
  }

  
  async submitTransaction(ipfsHash, summaryHash, summaries = []) {
    const response = {
      success: false,
      transactionId: null,
      metadataUrl: null,
      message: ''
    };

    try {
      // Ensure Lucid is fully initialized before proceeding
      await this.ensureInit();

      console.log('Starting transaction submission...');

      // Create metadata
      const metadata = this.createMetadata(ipfsHash, summaryHash, summaries);
      console.log('Metadata created:', JSON.stringify(metadata));

      // Get UTXOs using Lucid's built-in method
      const utxos = await this.lucid.utxosAt(this.walletAddress);
      
      if (!utxos || utxos.length === 0) {
        response.message = 'No UTXOs found in wallet';
        return response;
      }

      console.log(`Found ${utxos.length} UTXOs`);

      // Find UTXO with sufficient funds (at least 5 ADA for metadata transaction)
      const selectedUtxo = utxos.find(utxo => {
        let lovelace = 0n;
        if (utxo.assets && typeof utxo.assets.get === 'function') {
          lovelace = utxo.assets.get('lovelace') || 0n;
        } else if (utxo.assets && typeof utxo.assets === 'object') {
          // Handle Map-like objects
          lovelace = BigInt(utxo.assets.lovelace || utxo.assets['lovelace'] || 0);
        }
        return lovelace >= 5000000n;
      });

      if (!selectedUtxo) {
        response.message = 'No UTXO with sufficient funds (need at least 5 ADA)';
        return response;
      }

      // Get the lovelace amount from selected UTXO
      let selectedUtxoLovelace = 0n;
      if (selectedUtxo.assets && typeof selectedUtxo.assets.get === 'function') {
        selectedUtxoLovelace = selectedUtxo.assets.get('lovelace') || 0n;
      } else if (selectedUtxo.assets && typeof selectedUtxo.assets === 'object') {
        selectedUtxoLovelace = BigInt(selectedUtxo.assets.lovelace || selectedUtxo.assets['lovelace'] || 0);
      }

      // Build metadata transaction that spends the selected UTXO
      const tx = await this.lucid.newTx()
        .collectFrom([selectedUtxo])
        .attachMetadata(this.metadataLabel, metadata)
        .payToAddress(this.walletAddress, { lovelace: selectedUtxoLovelace - 2000000n })
        .complete();

      console.log('Transaction built');

      // Sign transaction
      const signedTx = await tx.sign().complete();
      console.log('Transaction signed');

      // Submit the signed transaction
      const txHash = await signedTx.submit();
      console.log('Transaction submitted! Hash:', txHash);

      response.success = true;
      response.transactionId = txHash;
      response.metadataUrl = `https://cardano-${this.network}.blockfrost.io/api/v0/txs/${txHash}`;
      response.message = `Transaction submitted successfully to Cardano ${this.network}`;
      response.metadata = metadata;

    } catch (error) {
      console.error('Error submitting transaction:', error);
      response.message = 'Error: ' + error.message;
      response.error = error.toString();
    }

    return response;
  }

  // Get UTXOs with retry logic
   
  async getUtxosWithRetry(address, maxRetries = 3, retryDelay = 2000) {
    const blockfrostUrl = this.network === 'mainnet' 
      ? 'https://cardano-mainnet.blockfrost.io/api/v0'
      : 'https://cardano-preprod.blockfrost.io/api/v0';
    
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        const response = await fetch(`${blockfrostUrl}/addresses/${address}/utxos`, {
          headers: { 'project_id': this.blockfrostProjectId }
        });
        
        if (!response.ok) {
          throw new Error(`Failed to get UTXOs: ${response.status} ${response.statusText}`);
        }
        
        const utxos = await response.json();
        return utxos.map(utxo => ({
          txHash: utxo.tx_hash,
          outputIndex: utxo.output_index,
          lovelace: utxo.amount.find(a => a.unit === 'lovelace')?.quantity || '0',
          assets: this.convertUtxoAssets(utxo.amount)
        }));
      } catch (error) {
        console.log(`Attempt ${attempt}/${maxRetries} failed: ${error.message}`);
        if (attempt < maxRetries) {
          console.log(`Retrying in ${retryDelay}ms...`);
          await new Promise(resolve => setTimeout(resolve, retryDelay));
        } else {
          throw error;
        }
      }
    }
  }

  // Submit transaction with retry logic
  async submitTxWithRetry(tx, maxRetries = 3, retryDelay = 2000) {
    const blockfrostUrl = this.network === 'mainnet' 
      ? 'https://cardano-mainnet.blockfrost.io/api/v0'
      : 'https://cardano-preprod.blockfrost.io/api/v0';
    
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        const response = await fetch(`${blockfrostUrl}/tx/submit`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/cbor',
            'project_id': this.blockfrostProjectId
          },
          body: tx
        });
        
        if (!response.ok) {
          const errorText = await response.text();
          throw new Error(`Failed to submit transaction: ${response.status} - ${errorText}`);
        }
        
        return await response.json();
      } catch (error) {
        console.log(`Submit attempt ${attempt}/${maxRetries} failed: ${error.message}`);
        if (attempt < maxRetries) {
          console.log(`Retrying in ${retryDelay}ms...`);
          await new Promise(resolve => setTimeout(resolve, retryDelay));
        } else {
          throw error;
        }
      }
    }
  }

  convertUtxoAssets(amounts) {
    const assets = { lovelace: 0n };
    if (amounts && Array.isArray(amounts)) {
      for (const amount of amounts) {
        if (amount.unit === 'lovelace') {
          assets.lovelace = BigInt(amount.quantity);
        } else if (amount.unit) {
          assets[amount.unit] = BigInt(amount.quantity);
        }
      }
    }
    return assets;
  }

  async verifyTransaction(txHash) {
    try {
      const blockfrostUrl = this.network === 'mainnet' 
        ? 'https://cardano-mainnet.blockfrost.io/api/v0'
        : 'https://cardano-preprod.blockfrost.io/api/v0';
      
      const response = await fetch(`${blockfrostUrl}/txs/${txHash}`, {
        headers: { 'project_id': this.blockfrostProjectId }
      });
      
      if (response.ok) {
        return {
          success: true,
          found: true,
          message: 'Transaction found'
        };
      } else if (response.status === 404) {
        return {
          success: true,
          found: false,
          message: 'Transaction not found'
        };
      } else {
        return { success: false, message: 'Error checking transaction' };
      }
    } catch (error) {
      return { success: false, message: error.message };
    }
  }

  async getTransaction(txHash) {
    try {
      const blockfrostUrl = this.network === 'mainnet' 
        ? 'https://cardano-mainnet.blockfrost.io/api/v0'
        : 'https://cardano-preprod.blockfrost.io/api/v0';
      
      const response = await fetch(`${blockfrostUrl}/txs/${txHash}`, {
        headers: { 'project_id': this.blockfrostProjectId }
      });
      
      if (response.ok) {
        const tx = await response.json();
        return { success: true, transaction: tx };
      } else if (response.status === 404) {
        return { success: false, error: 'Transaction not found' };
      } else {
        return { success: false, error: 'Error getting transaction' };
      }
    } catch (error) {
      return { success: false, error: error.message };
    }
  }
}
