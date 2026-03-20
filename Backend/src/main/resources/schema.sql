
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create tenders table
CREATE TABLE IF NOT EXISTS tenders (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'OPEN',
    budget DECIMAL(12, 2),
    deadline TIMESTAMP,
    required_documents TEXT,
    category VARCHAR(50),
    user_type VARCHAR(50),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Create bidders table
CREATE TABLE IF NOT EXISTS bidders (
    id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    type VARCHAR(50),
    total_bids INT DEFAULT 0,
    winning_bids INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    address VARCHAR(500),
    contact_person VARCHAR(100),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Create bids table (matching Bid entity)
CREATE TABLE IF NOT EXISTS bids (
    id BIGSERIAL PRIMARY KEY,
    tender_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    bid_amount DECIMAL(12, 2) NOT NULL,
    proposal_text TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    contact_number VARCHAR(20),
    is_winning BOOLEAN DEFAULT false,
    document_path VARCHAR(500),
    document_paths TEXT,
    zip_file_path VARCHAR(500),
    blockchain_tx_hash VARCHAR(100),
    ipfs_hash VARCHAR(200),
    blockchain_timestamp TIMESTAMP,
    FOREIGN KEY (tender_id) REFERENCES tenders(id),
    FOREIGN KEY (bidder_id) REFERENCES bidders(id)
);

-- Create activity logs table for manual changes
CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    description TEXT,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    old_value TEXT,
    new_value TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50)
);

-- Create index for faster activity log queries
CREATE INDEX IF NOT EXISTS idx_activity_username ON activity_logs(username);
CREATE INDEX IF NOT EXISTS idx_activity_entity ON activity_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_activity_timestamp ON activity_logs(timestamp DESC);

-- Create blockchain transactions table
CREATE TABLE IF NOT EXISTS blockchain_transactions (
    id BIGSERIAL PRIMARY KEY,
    tender_id BIGINT,
    bid_id BIGINT,
    summary_hash VARCHAR(1000),
    ipfs_hash VARCHAR(500),
    ipfs_url VARCHAR(1000),
    cardano_transaction_id VARCHAR(200),
    blockchain_url VARCHAR(1000),
    blockchain_network VARCHAR(100),
    status VARCHAR(50),
    total_records INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_blockchain_tender ON blockchain_transactions(tender_id);
CREATE INDEX IF NOT EXISTS idx_blockchain_bid ON blockchain_transactions(bid_id);
CREATE INDEX IF NOT EXISTS idx_blockchain_created ON blockchain_transactions(created_at DESC);
