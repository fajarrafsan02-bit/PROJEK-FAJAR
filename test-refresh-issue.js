// Test file khusus untuk masalah refresh data setelah update
// Jalankan di browser console setelah halaman harga-emas.html dimuat

console.log('=== TESTING REFRESH ISSUE AFTER UPDATE ===');

// Test 1: Cek status data sebelum update
function checkDataBeforeUpdate() {
    if (typeof goldPriceManager !== 'undefined') {
        console.log('📊 Data status BEFORE update:');
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

// Test 2: Test reliable refresh method
async function testReliableRefresh() {
    try {
        console.log('🔄 Testing reliable refresh method...');
        if (typeof goldPriceManager !== 'undefined' && goldPriceManager.reliableRefreshData) {
            console.log('Before reliable refresh - Price changes:', goldPriceManager.priceChanges);
            const result = await goldPriceManager.reliableRefreshData();
            console.log('After reliable refresh - Price changes:', goldPriceManager.priceChanges);
            console.log('Reliable refresh result:', result);
            console.log('✅ Reliable refresh test completed');
            return result;
        } else {
            console.log('❌ Reliable refresh method tidak tersedia');
            return false;
        }
    } catch (error) {
        console.error('❌ Error testing reliable refresh:', error);
        return false;
    }
}

// Test 3: Test individual refresh methods
async function testIndividualRefreshMethods() {
    try {
        console.log('🔍 Testing individual refresh methods...');
        
        if (typeof goldPriceManager !== 'undefined') {
            // Test loadCurrentPrices
            console.log('Testing loadCurrentPrices...');
            await goldPriceManager.loadCurrentPrices();
            console.log('Current prices after load:', goldPriceManager.currentPrices);
            
            // Test loadPriceChanges
            console.log('Testing loadPriceChanges...');
            await goldPriceManager.loadPriceChanges();
            console.log('Price changes after load:', goldPriceManager.priceChanges);
            
            // Test loadPriceHistory
            console.log('Testing loadPriceHistory...');
            await goldPriceManager.loadPriceHistory();
            console.log('Price history after load, length:', goldPriceManager.priceHistory.length);
            
            // Test calculatePriceChanges
            console.log('Testing calculatePriceChanges...');
            await goldPriceManager.calculatePriceChanges();
            console.log('Price changes after calculation:', goldPriceManager.priceChanges);
            
            console.log('✅ Individual refresh methods test completed');
        } else {
            console.log('❌ GoldPriceManager tidak tersedia');
        }
    } catch (error) {
        console.error('❌ Error testing individual refresh methods:', error);
    }
}

// Test 4: Test render after refresh
function testRenderAfterRefresh() {
    try {
        console.log('🎨 Testing render after refresh...');
        if (typeof goldPriceManager !== 'undefined' && goldPriceManager.renderCurrentPrices) {
            console.log('Before render - Price changes:', goldPriceManager.priceChanges);
            goldPriceManager.renderCurrentPrices();
            console.log('After render - Price changes:', goldPriceChanges);
            console.log('✅ Render test completed');
        } else {
            console.log('❌ Render method tidak tersedia');
        }
    } catch (error) {
        console.error('❌ Error testing render:', error);
    }
}

// Test 5: Simulate update dan test refresh
async function simulateUpdateAndTestRefresh() {
    try {
        console.log('🧪 Simulating update and testing refresh...');
        if (typeof goldPriceManager !== 'undefined') {
            // Simulate update dengan mengubah current prices
            const originalPrices = { ...goldPriceManager.currentPrices };
            goldPriceManager.currentPrices['24k'] = 1200000; // Simulate new price
            
            console.log('Simulated price update - 24K changed to:', goldPriceManager.currentPrices['24k']);
            
            // Test reliable refresh
            await goldPriceManager.reliableRefreshData();
            
            // Check if data is refreshed
            console.log('After simulated update and refresh:');
            console.log('Current prices:', goldPriceManager.currentPrices);
            console.log('Price changes:', goldPriceManager.priceChanges);
            
            // Restore original prices
            goldPriceManager.currentPrices = originalPrices;
            
            console.log('✅ Simulate update and refresh test completed');
        } else {
            console.log('❌ GoldPriceManager tidak tersedia');
        }
    } catch (error) {
        console.error('❌ Error simulating update and refresh:', error);
    }
}

// Test 6: Test timing issue
async function testTimingIssue() {
    try {
        console.log('⏱️ Testing timing issue...');
        if (typeof goldPriceManager !== 'undefined') {
            console.log('Starting timing test...');
            
            // Test dengan delay yang berbeda
            const delays = [500, 1000, 2000];
            
            for (const delay of delays) {
                console.log(`Testing with ${delay}ms delay...`);
                
                // Clear data
                goldPriceManager.priceChanges = {};
                
                // Load data dengan delay
                await new Promise(resolve => setTimeout(resolve, delay));
                await goldPriceManager.loadPriceChanges();
                
                console.log(`After ${delay}ms delay - Price changes:`, goldPriceManager.priceChanges);
            }
            
            console.log('✅ Timing test completed');
        } else {
            console.log('❌ GoldPriceManager tidak tersedia');
        }
    } catch (error) {
        console.error('❌ Error testing timing issue:', error);
    }
}

// Test 7: Test database endpoint langsung
async function testDatabaseEndpointDirectly() {
    try {
        console.log('🗄️ Testing database endpoint directly...');
        
        // Test endpoint changes
        const changesRes = await fetch('/gold-price/changes/latest');
        if (changesRes.ok) {
            const changesData = await changesRes.json();
            console.log('Direct changes endpoint response:', changesData);
            
            if (changesData.success && changesData.data) {
                console.log('Changes data from database:', changesData.data);
                changesData.data.forEach((change, index) => {
                    console.log(`${index + 1}. ${change.purity}: ${change.oldPrice} -> ${change.newPrice}`);
                });
            }
        } else {
            console.log('Changes endpoint error:', changesRes.status);
        }
        
        // Test endpoint history
        const historyRes = await fetch('/gold-price/history?page=0&size=3&sortBy=tanggalAmbil&sortDirection=desc');
        if (historyRes.ok) {
            const historyData = await historyRes.json();
            console.log('Direct history endpoint response:', historyData);
        } else {
            console.log('History endpoint error:', historyRes.status);
        }
        
        console.log('✅ Database endpoint test completed');
    } catch (error) {
        console.error('❌ Error testing database endpoint:', error);
    }
}

// Jalankan semua test
async function runAllRefreshTests() {
    console.log('🚀 Running all refresh tests...');
    
    checkDataBeforeUpdate();
    await testReliableRefresh();
    await testIndividualRefreshMethods();
    testRenderAfterRefresh();
    await simulateUpdateAndTestRefresh();
    await testTimingIssue();
    await testDatabaseEndpointDirectly();
    
    console.log('✅ All refresh tests completed');
}

// Export functions untuk testing manual
window.testRefreshIssue = {
    checkDataBeforeUpdate,
    testReliableRefresh,
    testIndividualRefreshMethods,
    testRenderAfterRefresh,
    simulateUpdateAndTestRefresh,
    testTimingIssue,
    testDatabaseEndpointDirectly,
    runAllRefreshTests
};

console.log('Refresh test functions available at: window.testRefreshIssue');
console.log('Run: window.testRefreshIssue.runAllRefreshTests() to test everything');
console.log('Or run individual tests like: window.testRefreshIssue.testReliableRefresh()');