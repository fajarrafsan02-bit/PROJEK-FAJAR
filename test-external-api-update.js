// Test file khusus untuk masalah perubahan harga dari API eksternal
// Jalankan di browser console setelah halaman harga-emas.html dimuat

console.log('=== TESTING EXTERNAL API UPDATE ISSUE ===');

// Test 1: Cek status data sebelum update
function checkDataBeforeExternalUpdate() {
    if (typeof goldPriceManager !== 'undefined') {
        console.log('📊 Data status BEFORE external API update:');
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

// Test 2: Test endpoint changes dengan tracking source
async function testChangesEndpointWithSourceTracking() {
    try {
        console.log('🔍 Testing /gold-price/changes/latest endpoint with source tracking...');
        const response = await fetch('/gold-price/changes/latest');
        const result = await response.json();
        
        if (response.ok && result.success && result.data) {
            console.log('✅ Changes endpoint berfungsi');
            console.log('Total changes:', result.data.length);
            
            // Group changes by source
            const sources = {};
            result.data.forEach(change => {
                if (!sources[change.changeSource]) {
                    sources[change.changeSource] = [];
                }
                sources[change.changeSource].push({
                    purity: change.purity,
                    oldPrice: change.oldPrice,
                    newPrice: change.newPrice,
                    changeType: change.changeType,
                    changePercent: change.changePercent,
                    date: change.changeDate,
                    notes: change.notes
                });
            });
            
            console.log('📊 Changes grouped by source:');
            Object.keys(sources).forEach(source => {
                console.log(`  ${source}:`, sources[source]);
            });
            
            // Check for MANUAL vs EXTERNAL_API changes
            if (sources['MANUAL'] && sources['EXTERNAL_API']) {
                console.log('✅ Both MANUAL and EXTERNAL_API changes found');
                console.log('  MANUAL changes:', sources['MANUAL'].length);
                console.log('  EXTERNAL_API changes:', sources['EXTERNAL_API'].length);
            } else if (sources['MANUAL']) {
                console.log('⚠️ Only MANUAL changes found');
            } else if (sources['EXTERNAL_API']) {
                console.log('⚠️ Only EXTERNAL_API changes found');
            } else {
                console.log('❌ No changes found');
            }
            
            return sources;
        } else {
            console.log('❌ Changes endpoint error:', response.status);
            console.log('Error response:', result);
            return null;
        }
    } catch (error) {
        console.error('❌ Error testing changes endpoint:', error);
        return null;
    }
}

// Test 3: Test endpoint history dengan detail
async function testHistoryEndpointWithDetail() {
    try {
        console.log('🔍 Testing /gold-price/history endpoint with detail...');
        const response = await fetch('/gold-price/history?page=0&size=5&sortBy=tanggalAmbil&sortDirection=desc');
        const result = await response.json();
        
        if (response.ok && result.success && result.data && result.data.content) {
            console.log('✅ History endpoint berfungsi');
            console.log('History data length:', result.data.content.length);
            
            console.log('📊 Latest history items:');
            result.data.content.forEach((item, index) => {
                console.log(`  ${index + 1}. Tanggal: ${item.tanggalAmbil}`);
                console.log(`     24K: ${item.hargaJual24k}`);
                console.log(`     22K: ${item.hargaJual22k}`);
                console.log(`     18K: ${item.hargaJual18k}`);
                console.log(`     Source: ${item.goldPriceEnum || 'N/A'}`);
                console.log('     ---');
            });
            
            return result.data.content;
        } else {
            console.log('❌ History endpoint error:', response.status);
            return null;
        }
    } catch (error) {
        console.error('❌ Error testing history endpoint:', error);
        return null;
    }
}

// Test 4: Test external API fetch
async function testExternalAPIFetch() {
    try {
        console.log('🌐 Testing external API fetch...');
        const response = await fetch('/gold-price/fetch-external');
        const result = await response.json();
        
        if (response.ok && result.success) {
            console.log('✅ External API fetch berhasil');
            console.log('External price data:', result.data);
            
            // Check if we have valid prices
            const hasValidPrices = result.data && (
                result.data.harga24k || result.data.price24k || 
                result.data.harga22k || result.data.price22k || 
                result.data.harga18k || result.data.price18k
            );
            
            if (hasValidPrices) {
                console.log('✅ Valid external prices available');
                return result.data;
            } else {
                console.log('⚠️ No valid external prices found');
                return null;
            }
        } else {
            console.log('❌ External API fetch error:', response.status);
            console.log('Error response:', result);
            return null;
        }
    } catch (error) {
        console.error('❌ Error testing external API fetch:', error);
        return null;
    }
}

// Test 5: Simulate external API update
async function simulateExternalAPIUpdate() {
    try {
        console.log('🧪 Simulating external API update...');
        if (typeof goldPriceManager !== 'undefined') {
            // Get current prices
            const currentPrices = { ...goldPriceManager.currentPrices };
            console.log('Current prices before simulation:', currentPrices);
            
            // Simulate external API update
            const newPrice24k = currentPrices['24k'] + 100000; // Add 100k
            console.log('Simulating update to 24K:', newPrice24k);
            
            // Send update request
            const response = await fetch('/gold-price/update', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ harga24k: newPrice24k })
            });
            
            if (response.ok) {
                const result = await response.json();
                console.log('Update response:', result);
                
                if (result.success) {
                    console.log('✅ Simulated external API update successful');
                    
                    // Wait for backend to process
                    await new Promise(resolve => setTimeout(resolve, 3000));
                    
                    // Check if price changes were recorded
                    const changesResponse = await fetch('/gold-price/changes/latest');
                    if (changesResponse.ok) {
                        const changesData = await changesResponse.json();
                        console.log('Price changes after simulation:', changesData);
                        
                        // Look for EXTERNAL_API changes
                        if (changesData.success && changesData.data) {
                            const externalChanges = changesData.data.filter(change => 
                                change.changeSource === 'EXTERNAL_API'
                            );
                            console.log('EXTERNAL_API changes found:', externalChanges.length);
                            externalChanges.forEach(change => {
                                console.log(`  ${change.purity}: ${change.oldPrice} -> ${change.newPrice} (${change.changeType})`);
                            });
                        }
                    }
                    
                    return true;
                } else {
                    console.log('❌ Simulated update failed:', result.message);
                    return false;
                }
            } else {
                console.log('❌ Update request failed:', response.status);
                return false;
            }
        } else {
            console.log('❌ GoldPriceManager tidak tersedia');
            return false;
        }
    } catch (error) {
        console.error('❌ Error simulating external API update:', error);
        return false;
    }
}

// Test 6: Test data refresh after external update
async function testDataRefreshAfterExternalUpdate() {
    try {
        console.log('🔄 Testing data refresh after external update...');
        if (typeof goldPriceManager !== 'undefined') {
            console.log('Before refresh - Price changes:', goldPriceManager.priceChanges);
            
            // Use reliable refresh
            if (goldPriceManager.reliableRefreshData) {
                await goldPriceManager.reliableRefreshData();
                console.log('After reliable refresh - Price changes:', goldPriceManager.priceChanges);
                
                // Check if we have valid changes
                const hasValidChanges = goldPriceManager.hasValidPriceChanges();
                console.log('Has valid price changes after refresh:', hasValidChanges);
                
                return hasValidChanges;
            } else {
                console.log('❌ Reliable refresh method tidak tersedia');
                return false;
            }
        } else {
            console.log('❌ GoldPriceManager tidak tersedia');
            return false;
        }
    } catch (error) {
        console.error('❌ Error testing data refresh:', error);
        return false;
    }
}

// Test 7: Test source consistency
async function testSourceConsistency() {
    try {
        console.log('🔍 Testing source consistency...');
        
        // Get latest changes
        const changesResponse = await fetch('/gold-price/changes/latest');
        if (changesResponse.ok) {
            const changesData = await changesResponse.json();
            
            if (changesData.success && changesData.data && changesData.data.length > 0) {
                console.log('📊 Source consistency check:');
                
                // Check if sources are consistent
                const sources = [...new Set(changesData.data.map(change => change.changeSource))];
                console.log('Available sources:', sources);
                
                // Check for mixed sources
                if (sources.length > 1) {
                    console.log('✅ Multiple sources found - system working correctly');
                    
                    sources.forEach(source => {
                        const sourceChanges = changesData.data.filter(change => change.changeSource === source);
                        console.log(`  ${source}: ${sourceChanges.length} changes`);
                    });
                } else if (sources.length === 1) {
                    console.log('⚠️ Only one source found:', sources[0]);
                } else {
                    console.log('❌ No sources found');
                }
                
                return sources;
            }
        }
        
        return null;
    } catch (error) {
        console.error('❌ Error testing source consistency:', error);
        return null;
    }
}

// Jalankan semua test
async function runAllExternalAPITests() {
    console.log('🚀 Running all external API tests...');
    
    checkDataBeforeExternalUpdate();
    await testChangesEndpointWithSourceTracking();
    await testHistoryEndpointWithDetail();
    await testExternalAPIFetch();
    await simulateExternalAPIUpdate();
    await testDataRefreshAfterExternalUpdate();
    await testSourceConsistency();
    
    console.log('✅ All external API tests completed');
}

// Export functions untuk testing manual
window.testExternalAPIUpdate = {
    checkDataBeforeExternalUpdate,
    testChangesEndpointWithSourceTracking,
    testHistoryEndpointWithDetail,
    testExternalAPIFetch,
    simulateExternalAPIUpdate,
    testDataRefreshAfterExternalUpdate,
    testSourceConsistency,
    runAllExternalAPITests
};

console.log('External API test functions available at: window.testExternalAPIUpdate');
console.log('Run: window.testExternalAPIUpdate.runAllExternalAPITests() to test everything');
console.log('Or run individual tests like: window.testExternalAPIUpdate.testSourceConsistency()');