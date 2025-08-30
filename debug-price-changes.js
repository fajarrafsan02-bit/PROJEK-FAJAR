// Debug file khusus untuk masalah perubahan harga emas
// Jalankan di browser console setelah halaman harga-emas.html dimuat

console.log('=== DEBUG PRICE CHANGES ISSUE ===');

// Test 1: Cek struktur data GoldPriceManager
function checkGoldPriceManager() {
    if (typeof goldPriceManager !== 'undefined') {
        console.log('✅ GoldPriceManager tersedia');
        console.log('Current prices:', goldPriceManager.currentPrices);
        console.log('Price changes:', goldPriceManager.priceChanges);
        console.log('Price history length:', goldPriceManager.priceHistory ? goldPriceManager.priceHistory.length : 0);
        console.log('Has valid price changes:', goldPriceManager.hasValidPriceChanges ? goldPriceManager.hasValidPriceChanges() : 'Method tidak tersedia');
        return true;
    } else {
        console.log('❌ GoldPriceManager tidak tersedia');
        return false;
    }
}

// Test 2: Test endpoint changes dengan detail
async function testChangesEndpointDetailed() {
    try {
        console.log('🔍 Testing /gold-price/changes/latest endpoint...');
        const response = await fetch('/gold-price/changes/latest');
        const result = await response.json();
        
        console.log('Response status:', response.status);
        console.log('Response headers:', response.headers);
        console.log('Full response:', result);
        
        if (response.ok) {
            if (result.success) {
                console.log('✅ Success flag: true');
                if (result.data) {
                    console.log('✅ Data tersedia');
                    console.log('Data type:', typeof result.data);
                    console.log('Data is array:', Array.isArray(result.data));
                    console.log('Data length:', result.data ? result.data.length : 'null/undefined');
                    
                    if (Array.isArray(result.data) && result.data.length > 0) {
                        console.log('📊 Changes data details:');
                        result.data.forEach((change, index) => {
                            console.log(`  ${index + 1}. Purity: ${change.purity}`);
                            console.log(`     Old Price: ${change.oldPrice}`);
                            console.log(`     New Price: ${change.newPrice}`);
                            console.log(`     Change Amount: ${change.changeAmount}`);
                            console.log(`     Change Percent: ${change.changePercent}`);
                            console.log(`     Change Type: ${change.changeType}`);
                            console.log(`     Change Date: ${change.changeDate}`);
                            console.log(`     Change Source: ${change.changeSource}`);
                            console.log(`     Notes: ${change.notes}`);
                            console.log('     ---');
                        });
                    } else {
                        console.log('⚠️ Data kosong atau bukan array');
                    }
                } else {
                    console.log('❌ Data tidak tersedia');
                }
            } else {
                console.log('❌ Success flag: false');
                console.log('Error message:', result.message);
            }
        } else {
            console.log('❌ HTTP Error:', response.status);
            console.log('Error response:', result);
        }
    } catch (error) {
        console.error('❌ Error testing changes endpoint:', error);
    }
}

// Test 3: Test endpoint history dengan detail
async function testHistoryEndpointDetailed() {
    try {
        console.log('🔍 Testing /gold-price/history endpoint...');
        const response = await fetch('/gold-price/history?page=0&size=5&sortBy=tanggalAmbil&sortDirection=desc');
        const result = await response.json();
        
        console.log('Response status:', response.status);
        console.log('Full response:', result);
        
        if (response.ok && result.success && result.data && result.data.content) {
            console.log('📊 History data details:');
            result.data.content.forEach((item, index) => {
                console.log(`  ${index + 1}. Tanggal: ${item.tanggalAmbil}`);
                console.log(`     24K: ${item.hargaJual24k}`);
                console.log(`     22K: ${item.hargaJual22k}`);
                console.log(`     18K: ${item.hargaJual18k}`);
                console.log(`     Updated By: ${item.goldPriceEnum || 'N/A'}`);
                console.log('     ---');
            });
        }
    } catch (error) {
        console.error('❌ Error testing history endpoint:', error);
    }
}

// Test 4: Test force refresh
async function testForceRefresh() {
    try {
        console.log('🔄 Testing force refresh...');
        if (typeof goldPriceManager !== 'undefined' && goldPriceManager.forceRefreshAllData) {
            console.log('Before refresh - Price changes:', goldPriceManager.priceChanges);
            await goldPriceManager.forceRefreshAllData();
            console.log('After refresh - Price changes:', goldPriceManager.priceChanges);
            console.log('✅ Force refresh completed');
        } else {
            console.log('❌ Force refresh method tidak tersedia');
        }
    } catch (error) {
        console.error('❌ Error testing force refresh:', error);
    }
}

// Test 5: Test manual load price changes
async function testManualLoadPriceChanges() {
    try {
        console.log('🔍 Testing manual load price changes...');
        if (typeof goldPriceManager !== 'undefined' && goldPriceManager.loadPriceChanges) {
            console.log('Before manual load - Price changes:', goldPriceManager.priceChanges);
            await goldPriceManager.loadPriceChanges();
            console.log('After manual load - Price changes:', goldPriceManager.priceChanges);
            console.log('✅ Manual load completed');
        } else {
            console.log('❌ Manual load method tidak tersedia');
        }
    } catch (error) {
        console.error('❌ Error testing manual load:', error);
    }
}

// Test 6: Test render current prices
function testRenderCurrentPrices() {
    try {
        console.log('🎨 Testing render current prices...');
        if (typeof goldPriceManager !== 'undefined' && goldPriceManager.renderCurrentPrices) {
            console.log('Before render - Price changes:', goldPriceManager.priceChanges);
            goldPriceManager.renderCurrentPrices();
            console.log('After render - Price changes:', goldPriceManager.priceChanges);
            console.log('✅ Render completed');
        } else {
            console.log('❌ Render method tidak tersedia');
        }
    } catch (error) {
        console.error('❌ Error testing render:', error);
    }
}

// Test 7: Simulate price update untuk test
async function simulatePriceUpdate() {
    try {
        console.log('🧪 Simulating price update...');
        if (typeof goldPriceManager !== 'undefined') {
            // Simulate update dengan harga yang berbeda
            const testPrice = 1000000; // 1 juta rupiah
            console.log('Simulating update to:', testPrice);
            
            // Update current prices untuk test
            goldPriceManager.currentPrices['24k'] = testPrice;
            
            // Force render
            goldPriceManager.renderCurrentPrices();
            
            console.log('✅ Simulation completed');
        } else {
            console.log('❌ GoldPriceManager tidak tersedia');
        }
    } catch (error) {
        console.error('❌ Error simulating price update:', error);
    }
}

// Jalankan semua test
async function runAllDebugTests() {
    console.log('🚀 Running all debug tests...');
    
    checkGoldPriceManager();
    await testChangesEndpointDetailed();
    await testHistoryEndpointDetailed();
    await testManualLoadPriceChanges();
    testRenderCurrentPrices();
    await testForceRefresh();
    await simulatePriceUpdate();
    
    console.log('✅ All debug tests completed');
}

// Export functions untuk testing manual
window.debugPriceChangesIssue = {
    checkGoldPriceManager,
    testChangesEndpointDetailed,
    testHistoryEndpointDetailed,
    testForceRefresh,
    testManualLoadPriceChanges,
    testRenderCurrentPrices,
    simulatePriceUpdate,
    runAllDebugTests
};

console.log('Debug functions available at: window.debugPriceChangesIssue');
console.log('Run: window.debugPriceChangesIssue.runAllDebugTests() to test everything');
console.log('Or run individual tests like: window.debugPriceChangesIssue.testChangesEndpointDetailed()');