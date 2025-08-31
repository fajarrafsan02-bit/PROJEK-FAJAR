const express = require('express');
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static('public'));

// In-memory storage untuk demo
let currentPrices = {
    '24k': 2500000,
    '22k': 2291750,
    '18k': 1875000
};

let priceHistory = [];
let priceChanges = [];

// Routes
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Get current prices
app.get('/api/prices', (req, res) => {
    res.json({
        success: true,
        data: currentPrices,
        timestamp: new Date().toISOString()
    });
});

// Update prices
app.post('/api/prices', (req, res) => {
    try {
        const { harga24k, harga22k, harga18k, source = 'MANUAL' } = req.body;
        
        const oldPrices = { ...currentPrices };
        
        if (harga24k) {
            currentPrices['24k'] = parseInt(harga24k);
            // Auto-calculate other karats based on ratio
            currentPrices['22k'] = Math.round(currentPrices['24k'] * 0.9167);
            currentPrices['18k'] = Math.round(currentPrices['24k'] * 0.75);
        }
        
        if (harga22k) currentPrices['22k'] = parseInt(harga22k);
        if (harga18k) currentPrices['18k'] = parseInt(harga18k);
        
        // Record price change
        const change = {
            id: Date.now(),
            purity: '24k',
            oldPrice: oldPrices['24k'],
            newPrice: currentPrices['24k'],
            changeAmount: currentPrices['24k'] - oldPrices['24k'],
            changePercent: ((currentPrices['24k'] - oldPrices['24k']) / oldPrices['24k'] * 100).toFixed(2),
            changeType: currentPrices['24k'] > oldPrices['24k'] ? 'INCREASE' : 'DECREASE',
            changeDate: new Date().toISOString(),
            changeSource: source,
            notes: `Updated via ${source}`
        };
        
        priceChanges.unshift(change);
        priceHistory.unshift({
            id: Date.now(),
            tanggalAmbil: new Date().toISOString(),
            hargaJual24k: currentPrices['24k'],
            hargaJual22k: currentPrices['22k'],
            hargaJual18k: currentPrices['18k'],
            goldPriceEnum: source
        });
        
        res.json({
            success: true,
            message: 'Prices updated successfully',
            data: currentPrices,
            change: change
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Error updating prices',
            error: error.message
        });
    }
});

// Get price changes
app.get('/api/changes', (req, res) => {
    res.json({
        success: true,
        data: priceChanges.slice(0, 10) // Return last 10 changes
    });
});

// Get price history
app.get('/api/history', (req, res) => {
    const page = parseInt(req.query.page) || 0;
    const size = parseInt(req.query.size) || 10;
    const start = page * size;
    const end = start + size;
    
    res.json({
        success: true,
        data: {
            content: priceHistory.slice(start, end),
            totalElements: priceHistory.length,
            totalPages: Math.ceil(priceHistory.length / size),
            currentPage: page
        }
    });
});

// External API simulation
app.get('/api/external', (req, res) => {
    // Simulate external API response
    const externalPrice = currentPrices['24k'] + Math.floor(Math.random() * 100000) - 50000;
    
    res.json({
        success: true,
        data: {
            harga24k: externalPrice,
            harga22k: Math.round(externalPrice * 0.9167),
            harga18k: Math.round(externalPrice * 0.75),
            source: 'EXTERNAL_API',
            timestamp: new Date().toISOString()
        }
    });
});

// Start server
app.listen(PORT, () => {
    console.log(`🚀 Server running on http://localhost:${PORT}`);
    console.log(`📊 Current prices:`, currentPrices);
});