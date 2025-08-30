// Test file untuk memverifikasi perubahan harga emas
// Jalankan di browser console setelah halaman harga-emas.html dimuat

console.log('=== TESTING GOLD PRICE CHANGES ===');

// Test 1: Cek apakah GoldPriceManager sudah terinisialisasi
if (typeof goldPriceManager !== 'undefined') {
    console.log('✅ GoldPriceManager sudah terinisialisasi');
    console.log('Current prices:', goldPriceManager.currentPrices);
    console.log('Price changes:', goldPriceManager.priceChanges);
    console.log('Price history length:', goldPriceManager.priceHistory ? goldPriceManager.priceHistory.length : 0);
} else {
    console.log('❌ GoldPriceManager belum terinisialisasi');
}

// Test 2: Test endpoint changes
async function testChangesEndpoint() {
    try {
        console.log('Testing /gold-price/changes/latest endpoint...');
        const response = await fetch('/gold-price/changes/latest');
        const result = await response.json();
        
        if (response.ok) {
            console.log('✅ Changes endpoint berfungsi');
            console.log('Response:', result);
            
            if (result.success && result.data) {
                console.log('Data changes:', result.data);
                result.data.forEach((change, index) => {
                    console.log(`${index + 1}. ${change.purity}: ${change.oldPrice} -> ${change.newPrice} (${change.changeType}, ${change.changePercent}%)`);
                });
            }
        } else {
            console.log('❌ Changes endpoint error:', response.status);
            console.log('Error response:', result);
        }
    } catch (error) {
        console.error('❌ Error testing changes endpoint:', error);
    }
}

// Test 3: Test endpoint history
async function testHistoryEndpoint() {
    try {
        console.log('Testing /gold-price/history endpoint...');
        const response = await fetch('/gold-price/history?page=0&size=5&sortBy=tanggalAmbil&sortDirection=desc');
        const result = await response.json();
        
        if (response.ok) {
            console.log('✅ History endpoint berfungsi');
            console.log('Response:', result);
            
            if (result.success && result.data && result.data.content) {
                console.log('History data length:', result.data.content.length);
                result.data.content.forEach((item, index) => {
                    console.log(`${index + 1}. 24K: ${item.hargaJual24k}, 22K: ${item.hargaJual22k}, 18K: ${item.hargaJual18k}, Tanggal: ${item.tanggalAmbil}`);
                });
            }
        } else {
            console.log('❌ History endpoint error:', response.status);
            console.log('Error response:', result);
        }
    } catch (error) {
        console.error('❌ Error testing history endpoint:', error);
    }
}

// Test 4: Test endpoint latest
async function testLatestEndpoint() {
    try {
        console.log('Testing /gold-price/latest endpoint...');
        const response = await fetch('/gold-price/latest');
        const result = await response.json();
        
        if (response.ok) {
            console.log('✅ Latest endpoint berfungsi');
            console.log('Response:', result);
            
            if (result.success && result.data) {
                console.log('Latest price data:', result.data);
            }
        } else {
            console.log('❌ Latest endpoint error:', response.status);
            console.log('Error response:', result);
        }
    } catch (error) {
        console.error('❌ Error testing latest endpoint:', error);
    }
}

// Test 5: Test force refresh
async function testForceRefresh() {
    try {
        console.log('Testing force refresh...');
        if (typeof goldPriceManager !== 'undefined' && goldPriceManager.forceRefreshAllData) {
            await goldPriceManager.forceRefreshAllData();
            console.log('✅ Force refresh berhasil');
        } else {
            console.log('❌ Force refresh method tidak tersedia');
        }
    } catch (error) {
        console.error('❌ Error testing force refresh:', error);
    }
}

// Test 6: Test debug
function testDebug() {
    try {
        console.log('Testing debug method...');
        if (typeof goldPriceManager !== 'undefined' && goldPriceManager.debugPriceChanges) {
            goldPriceManager.debugPriceChanges();
            console.log('✅ Debug method berhasil');
        } else {
            console.log('❌ Debug method tidak tersedia');
        }
    } catch (error) {
        console.error('❌ Error testing debug:', error);
    }
}

// Jalankan semua test
async function runAllTests() {
    console.log('Running all tests...');
    
    await testChangesEndpoint();
    await testHistoryEndpoint();
    await testLatestEndpoint();
    await testForceRefresh();
    testDebug();
    
    console.log('=== ALL TESTS COMPLETED ===');
}

// Export functions untuk testing manual
window.testGoldPriceChanges = {
    testChangesEndpoint,
    testHistoryEndpoint,
    testLatestEndpoint,
    testForceRefresh,
    testDebug,
    runAllTests
};

console.log('Test functions available at: window.testGoldPriceChanges');
console.log('Run: window.testGoldPriceChanges.runAllTests() to test everything');